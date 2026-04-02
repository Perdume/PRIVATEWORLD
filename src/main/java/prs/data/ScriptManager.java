package prs.data;

import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import prs.privateworld.PrivateWorld;
import prs.world.WorldManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

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
    /** DSL interpreter instance (stateless parser, stateful only for player vars). */
    private final ScriptDSL dsl = new ScriptDSL();

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
     * Returns the raw DSL text for the given trigger.
     * Migrates old list-based storage automatically and saves the result.
     */
    public String getRawScript(String worldName, Trigger trigger) {
        FileConfiguration config = loadConfig(worldName);
        Object value = config.get(trigger.name());
        if (value instanceof List<?> list) {
            // Migrate from old action-line list format: join, save, return
            String migrated = list.stream().map(Object::toString).collect(Collectors.joining("\n"));
            config.set(trigger.name(), migrated.isBlank() ? null : migrated);
            saveConfig(worldName, config);
            return migrated;
        }
        if (value instanceof String s) return s;
        return "";
    }

    /**
     * Overwrites the raw DSL text for the given trigger.
     * Pass an empty string to clear the script.
     */
    public void setRawScript(String worldName, Trigger trigger, String rawText) {
        FileConfiguration config = loadConfig(worldName);
        config.set(trigger.name(), rawText.isBlank() ? null : rawText);
        saveConfig(worldName, config);
    }

    /**
     * Returns the action lines defined for {@code trigger} in {@code worldName}.
     * Kept for backward-compat; splits the raw script by line.
     *
     * @deprecated Use {@link #getRawScript} / {@link #setRawScript} instead.
     */
    @Deprecated
    public List<String> getActions(String worldName, Trigger trigger) {
        String raw = getRawScript(worldName, trigger);
        if (raw.isBlank()) return new ArrayList<>();
        List<String> lines = new ArrayList<>();
        for (String line : raw.split("\n", -1)) {
            String t = line.strip();
            if (!t.isEmpty() && !t.startsWith("#")) lines.add(t);
        }
        return lines;
    }

    /**
     * Overwrites the action list for {@code trigger}.
     *
     * @deprecated Use {@link #setRawScript} instead.
     */
    @Deprecated
    public void setActions(String worldName, Trigger trigger, List<String> actions) {
        setRawScript(worldName, trigger, String.join("\n", actions));
    }

    /** Returns {@code true} if the world has at least one non-empty script. */
    public boolean hasAnyScript(String worldName) {
        for (Trigger t : Trigger.values()) {
            if (!getRawScript(worldName, t).isBlank()) return true;
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
     * Fires the DSL script for {@code trigger} in {@code worldName} for the
     * given {@code player}.  Does nothing if no script is set.
     */
    public void fire(String worldName, Trigger trigger, Player player) {
        String rawScript = getRawScript(worldName, trigger);
        if (rawScript.isBlank()) return;
        World world = Bukkit.getWorld(worldName);
        dsl.execute(rawScript, player, world);
    }
}
