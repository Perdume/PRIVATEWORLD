package prs.data;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import prs.world.WorldManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A Skript-inspired domain-specific language interpreter for PrivateWorld scripts.
 *
 * <h3>Supported syntax</h3>
 * <pre>
 * # 주석
 * broadcast &a{player}님이 입장했습니다
 * message {player} &6환영합니다
 * title {player}: &6제목: &7부제목
 * actionbar {player} &e액션바 텍스트
 * sound {player} ENTITY_PLAYER_LEVELUP
 * effect {player} speed 200 1
 * cleareffects {player}
 * give {player} DIAMOND 3
 * clearinventory {player}
 * teleport {player} 0 100 0
 * teleport {player} spawn
 * kill {player}
 * heal {player}
 * feed {player}
 * gamemode {player} adventure
 * setlevel {player} 10
 * setspeed {player} walk 0.2
 * settime 6000
 * setweather clear
 * command say hello
 *
 * set {score} to 0
 * add 1 to {score}
 * remove 1 from {score}
 * delete {score}
 *
 * if player is op:
 *     message {player} &c관리자입니다
 * else:
 *     message {player} &7일반 플레이어입니다
 * </pre>
 *
 * <h3>Conditions</h3>
 * <ul>
 *   <li>{@code player is op}</li>
 *   <li>{@code player has permission "node"}</li>
 *   <li>{@code player gamemode is survival}</li>
 *   <li>{@code player health > 10}</li>
 *   <li>{@code player health < 5}</li>
 *   <li>{@code player level > 5}</li>
 *   <li>{@code player has DIAMOND}</li>
 *   <li>{@code {var} equals value}</li>
 *   <li>{@code {var} > number}</li>
 *   <li>{@code {var} < number}</li>
 *   <li>{@code {var} is set}</li>
 *   <li>{@code not <condition>}</li>
 * </ul>
 *
 * <h3>Placeholders</h3>
 * {@code {player}}, {@code {world}}, {@code {owner}}, {@code {var}}
 */
public class ScriptDSL {

    // In-memory per-player variable store (reset on restart or quit)
    private static final ConcurrentHashMap<UUID, ConcurrentHashMap<String, String>>
            VARS = new ConcurrentHashMap<>();

    private final WorldManager worldMgr = new WorldManager();

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Parse and execute {@code rawScript} for the given player in the given world.
     */
    public void execute(String rawScript, Player player, World world) {
        if (rawScript == null || rawScript.isBlank()) return;

        String worldDisplay = world != null ? world.getName() : "";
        if (world != null) {
            UserWorldManager s = new UserWorldManager(world);
            String n = s.getWorldName();
            if (n != null && !n.isEmpty()) worldDisplay = n;
        }
        String ownerName = "Unknown";
        if (world != null) {
            OfflinePlayer owner = worldMgr.getWorldOwner(world);
            if (owner != null && owner.getName() != null) ownerName = owner.getName();
        }

        ExecContext ctx = new ExecContext(player, world, worldDisplay, ownerName);
        List<String> lines = Arrays.asList(rawScript.split("\n", -1));
        ParseResult root = parseBlock(lines, 0, 0);
        runBlock(root.stmts, ctx);
    }

    /** Remove in-memory variables for a player (call on disconnect). */
    public static void clearPlayerVars(UUID playerId) {
        VARS.remove(playerId);
    }

    // -------------------------------------------------------------------------
    // Parser
    // -------------------------------------------------------------------------

    private record ParseResult(List<Stmt> stmts, int nextLine) {}

