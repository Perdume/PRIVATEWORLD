package prs.data;

import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import prs.privateworld.PrivateWorld;
import prs.world.WorldManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Manages per-world event-driven scripts.
 *
 * <p>Scripts are stored in {@code plugins/Prs/scripts/{worldname}.yml} and
 * consist of action lines that fire on defined triggers:
 * <ul>
 *   <li>{@link Trigger#ENTER} – a player enters the world</li>
 *   <li>{@link Trigger#LEAVE} – a player leaves the world</li>
 *   <li>{@link Trigger#DEATH} – a player dies in the world</li>
 *   <li>{@link Trigger#RESPAWN} – a player respawns after dying in the world</li>
 * </ul>
 *
 * <h3>Supported actions</h3>
 * <pre>
 *   broadcast &amp;a{player}님이 입장했습니다
 *   message {player} &amp;6안녕하세요
 *   title {player}:&amp;6환영합니다:&amp;7{world}에 오신 것을 환영해요
 *   sound {player} ENTITY_PLAYER_LEVELUP
 *   effect {player} speed 200 1
 *   give {player} DIAMOND 3
 *   teleport {player} 0 100 0
 *   teleport {player} spawn
 *   kill {player}
 *   gamemode {player} adventure
 *   command <명령어>
 * </pre>
 *
 * <h3>Placeholders</h3>
 * <ul>
 *   <li>{@code {player}} – the triggering player's name</li>
 *   <li>{@code {world}}  – the world's display name (or Bukkit name)</li>
 *   <li>{@code {owner}}  – the world owner's name</li>
 * </ul>
 */
public class ScriptManager {

    // -------------------------------------------------------------------------
    // Trigger catalogue
    // -------------------------------------------------------------------------

    /** Events that can trigger a script. */
    public enum Trigger {
        ENTER  ("입장",   "플레이어가 월드에 입장할 때"),
        LEAVE  ("퇴장",   "플레이어가 월드에서 나갈 때"),
        DEATH  ("사망",   "플레이어가 월드에서 사망할 때"),
        RESPAWN("리스폰", "플레이어가 월드에서 리스폰할 때");

        public final String displayName;
        public final String description;

        Trigger(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
    }

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private final PrivateWorld plugin;
    private final WorldManager worldMgr = new WorldManager();
    private final File scriptsDir;

    public ScriptManager(PrivateWorld plugin) {
        this.plugin = plugin;
        this.scriptsDir = new File(plugin.getDataFolder(), "scripts");
        if (!scriptsDir.exists() && !scriptsDir.mkdirs()) {
            plugin.getLogger().severe(
                    "스크립트 디렉토리를 생성할 수 없습니다: " + scriptsDir.getAbsolutePath());
        }
    }

    // -------------------------------------------------------------------------
    // Storage helpers
    // -------------------------------------------------------------------------

    private File scriptFile(String worldName) {
        return new File(scriptsDir, worldName + ".yml");
    }

    private FileConfiguration loadConfig(String worldName) {
        return YamlConfiguration.loadConfiguration(scriptFile(worldName));
    }

    private void saveConfig(String worldName, FileConfiguration config) {
        try {
            config.save(scriptFile(worldName));
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "스크립트 저장 실패: " + worldName, e);
        }
    }

    // -------------------------------------------------------------------------
    // CRUD
    // -------------------------------------------------------------------------

    /**
     * Returns the action lines defined for {@code trigger} in {@code worldName}.
     * Returns an empty list when no script is set.
     */
    public List<String> getActions(String worldName, Trigger trigger) {
        List<String> result = loadConfig(worldName).getStringList(trigger.name());
        return result != null ? result : new ArrayList<>();
    }

    /**
     * Overwrites the action list for {@code trigger} in {@code worldName}.
     * Pass an empty list to remove the trigger's script.
     */
    public void setActions(String worldName, Trigger trigger, List<String> actions) {
        FileConfiguration config = loadConfig(worldName);
        config.set(trigger.name(), actions.isEmpty() ? null : actions);
        saveConfig(worldName, config);
    }

    /** Returns {@code true} if the world has at least one non-empty script. */
    public boolean hasAnyScript(String worldName) {
        for (Trigger t : Trigger.values()) {
            if (!getActions(worldName, t).isEmpty()) return true;
        }
        return false;
    }

    /**
     * Deletes the script file for the given world (called on world deletion).
     */
    public void deleteScripts(String worldName) {
        File f = scriptFile(worldName);
        if (f.exists() && !f.delete()) {
            plugin.getLogger().warning("스크립트 파일을 삭제할 수 없습니다: " + f.getAbsolutePath());
        }
    }

    // -------------------------------------------------------------------------
    // Execution
    // -------------------------------------------------------------------------

    /**
     * Fires all actions for {@code trigger} in {@code worldName} for the
     * given {@code player}.  Does nothing if no actions are defined.
     */
    public void fire(String worldName, Trigger trigger, Player player) {
        List<String> actions = getActions(worldName, trigger);
        if (actions.isEmpty()) return;

        World world = Bukkit.getWorld(worldName);

        String worldDisplayName = worldName;
        if (world != null) {
            UserWorldManager settings = new UserWorldManager(world);
            String n = settings.getWorldName();
            if (n != null && !n.isEmpty()) worldDisplayName = n;
        }

        String ownerName = "Unknown";
        if (world != null) {
            OfflinePlayer owner = worldMgr.getWorldOwner(world);
            if (owner != null && owner.getName() != null) ownerName = owner.getName();
        }

        for (String rawLine : actions) {
            String line = rawLine
                    .replace("{player}", player.getName())
                    .replace("{world}",  worldDisplayName)
                    .replace("{owner}",  ownerName);
            executeAction(line.trim(), player, world);
        }
    }

    // -------------------------------------------------------------------------
    // Action dispatcher
    // -------------------------------------------------------------------------

    private void executeAction(String line, Player player, World world) {
        if (line.isEmpty() || line.startsWith("#")) return;

        String lower = line.toLowerCase();

        if (lower.startsWith("broadcast ")) {
            String msg = ChatColor.translateAlternateColorCodes('&', line.substring(10));
            if (world != null) {
                for (Player p : world.getPlayers()) p.sendMessage(msg);
            } else {
                player.sendMessage(msg);
            }

        } else if (lower.startsWith("message ")) {
            // message <player> <text>
            String rest = line.substring(8);
            int sp = rest.indexOf(' ');
            if (sp < 0) return;
            Player target = Bukkit.getPlayerExact(rest.substring(0, sp).trim());
            if (target != null) target.sendMessage(
                    ChatColor.translateAlternateColorCodes('&', rest.substring(sp + 1)));

        } else if (lower.startsWith("title ")) {
            // title <player>:<title>:<subtitle>
            String rest = line.substring(6);
            String[] parts = rest.split(":", 3);
            if (parts.length < 2) return;
            Player target = Bukkit.getPlayerExact(parts[0].trim());
            if (target == null) return;
            String title    = ChatColor.translateAlternateColorCodes('&', parts[1]);
            String subtitle = parts.length >= 3
                    ? ChatColor.translateAlternateColorCodes('&', parts[2]) : "";
            target.sendTitle(title, subtitle, 10, 70, 20);

        } else if (lower.startsWith("sound ")) {
            // sound <player> <sound_name>
            String rest = line.substring(6);
            int sp = rest.indexOf(' ');
            if (sp < 0) return;
            Player target = Bukkit.getPlayerExact(rest.substring(0, sp).trim());
            if (target == null) return;
            try {
                Sound sound = Sound.valueOf(rest.substring(sp + 1).trim().toUpperCase());
                target.playSound(target.getLocation(), sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException ignored) {}

        } else if (lower.startsWith("effect ")) {
            // effect <player> <effect_name> <duration_ticks> [amplifier]
            String[] parts = line.substring(7).split(" ");
            if (parts.length < 3) return;
            Player target = Bukkit.getPlayerExact(parts[0].trim());
            if (target == null) return;
            PotionEffectType type = resolveEffectType(parts[1].trim());
            if (type == null) return;
            try {
                int duration  = Integer.parseInt(parts[2].trim());
                int amplifier = parts.length >= 4 ? Integer.parseInt(parts[3].trim()) : 0;
                target.addPotionEffect(new PotionEffect(type, duration, amplifier));
            } catch (NumberFormatException ignored) {}

        } else if (lower.startsWith("give ")) {
            // give <player> <material> [amount]
            String[] parts = line.substring(5).split(" ");
            if (parts.length < 2) return;
            Player target = Bukkit.getPlayerExact(parts[0].trim());
            if (target == null) return;
            try {
                Material mat = Material.valueOf(parts[1].trim().toUpperCase());
                int amount = parts.length >= 3 ? Integer.parseInt(parts[2].trim()) : 1;
                target.getInventory().addItem(new ItemStack(mat, amount));
            } catch (IllegalArgumentException ignored) {}

        } else if (lower.startsWith("teleport ")) {
            // teleport <player> spawn  OR  teleport <player> <x> <y> <z>
            String[] parts = line.substring(9).split(" ");
            if (parts.length < 2) return;
            Player target = Bukkit.getPlayerExact(parts[0].trim());
            if (target == null) return;
            if ("spawn".equalsIgnoreCase(parts[1])) {
                if (world != null) target.teleport(world.getSpawnLocation());
            } else if (parts.length >= 4) {
                try {
                    double x = Double.parseDouble(parts[1]);
                    double y = Double.parseDouble(parts[2]);
                    double z = Double.parseDouble(parts[3]);
                    target.teleport(new Location(target.getWorld(), x, y, z));
                } catch (NumberFormatException ignored) {}
            }

        } else if (lower.startsWith("kill ")) {
            Player target = Bukkit.getPlayerExact(line.substring(5).trim());
            if (target != null) target.setHealth(0);

        } else if (lower.startsWith("gamemode ")) {
            // gamemode <player> <mode>
            String[] parts = line.substring(9).split(" ");
            if (parts.length < 2) return;
            Player target = Bukkit.getPlayerExact(parts[0].trim());
            if (target == null) return;
            try {
                GameMode gm = GameMode.valueOf(parts[1].trim().toUpperCase());
                target.setGameMode(gm);
            } catch (IllegalArgumentException ignored) {}

        } else if (lower.startsWith("command ")) {
            // command <cmd> — executed as the triggering player
            // (bound by the player's own permissions for safety)
            player.performCommand(line.substring(8).trim());
        }
    }

    /** Resolves a potion effect type by name (case-insensitive). */
    @SuppressWarnings("deprecation")
    private static PotionEffectType resolveEffectType(String name) {
        return PotionEffectType.getByName(name.toUpperCase());
    }
}
