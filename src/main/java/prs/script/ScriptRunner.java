package prs.script;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import prs.data.ScriptManager;
import prs.data.ScriptManager.LoadedScript;
import prs.data.ScriptManager.WorldScripts;
import prs.data.UserWorldManager;
import prs.privateworld.PrivateWorld;
import prs.script.CompiledScript.Action;
import prs.script.CompiledScript.If;
import prs.script.CompiledScript.Operand;
import prs.script.CompiledScript.Statement;
import prs.script.CompiledScript.Trigger;
import prs.script.CompiledScript.TriggerType;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Executes compiled Workshop scripts.
 *
 * <p>Everything a script can do is scoped to the world it is installed in and
 * to the player that caused the event. Runs are bounded: each run may execute
 * at most {@code script.max-actions-per-run} statements, {@code wait} pauses
 * are capped, and each world may only have a limited number of paused runs.</p>
 *
 * <p>Per-player state (checkpoint, stopwatch, variables) lives in a
 * {@link Session} that is reset whenever the player changes world. Shared
 * {@code $variables} live until the world is empty.</p>
 */
public class ScriptRunner {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\$?[\\p{L}\\p{N}_]+)}");
    private static final int MAX_VARIABLES = 64;
    private static final long CLICK_COOLDOWN_MS = 200;
    private static final long REPORT_COOLDOWN_MS = 3000;

    private final PrivateWorld plugin;
    private final ScriptManager scripts;

    private final Map<UUID, Session> sessions = new HashMap<>();
    /** world name → script id → shared variables */
    private final Map<String, Map<String, Map<String, Double>>> globals = new HashMap<>();
    private final Map<String, Integer> pendingPerWorld = new HashMap<>();
    private final Map<String, Long> lastReport = new HashMap<>();
    private final Map<UUID, Long> lastClick = new HashMap<>();

    private BukkitTask timerTask;
    private long timerSeconds = 0;

    public ScriptRunner(PrivateWorld plugin, ScriptManager scripts) {
        this.plugin = plugin;
        this.scripts = scripts;
    }

    // -------------------------------------------------------------------------
    // Sessions
    // -------------------------------------------------------------------------

    /** Per-player state for the world the player is currently in. */
    static final class Session {
        final String world;
        boolean joined;
        Location checkpoint;
        long timerStart = -1;
        long timerFrozen = -1;
        int stepX, stepY, stepZ;
        boolean hasStep;
        boolean pendingDeath;
        int teleports;
        /** Gamemode before a script first changed it, restored on leave. */
        GameMode gameModeBefore;
        /** Potion effects applied by scripts, removed on leave. */
        final Set<PotionEffectType> effects = new HashSet<>();
        final Map<String, Map<String, Double>> vars = new HashMap<>();

        Session(String world) {
            this.world = world;
        }

        double elapsedSeconds() {
            if (timerFrozen >= 0) return timerFrozen / 1000.0;
            if (timerStart < 0) return 0;
            return (System.currentTimeMillis() - timerStart) / 1000.0;
        }
    }

    private Session session(Player p) {
        Session s = sessions.get(p.getUniqueId());
        if (s == null || !s.world.equals(p.getWorld().getName())) {
            s = new Session(p.getWorld().getName());
            sessions.put(p.getUniqueId(), s);
        }
        return s;
    }

    // -------------------------------------------------------------------------
    // Lifecycle hooks (called by ScriptListener)
    // -------------------------------------------------------------------------

    public void start() {
        timerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickTimers, 20L, 20L);
    }

    public void stop() {
        if (timerTask != null) timerTask.cancel();
        for (Map.Entry<UUID, Session> entry : sessions.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p != null) restore(p, entry.getValue());
        }
        sessions.clear();
        globals.clear();
        pendingPerWorld.clear();
    }

    /** Player arrived in {@code world}: start a fresh session and run {@code on join}. */
    public void handleJoin(Player p) {
        Session s = session(p);
        if (s.joined) return;
        s.joined = true;
        fire(p, TriggerType.JOIN, t -> true);
    }

    /** Player left {@code from} (world change or quit): run {@code on leave} and clean up. */
    public void handleLeave(Player p, World from) {
        Session s = sessions.get(p.getUniqueId());
        if (s == null || !s.world.equals(from.getName())) return;
        WorldScripts ws = scripts.hasScripts(from) ? scripts.getWorldScripts(from) : null;
        if (ws != null) {
            for (LoadedScript ls : ws.scripts()) {
                for (Trigger t : ls.script().getTriggers(TriggerType.LEAVE)) {
                    new Execution(p, from, ls, ws, s, t, true).run();
                }
            }
        }
        restore(p, s);
        sessions.remove(p.getUniqueId());
        lastClick.remove(p.getUniqueId());
        boolean empty = true;
        for (Player other : from.getPlayers()) {
            if (!other.equals(p)) {
                empty = false;
                break;
            }
        }
        if (empty) globals.remove(from.getName());
    }

    public void handleMove(Player p, Location from, Location to) {
        WorldScripts ws = scripts.getWorldScripts(to.getWorld());
        Session s = session(p);
        int teleportsBefore = s.teleports;

        for (LoadedScript ls : ws.scripts()) {
            for (Trigger t : ls.script().getTriggers(TriggerType.FALL)) {
                if (from.getY() >= t.number && to.getY() < t.number) {
                    new Execution(p, to.getWorld(), ls, ws, s, t, false).run();
                }
            }
        }
        // A fall trigger moved the player: 'to' is stale, skip step detection
        if (s.teleports != teleportsBefore || sessions.get(p.getUniqueId()) != s) return;

        Location below = to.clone().subtract(0, 0.01, 0);
        int gx = below.getBlockX(), gy = below.getBlockY(), gz = below.getBlockZ();
        Material ground = below.getBlock().getType();
        if (ground.isAir()) return;
        if (s.hasStep && s.stepX == gx && s.stepY == gy && s.stepZ == gz) return;
        s.hasStep = true;
        s.stepX = gx;
        s.stepY = gy;
        s.stepZ = gz;

        Material feet = to.getBlock().getType();
        int fx = to.getBlockX(), fy = to.getBlockY(), fz = to.getBlockZ();
        fire(p, TriggerType.STEP, t -> t.matchesBlock(ground, gx, gy, gz)
                || (!feet.isAir() && t.matchesBlock(feet, fx, fy, fz)));
    }

    public void handleClick(Player p, Material type, int x, int y, int z) {
        long now = System.currentTimeMillis();
        Long last = lastClick.get(p.getUniqueId());
        if (last != null && now - last < CLICK_COOLDOWN_MS) return;
        lastClick.put(p.getUniqueId(), now);
        fire(p, TriggerType.CLICK, t -> t.matchesBlock(type, x, y, z));
    }

    public void handleChat(Player p, String message) {
        String msg = message.trim();
        fire(p, TriggerType.CHAT, t -> t.text.equalsIgnoreCase(msg));
    }

    /** Marks that the player died here so {@code on death} runs after respawning in this world. */
    public void markDeath(Player p) {
        session(p).pendingDeath = true;
    }

    /** Where a player who died in a scripted world should respawn: checkpoint, else world spawn. */
    public Location respawnLocation(Player p) {
        Session s = sessions.get(p.getUniqueId());
        if (s == null || !s.pendingDeath) return null;
        World w = Bukkit.getWorld(s.world);
        if (w == null || !scripts.hasScripts(w)) return null;
        return s.checkpoint != null ? s.checkpoint.clone() : spawnOf(w);
    }

    /** Runs {@code on death} once the player is alive again. */
    public void handleRespawned(Player p) {
        Session s = sessions.get(p.getUniqueId());
        if (s == null || !s.pendingDeath) return;
        s.pendingDeath = false;
        if (s.world.equals(p.getWorld().getName())) fire(p, TriggerType.DEATH, t -> true);
    }

    public void handleKill(Player killer) {
        fire(killer, TriggerType.KILL, t -> true);
    }

    /** Undoes changes a script should not carry outside its world (effects, gamemode). */
    private static void restore(Player p, Session s) {
        for (PotionEffectType type : s.effects) p.removePotionEffect(type);
        s.effects.clear();
        if (s.gameModeBefore != null) {
            p.setGameMode(s.gameModeBefore);
            s.gameModeBefore = null;
        }
    }

    public void forget(Player p) {
        sessions.remove(p.getUniqueId());
        lastClick.remove(p.getUniqueId());
    }

    private void tickTimers() {
        timerSeconds++;
        if (!plugin.configManager.isScriptEnabled()) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            World w = p.getWorld();
            if (!scripts.hasScripts(w)) continue;
            WorldScripts ws = scripts.getWorldScripts(w);
            Session s = session(p);
            for (LoadedScript ls : ws.scripts()) {
                for (Trigger t : ls.script().getTriggers(TriggerType.TIMER)) {
                    if (timerSeconds % t.number == 0) new Execution(p, w, ls, ws, s, t, false).run();
                }
            }
        }
    }

    /** Runs every trigger of {@code type} accepted by {@code filter} in the player's current world. */
    private void fire(Player p, TriggerType type, Predicate<Trigger> filter) {
        World w = p.getWorld();
        if (!scripts.hasScripts(w)) return;
        WorldScripts ws = scripts.getWorldScripts(w);
        Session s = session(p);
        for (LoadedScript ls : ws.scripts()) {
            for (Trigger t : ls.script().getTriggers(type)) {
                if (filter.test(t)) new Execution(p, w, ls, ws, s, t, false).run();
            }
        }
    }

    // -------------------------------------------------------------------------
    // Error reporting
    // -------------------------------------------------------------------------

    /** Tells the world owner (if online) that a script failed; throttled per script. */
    private void report(World w, WorldScripts ws, LoadedScript ls, int line, String msg) {
        String key = w.getName() + "/" + ls.id();
        long now = System.currentTimeMillis();
        Long last = lastReport.get(key);
        if (last != null && now - last < REPORT_COOLDOWN_MS) return;
        lastReport.put(key, now);
        if (ws.owner() == null) return;
        Player owner = Bukkit.getPlayer(ws.owner());
        if (owner == null) return;
        owner.sendMessage(ChatColor.RED + "[스크립트] " + ChatColor.YELLOW + ls.name()
                + ChatColor.RED + " " + line + "번째 줄: " + ChatColor.WHITE + msg);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static Location spawnOf(World w) {
        Location loc = new UserWorldManager(w).getSpawnLocation();
        if (loc != null && w.equals(loc.getWorld())) return loc.clone();
        return new Location(w, 0.5, 65, 0.5);
    }

    private static String format(double d) {
        if (d == Math.rint(d) && Math.abs(d) < 1e15) return String.valueOf((long) d);
        return String.format("%.2f", d);
    }

    // -------------------------------------------------------------------------
    // Execution
    // -------------------------------------------------------------------------

    /** A single run of one trigger body; survives {@code wait} by rescheduling itself. */
    private final class Execution {
        private final Player player;
        private final World world;
        private final LoadedScript ls;
        private final WorldScripts ws;
        private final Session session;
        private final boolean leaving;
        private final Deque<Frame> stack = new ArrayDeque<>();
        private int steps = 0;

        private final class Frame {
            final List<Statement> body;
            int index = 0;

            Frame(List<Statement> body) {
                this.body = body;
            }
        }

        Execution(Player player, World world, LoadedScript ls, WorldScripts ws, Session session,
                  Trigger trigger, boolean leaving) {
            this.player = player;
            this.world = world;
            this.ls = ls;
            this.ws = ws;
            this.session = session;
            this.leaving = leaving;
            stack.push(new Frame(trigger.body));
        }

        void run() {
            int maxSteps = plugin.configManager.getMaxActionsPerRun();
            while (!stack.isEmpty()) {
                Frame frame = stack.peek();
                if (frame.index >= frame.body.size()) {
                    stack.pop();
                    continue;
                }
                Statement stmt = frame.body.get(frame.index++);
                if (++steps > maxSteps) {
                    report(world, ws, ls, stmt.line(), "한 번에 실행할 수 있는 명령 수(" + maxSteps + ")를 넘었습니다");
                    return;
                }
                try {
                    if (stmt instanceof If cond) {
                        List<Statement> branch = cond.test(value(cond.left()), value(cond.right()))
                                ? cond.then() : cond.otherwise();
                        if (!branch.isEmpty()) stack.push(new Frame(branch));
                        continue;
                    }
                    Action action = (Action) stmt;
                    switch (action.type()) {
                        case STOP -> {
                            return;
                        }
                        case WAIT -> {
                            scheduleResume(action);
                            return;
                        }
                        default -> perform(action);
                    }
                } catch (RuntimeException ex) {
                    report(world, ws, ls, stmt.line(), "실행 중 오류: " + ex.getMessage());
                    return;
                }
            }
        }

        private void scheduleResume(Action action) {
            if (leaving) return; // nothing left to do for a player who already left
            int max = plugin.configManager.getMaxPendingPerWorld();
            int pending = pendingPerWorld.getOrDefault(world.getName(), 0);
            if (pending >= max) {
                report(world, ws, ls, action.line(), "대기 중인 실행이 너무 많습니다 (최대 " + max + ")");
                return;
            }
            pendingPerWorld.put(world.getName(), pending + 1);
            long ticks = Math.max(1, Math.round(Double.parseDouble(action.arg(0)) * 20));
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                pendingPerWorld.merge(world.getName(), -1, Integer::sum);
                if (stillValid()) run();
            }, ticks);
        }

        /** After a wait the player must still be in the same visit of the same world, with the script installed. */
        private boolean stillValid() {
            if (!player.isOnline() || !player.getWorld().equals(world)) return false;
            // A new visit creates a new Session object, so identity means "same visit"
            if (sessions.get(player.getUniqueId()) != session) return false;
            for (LoadedScript other : scripts.getWorldScripts(world).scripts()) {
                if (other.id().equals(ls.id())) return true;
            }
            return false;
        }

        // ---- values ----------------------------------------------------------

        private double value(Operand o) {
            if (o.isLiteral()) return o.literal();
            return valueOf(o.name());
        }

        private double valueOf(String name) {
            Location loc = player.getLocation();
            switch (name) {
                case "time":    return session.elapsedSeconds();
                case "x":       return loc.getX();
                case "y":       return loc.getY();
                case "z":       return loc.getZ();
                case "health":  return player.getHealth();
                case "food":    return player.getFoodLevel();
                case "players": return world.getPlayers().size();
                case "random":  return ThreadLocalRandom.current().nextDouble(100);
                default:
                    Double v = vars(name).get(name);
                    return v == null ? 0 : v;
            }
        }

        private Map<String, Double> vars(String name) {
            if (name.startsWith("$")) {
                return globals.computeIfAbsent(world.getName(), k -> new HashMap<>())
                        .computeIfAbsent(ls.id(), k -> new HashMap<>());
            }
            return session.vars.computeIfAbsent(ls.id(), k -> new HashMap<>());
        }

        private void setVar(String name, double value, int line) {
            Map<String, Double> map = vars(name);
            if (!map.containsKey(name) && map.size() >= MAX_VARIABLES) {
                report(world, ws, ls, line, "변수는 최대 " + MAX_VARIABLES + "개까지 만들 수 있습니다");
                return;
            }
            map.put(name, value);
        }

        private double operand(String raw) {
            Double d = ScriptParser.parseDouble(raw);
            return d != null ? d : valueOf(raw);
        }

        /** Applies placeholders ({player}, {time}, {coins}, ...) and & colour codes. */
        private String text(String template) {
            String colored = ChatColor.translateAlternateColorCodes('&', template);
            Matcher m = PLACEHOLDER.matcher(colored);
            StringBuilder sb = new StringBuilder();
            while (m.find()) {
                String key = m.group(1).toLowerCase();
                String replacement = switch (key) {
                    case "player" -> player.getName();
                    case "world"  -> ws.displayName();
                    case "time"   -> String.format("%.2f", session.elapsedSeconds());
                    default       -> format(valueOf(key));
                };
                m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            m.appendTail(sb);
            return sb.toString();
        }

        // ---- actions ---------------------------------------------------------

        private void perform(Action a) {
            switch (a.type()) {
                case MESSAGE -> player.sendMessage(text(a.arg(0)));
                case BROADCAST -> {
                    String msg = text(a.arg(0));
                    for (Player other : world.getPlayers()) other.sendMessage(msg);
                }
                case TITLE -> player.sendTitle(text(a.arg(0)), a.arg(1) != null ? text(a.arg(1)) : "", 10, 60, 20);
                case ACTIONBAR -> player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        TextComponent.fromLegacyText(text(a.arg(0))));
                case SOUND -> {
                    float volume = a.arg(1) != null ? Float.parseFloat(a.arg(1)) : 1f;
                    float pitch  = a.arg(2) != null ? Float.parseFloat(a.arg(2)) : 1f;
                    player.playSound(player.getLocation(), a.arg(0), volume, pitch);
                }
                case PARTICLE -> {
                    int count = a.arg(1) != null ? (int) Double.parseDouble(a.arg(1)) : 10;
                    world.spawnParticle(Particle.valueOf(a.arg(0)), player.getLocation().add(0, 1, 0),
                            count, 0.5, 0.5, 0.5, 0);
                }
                case TELEPORT -> {
                    if (leaving) return;
                    switch (a.arg(0)) {
                        case "spawn" -> moveTo(spawnOf(world));
                        case "checkpoint" -> moveTo(session.checkpoint != null ? session.checkpoint : spawnOf(world));
                        default -> moveTo(coordinates(a));
                    }
                }
                case CHECKPOINT -> {
                    if (!leaving) session.checkpoint = player.getLocation().clone();
                }
                case RESPAWN -> {
                    if (!leaving) moveTo(session.checkpoint != null ? session.checkpoint : spawnOf(world));
                }
                case LOBBY -> {
                    if (leaving) return;
                    if (!plugin.worldManager.isLobbySet()) {
                        report(world, ws, ls, a.line(), "로비가 설정되지 않았습니다");
                        return;
                    }
                    player.teleport(plugin.worldManager.getLobby());
                }
                case GIVE -> {
                    int amount = a.arg(1) != null ? (int) Double.parseDouble(a.arg(1)) : 1;
                    player.getInventory().addItem(new ItemStack(Material.valueOf(a.arg(0)), amount));
                }
                case CLEAR -> player.getInventory().clear();
                case GAMEMODE -> {
                    if (session.gameModeBefore == null) session.gameModeBefore = player.getGameMode();
                    player.setGameMode(GameMode.valueOf(a.arg(0)));
                }
                case HEAL -> {
                    player.setHealth(player.getMaxHealth());
                    player.setFoodLevel(20);
                    player.setSaturation(20f);
                    player.setFireTicks(0);
                }
                case DAMAGE -> player.damage(Double.parseDouble(a.arg(0)));
                case EFFECT -> {
                    if (a.arg(0).equals("clear")) {
                        for (PotionEffect effect : player.getActivePotionEffects()) {
                            player.removePotionEffect(effect.getType());
                        }
                        session.effects.clear();
                        return;
                    }
                    PotionEffectType type = ScriptParser.findEffect(a.arg(0));
                    if (type == null) return;
                    int seconds = (int) Double.parseDouble(a.arg(1));
                    int level = (int) Double.parseDouble(a.arg(2));
                    player.addPotionEffect(new PotionEffect(type, seconds * 20, level - 1, false, true));
                    session.effects.add(type);
                }
                case SET -> setVar(a.arg(0), operand(a.arg(1)), a.line());
                case ADD -> setVar(a.arg(0), valueOf(a.arg(0)) + operand(a.arg(1)), a.line());
                case TIMER -> {
                    if (a.arg(0).equals("start")) {
                        session.timerStart = System.currentTimeMillis();
                        session.timerFrozen = -1;
                    } else if (session.timerStart >= 0 && session.timerFrozen < 0) {
                        session.timerFrozen = System.currentTimeMillis() - session.timerStart;
                    }
                }
                default -> { }
            }
        }

        private Location coordinates(Action a) {
            Location base = player.getLocation();
            double x = coord(a.arg(0), base.getX());
            double y = coord(a.arg(1), base.getY());
            double z = coord(a.arg(2), base.getZ());
            float yaw   = a.arg(3) != null ? Float.parseFloat(a.arg(3)) : base.getYaw();
            float pitch = a.arg(4) != null ? Float.parseFloat(a.arg(4)) : base.getPitch();
            double limit = 29_999_000;
            x = Math.max(-limit, Math.min(limit, x));
            z = Math.max(-limit, Math.min(limit, z));
            y = Math.max(world.getMinHeight(), Math.min(world.getMaxHeight() + 64, y));
            return new Location(world, x, y, z, yaw, pitch);
        }

        private double coord(String raw, double current) {
            if (raw.startsWith("~")) {
                String rest = raw.substring(1);
                return current + (rest.isEmpty() ? 0 : Double.parseDouble(rest));
            }
            return Double.parseDouble(raw);
        }

        private void moveTo(Location loc) {
            if (!world.equals(loc.getWorld())) return;
            session.teleports++;
            player.setFallDistance(0f);
            player.setVelocity(new Vector());
            player.teleport(loc);
        }
    }
}