    /**
     * Parse lines at the given {@code blockIndent}.
     * Returns when a line with indent less than {@code blockIndent} is found.
     */
    private ParseResult parseBlock(List<String> lines, int startLine, int blockIndent) {
        List<Stmt> stmts = new ArrayList<>();
        int i = startLine;
        while (i < lines.size()) {
            String raw = lines.get(i);
            String stripped = raw.stripLeading();

            if (stripped.isEmpty() || stripped.startsWith("#")) { i++; continue; }

            int indent = raw.length() - stripped.length();
            if (indent < blockIndent) break;        // exited block
            if (indent > blockIndent) { i++; continue; } // belongs to child block already parsed

            String lo = stripped.toLowerCase();

            if (lo.startsWith("if ") && lo.endsWith(":")) {
                String cond = stripped.substring(3, stripped.length() - 1).trim();
                i++;
                int childIndent = peekIndent(lines, i);
                ParseResult thenRes = parseBlock(lines, i, childIndent);
                i = thenRes.nextLine;

                List<Stmt> elseStmts = List.of();
                if (i < lines.size()) {
                    String nl = lines.get(i);
                    String ns = nl.stripLeading();
                    int ni = nl.length() - ns.length();
                    if (ni == blockIndent && ns.equalsIgnoreCase("else:")) {
                        i++;
                        int elseIndent = peekIndent(lines, i);
                        ParseResult elseRes = parseBlock(lines, i, elseIndent);
                        elseStmts = elseRes.stmts;
                        i = elseRes.nextLine;
                    }
                }
                stmts.add(new IfStmt(cond, thenRes.stmts, elseStmts));
            } else {
                stmts.add(new ActionStmt(stripped));
                i++;
            }
        }
        return new ParseResult(stmts, i);
    }

    private int peekIndent(List<String> lines, int from) {
        for (int i = from; i < lines.size(); i++) {
            String s = lines.get(i);
            String t = s.stripLeading();
            if (!t.isEmpty() && !t.startsWith("#")) return s.length() - t.length();
        }
        return 0;
    }

    // -------------------------------------------------------------------------
    // Statement types
    // -------------------------------------------------------------------------

    private interface Stmt { void run(ExecContext ctx, ScriptDSL dsl); }

    private record ActionStmt(String raw) implements Stmt {
        public void run(ExecContext ctx, ScriptDSL dsl) {
            dsl.dispatch(ctx.resolve(raw, dsl), ctx);
        }
    }

    private record IfStmt(String cond, List<Stmt> then, List<Stmt> els) implements Stmt {
        public void run(ExecContext ctx, ScriptDSL dsl) {
            if (dsl.evalCond(ctx.resolve(cond, dsl), ctx)) dsl.runBlock(then, ctx);
            else if (!els.isEmpty()) dsl.runBlock(els, ctx);
        }
    }

    // -------------------------------------------------------------------------
    // Execution context
    // -------------------------------------------------------------------------

    private static class ExecContext {
        final Player player;
        final World world;
        final String worldDisplay;
        final String ownerName;

        ExecContext(Player player, World world, String worldDisplay, String ownerName) {
            this.player = player;
            this.world = world;
            this.worldDisplay = worldDisplay;
            this.ownerName = ownerName;
        }

        /** Resolve system placeholders then user variables in {@code text}. */
        String resolve(String text, ScriptDSL dsl) {
            // Pass 1 – fixed system placeholders
            text = text.replace("{player}", player.getName())
                       .replace("{world}",  worldDisplay)
                       .replace("{owner}",  ownerName);
            // Pass 2 – user-defined {var} references
            StringBuilder sb = new StringBuilder(text.length() + 16);
            int pos = 0;
            while (pos < text.length()) {
                int s = text.indexOf('{', pos);
                if (s < 0) { sb.append(text, pos, text.length()); break; }
                sb.append(text, pos, s);
                int e = text.indexOf('}', s + 1);
                if (e < 0) { sb.append(text, s, text.length()); break; }
                String varName = text.substring(s + 1, e);
                String val = dsl.getVar(player.getUniqueId(), varName);
                sb.append(val != null ? val : "");
                pos = e + 1;
            }
            return sb.toString();
        }
    }

    // -------------------------------------------------------------------------
    // Executor helpers
    // -------------------------------------------------------------------------

    private void runBlock(List<Stmt> stmts, ExecContext ctx) {
        for (Stmt s : stmts) s.run(ctx, this);
    }

    // -------------------------------------------------------------------------
    // Condition evaluator
    // -------------------------------------------------------------------------

    boolean evalCond(String cond, ExecContext ctx) {
        String lo = cond.trim().toLowerCase();
        Player p = ctx.player;

        if (lo.startsWith("not ")) return !evalCond(cond.substring(4), ctx);
        if (lo.equals("player is op")) return p.isOp();

        if (lo.startsWith("player has permission ")) {
            String perm = unquote(cond.substring("player has permission ".length()).trim());
            return p.hasPermission(perm);
        }
        if (lo.startsWith("player gamemode is ")) {
            try { return p.getGameMode() == GameMode.valueOf(lo.substring(19).trim().toUpperCase()); }
            catch (IllegalArgumentException e) { return false; }
        }
        if (lo.startsWith("player health > ")) {
            try { return p.getHealth() > Double.parseDouble(lo.substring(16).trim()); }
            catch (NumberFormatException e) { return false; }
        }
        if (lo.startsWith("player health < ")) {
            try { return p.getHealth() < Double.parseDouble(lo.substring(16).trim()); }
            catch (NumberFormatException e) { return false; }
        }
        if (lo.startsWith("player level > ")) {
            try { return p.getLevel() > Integer.parseInt(lo.substring(15).trim()); }
            catch (NumberFormatException e) { return false; }
        }
        if (lo.startsWith("player level < ")) {
            try { return p.getLevel() < Integer.parseInt(lo.substring(15).trim()); }
            catch (NumberFormatException e) { return false; }
        }
        if (lo.startsWith("player has ")) {
            try { return p.getInventory().contains(Material.valueOf(lo.substring(11).trim().toUpperCase())); }
            catch (IllegalArgumentException e) { return false; }
        }

        // Variable conditions: {varName} equals/>/< value  or  {varName} is set
        if (lo.startsWith("{")) {
            int end = lo.indexOf('}');
            if (end > 0) {
                String varName = lo.substring(1, end);
                String rest = lo.substring(end + 1).trim();
                String varVal = getVar(p.getUniqueId(), varName);
                if (rest.equals("is set")) return varVal != null;
                if (rest.startsWith("equals ")) {
                    String expected = rest.substring(7).trim();
                    return expected.equals(varVal != null ? varVal.toLowerCase() : "");
                }
                if (rest.startsWith("> ")) {
                    try {
                        double threshold = Double.parseDouble(rest.substring(2).trim());
                        return parseNum(varVal) > threshold;
                    } catch (NumberFormatException e) { return false; }
                }
                if (rest.startsWith("< ")) {
                    try {
                        double threshold = Double.parseDouble(rest.substring(2).trim());
                        return parseNum(varVal) < threshold;
                    } catch (NumberFormatException e) { return false; }
                }
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Action dispatcher
    // -------------------------------------------------------------------------

    void dispatch(String line, ExecContext ctx) {
        if (line.isBlank() || line.startsWith("#")) return;
        String lo = line.toLowerCase();
        Player p = ctx.player;
        World world = ctx.world;
        UUID uid = p.getUniqueId();

        // ---- Variable operations ----
        if (lo.startsWith("set {")) {
            int end = lo.indexOf('}');
            if (end < 0) return;
            String varName = line.substring(5, end);
            String rest = line.substring(end + 1).trim();
            if (rest.toLowerCase().startsWith("to ")) setVar(uid, varName, rest.substring(3).trim());
            return;
        }
        if (lo.startsWith("add ")) {
            // add <n> to {var}
            int toIdx = lo.lastIndexOf(" to {");
            if (toIdx < 0) return;
            try {
                double n = Double.parseDouble(line.substring(4, toIdx).trim());
                String varName = line.substring(toIdx + 5, line.indexOf('}', toIdx + 5));
                setVar(uid, varName, fmtNum(parseNum(getVar(uid, varName)) + n));
            } catch (Exception ignored) {}
            return;
        }
        if (lo.startsWith("remove ")) {
            // remove <n> from {var}
            int fromIdx = lo.lastIndexOf(" from {");
            if (fromIdx < 0) return;
            try {
                double n = Double.parseDouble(line.substring(7, fromIdx).trim());
                String varName = line.substring(fromIdx + 7, line.indexOf('}', fromIdx + 7));
                setVar(uid, varName, fmtNum(parseNum(getVar(uid, varName)) - n));
            } catch (Exception ignored) {}
            return;
        }
        if (lo.startsWith("delete {")) {
            int end = lo.indexOf('}');
            if (end > 0) deleteVar(uid, line.substring(8, end));
            return;
        }

        // ---- Communication ----
        if (lo.startsWith("broadcast ")) {
            String msg = ChatColor.translateAlternateColorCodes('&', line.substring(10));
            if (world != null) { for (Player q : world.getPlayers()) q.sendMessage(msg); }
            else p.sendMessage(msg);
            return;
        }
        if (lo.startsWith("message ")) {
            String rest = line.substring(8);
            int sp = rest.indexOf(' ');
            if (sp < 0) return;
            Player t = resolvePlayer(rest.substring(0, sp).trim(), p);
            if (t != null) t.sendMessage(ChatColor.translateAlternateColorCodes('&', rest.substring(sp + 1)));
            return;
        }
        if (lo.startsWith("title ")) {
            String rest = line.substring(6);
            String[] parts = rest.split(":", 3);
            if (parts.length < 2) return;
            Player t = resolvePlayer(parts[0].trim(), p);
            if (t == null) return;
            String title = ChatColor.translateAlternateColorCodes('&', parts[1]);
            String sub   = parts.length >= 3 ? ChatColor.translateAlternateColorCodes('&', parts[2]) : "";
            t.sendTitle(title, sub, 10, 70, 20);
            return;
        }
        if (lo.startsWith("actionbar ")) {
            String rest = line.substring(10);
            int sp = rest.indexOf(' ');
            if (sp < 0) return;
            Player t = resolvePlayer(rest.substring(0, sp).trim(), p);
            if (t != null) t.sendActionBar(ChatColor.translateAlternateColorCodes('&', rest.substring(sp + 1)));
            return;
        }

        // ---- Sound / effects ----
        if (lo.startsWith("sound ")) {
            String rest = line.substring(6);
            int sp = rest.indexOf(' ');
            if (sp < 0) return;
            Player t = resolvePlayer(rest.substring(0, sp).trim(), p);
            if (t == null) return;
            try { t.playSound(t.getLocation(), Sound.valueOf(rest.substring(sp + 1).trim().toUpperCase()), 1f, 1f); }
            catch (IllegalArgumentException ignored) {}
            return;
        }
        if (lo.startsWith("effect ")) {
            String[] parts = line.substring(7).split(" ");
            if (parts.length < 3) return;
            Player t = resolvePlayer(parts[0].trim(), p);
            if (t == null) return;
            PotionEffectType type = resolveEffect(parts[1].trim());
            if (type == null) return;
            try {
                int dur = Integer.parseInt(parts[2].trim());
                int amp = parts.length >= 4 ? Integer.parseInt(parts[3].trim()) : 0;
                t.addPotionEffect(new PotionEffect(type, dur, amp));
            } catch (NumberFormatException ignored) {}
            return;
        }
        if (lo.startsWith("cleareffects ")) {
            Player t = resolvePlayer(line.substring(13).trim(), p);
            if (t != null) t.clearActivePotionEffects();
            return;
        }

        // ---- Inventory ----
        if (lo.startsWith("give ")) {
            String[] parts = line.substring(5).split(" ");
            if (parts.length < 2) return;
            Player t = resolvePlayer(parts[0].trim(), p);
            if (t == null) return;
            try {
                Material mat = Material.valueOf(parts[1].trim().toUpperCase());
                int amt = parts.length >= 3 ? Integer.parseInt(parts[2].trim()) : 1;
                t.getInventory().addItem(new ItemStack(mat, amt));
            } catch (IllegalArgumentException ignored) {}
            return;
        }
        if (lo.startsWith("clearinventory ")) {
            Player t = resolvePlayer(line.substring(15).trim(), p);
            if (t != null) t.getInventory().clear();
            return;
        }

        // ---- Player state ----
        if (lo.startsWith("teleport ")) {
            String[] parts = line.substring(9).split(" ");
            if (parts.length < 2) return;
            Player t = resolvePlayer(parts[0].trim(), p);
            if (t == null) return;
            if ("spawn".equalsIgnoreCase(parts[1])) {
                if (world != null) t.teleport(world.getSpawnLocation());
            } else if (parts.length >= 4) {
                try {
                    t.teleport(new Location(t.getWorld(),
                            Double.parseDouble(parts[1]),
                            Double.parseDouble(parts[2]),
                            Double.parseDouble(parts[3])));
                } catch (NumberFormatException ignored) {}
            }
            return;
        }
        if (lo.startsWith("kill ")) {
            Player t = resolvePlayer(line.substring(5).trim(), p);
            if (t != null) t.setHealth(0);
            return;
        }
        if (lo.startsWith("heal ")) {
            Player t = resolvePlayer(line.substring(5).trim(), p);
            if (t != null) t.setHealth(t.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue());
            return;
        }
        if (lo.startsWith("feed ")) {
            Player t = resolvePlayer(line.substring(5).trim(), p);
            if (t != null) t.setFoodLevel(20);
            return;
        }
        if (lo.startsWith("gamemode ")) {
            String[] parts = line.substring(9).split(" ");
            if (parts.length < 2) return;
            Player t = resolvePlayer(parts[0].trim(), p);
            if (t == null) return;
            try { t.setGameMode(GameMode.valueOf(parts[1].trim().toUpperCase())); }
            catch (IllegalArgumentException ignored) {}
            return;
        }
        if (lo.startsWith("setlevel ")) {
            String[] parts = line.substring(9).split(" ");
            if (parts.length < 2) return;
            Player t = resolvePlayer(parts[0].trim(), p);
            if (t == null) return;
            try { t.setLevel(Integer.parseInt(parts[1].trim())); }
            catch (NumberFormatException ignored) {}
            return;
        }
        if (lo.startsWith("setspeed ")) {
            // setspeed <player> walk|fly <value>
            String[] parts = line.substring(9).split(" ");
            if (parts.length < 3) return;
            Player t = resolvePlayer(parts[0].trim(), p);
            if (t == null) return;
            try {
                float speed = Float.parseFloat(parts[2].trim());
                if ("fly".equalsIgnoreCase(parts[1])) t.setFlySpeed(speed);
                else t.setWalkSpeed(speed);
            } catch (NumberFormatException ignored) {}
            return;
        }

        // ---- World ----
        if (lo.startsWith("settime ")) {
            if (world == null) return;
            try { world.setTime(Long.parseLong(line.substring(8).trim())); }
            catch (NumberFormatException ignored) {}
            return;
        }
        if (lo.startsWith("setweather ")) {
            if (world == null) return;
            String w = lo.substring(11).trim();
            world.setStorm(w.equals("rain") || w.equals("storm"));
            return;
        }

        // ---- Command ----
        if (lo.startsWith("command ")) {
            p.performCommand(line.substring(8).trim());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Player resolvePlayer(String expr, Player trigger) {
        if (expr.equalsIgnoreCase("player") || expr.equals("{player}")
                || expr.equalsIgnoreCase(trigger.getName())) return trigger;
        return Bukkit.getPlayerExact(expr);
    }

    @SuppressWarnings("deprecation")
    private static PotionEffectType resolveEffect(String name) {
        return PotionEffectType.getByName(name.toUpperCase());
    }

    private static String unquote(String s) {
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'")))
            return s.substring(1, s.length() - 1);
        return s;
    }

    private static double parseNum(String s) {
        if (s == null) return 0;
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0; }
    }

    private static String fmtNum(double n) {
        if (n == Math.floor(n) && !Double.isInfinite(n)) return String.valueOf((long) n);
        return String.valueOf(n);
    }

    // -------------------------------------------------------------------------
    // Variable store
    // -------------------------------------------------------------------------

    String getVar(UUID uid, String name) {
        ConcurrentHashMap<String, String> vars = VARS.get(uid);
        return vars != null ? vars.get(name.toLowerCase()) : null;
    }

    void setVar(UUID uid, String name, String value) {
        VARS.computeIfAbsent(uid, k -> new ConcurrentHashMap<>()).put(name.toLowerCase(), value);
    }

    void deleteVar(UUID uid, String name) {
        ConcurrentHashMap<String, String> vars = VARS.get(uid);
        if (vars != null) vars.remove(name.toLowerCase());
    }
}
