package prs.data;

import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import prs.privateworld.PrivateWorld;
import prs.script.CompiledScript;
import prs.script.ScriptParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Stores player-written Workshop scripts and tracks which worlds they are
 * installed in.
 *
 * <p>Each script has a <b>draft</b> source (edited through a Book &amp; Quill)
 * and, once published, a frozen <b>published</b> source that other players
 * install. Worlds owned by the script's author run the draft so the author can
 * test changes immediately; every other world runs the published version.</p>
 *
 * Persisted in {@code plugins/Prs/workshop/scripts.yml}; the list of scripts
 * installed in a world lives in that world's {@link UserWorldManager} file.
 */
public class ScriptManager {

    private static final String ROOT = "scripts";

    public static final String TEMPLATE = String.join("\n",
            "# 내 스크립트! 다 쓰면 '완료'를 누르세요",
            "# # 뒤는 주석입니다",
            "",
            "on join",
            "  message \"&a{player}님 환영합니다!\"",
            "  checkpoint",
            "  timer start",
            "",
            "on fall 0",
            "  respawn",
            "",
            "on step gold_block",
            "  checkpoint",
            "  actionbar \"&e체크포인트 저장!\"",
            "",
            "on step diamond_block",
            "  timer stop",
            "  broadcast \"&b{player}님 {time}초 클리어!\"",
            "  sound ui.toast.challenge_complete");

    /** A script resolved for a specific world, ready to execute. */
    public record LoadedScript(String id, String name, UUID author, CompiledScript script) {}

    /** Everything the runtime needs to know about one world's scripts. */
    public record WorldScripts(UUID owner, String displayName, List<LoadedScript> scripts) {
        static final WorldScripts EMPTY = new WorldScripts(null, "", Collections.emptyList());

        public boolean isEmpty() {
            return scripts.isEmpty();
        }
    }

    private final PrivateWorld plugin;
    private final File file;
    private FileConfiguration config;

    private final Map<String, ScriptParser.Result> draftCache     = new HashMap<>();
    private final Map<String, ScriptParser.Result> publishedCache = new HashMap<>();
    private final Map<String, WorldScripts> worldCache            = new HashMap<>();

    public ScriptManager(PrivateWorld plugin) {
        this.plugin = plugin;
        File dir = new File(plugin.getDataFolder(), "workshop");
        if (!dir.exists() && !dir.mkdirs()) {
            plugin.getLogger().severe("워크샵 디렉토리를 생성할 수 없습니다: " + dir.getAbsolutePath());
        }
        file = new File(dir, "scripts.yml");
        reload();
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(file);
        draftCache.clear();
        publishedCache.clear();
        worldCache.clear();
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "워크샵 스크립트를 저장할 수 없습니다", e);
        }
    }

    private String path(String id, String key) {
        return ROOT + "." + id + "." + key;
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    public List<String> getAllScriptIds() {
        ConfigurationSection section = config.getConfigurationSection(ROOT);
        if (section == null) return new ArrayList<>();
        return new ArrayList<>(section.getKeys(false));
    }

    public List<String> getScriptsByAuthor(UUID authorId) {
        List<String> result = new ArrayList<>();
        for (String id : getAllScriptIds()) {
            if (authorId.equals(getAuthor(id))) result.add(id);
        }
        return result;
    }

    public List<String> getPublishedScriptIds() {
        List<String> result = new ArrayList<>();
        for (String id : getAllScriptIds()) {
            if (isPublished(id)) result.add(id);
        }
        return result;
    }

    public boolean exists(String id) {
        return id != null && config.isConfigurationSection(ROOT + "." + id);
    }

    public String getName(String id) {
        return config.getString(path(id, "name"), id);
    }

    public String getAuthorName(String id) {
        return config.getString(path(id, "authorName"), "Unknown");
    }

    /** Returns the author UUID, or {@code null} if the field is missing or malformed. */
    public UUID getAuthor(String id) {
        String s = config.getString(path(id, "author"));
        if (s == null) return null;
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean isAuthor(String id, Player p) {
        return p.getUniqueId().equals(getAuthor(id));
    }

    public String getSource(String id) {
        return config.getString(path(id, "source"), "");
    }

    public String getPublishedSource(String id) {
        return config.getString(path(id, "publishedSource"), "");
    }

    public boolean isPublished(String id) {
        return config.getBoolean(path(id, "published"), false);
    }

    /** {@code true} when the draft differs from what was last published. */
    public boolean hasUnpublishedChanges(String id) {
        return !isPublished(id) || !getSource(id).equals(getPublishedSource(id));
    }

    public int getVersion(String id) {
        return config.getInt(path(id, "version"), 0);
    }

    public int getInstalls(String id) {
        return config.getInt(path(id, "installs"), 0);
    }

    /** Finds a script by its display name (case-insensitive). */
    public String findIdByName(String name) {
        for (String id : getAllScriptIds()) {
            if (name.equalsIgnoreCase(getName(id))) return id;
        }
        return null;
    }

    public static int countLines(String source) {
        int n = 0;
        for (String line : source.split("\n")) {
            String t = line.trim();
            if (!t.isEmpty() && !t.startsWith("#")) n++;
        }
        return n;
    }

    // -------------------------------------------------------------------------
    // Compilation (cached)
    // -------------------------------------------------------------------------

    public ScriptParser.Result compileDraft(String id) {
        return draftCache.computeIfAbsent(id, k -> ScriptParser.parse(getSource(k)));
    }

    public ScriptParser.Result compilePublished(String id) {
        return publishedCache.computeIfAbsent(id, k -> ScriptParser.parse(getPublishedSource(k)));
    }

    // -------------------------------------------------------------------------
    // Mutations
    // -------------------------------------------------------------------------

    /** Creates a new script owned by {@code author} with the starter template. Returns its ID. */
    public String createScript(Player author, String name) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        while (exists(id)) id = UUID.randomUUID().toString().substring(0, 8);
        long now = System.currentTimeMillis();
        config.set(path(id, "name"), name);
        config.set(path(id, "author"), author.getUniqueId().toString());
        config.set(path(id, "authorName"), author.getName());
        config.set(path(id, "source"), TEMPLATE);
        config.set(path(id, "published"), false);
        config.set(path(id, "version"), 0);
        config.set(path(id, "installs"), 0);
        config.set(path(id, "created"), now);
        config.set(path(id, "updated"), now);
        save();
        return id;
    }

    public void updateSource(String id, String source) {
        config.set(path(id, "source"), source);
        config.set(path(id, "updated"), System.currentTimeMillis());
        save();
        draftCache.remove(id);
        worldCache.clear();
    }

    public void rename(String id, String name) {
        config.set(path(id, "name"), name);
        save();
        worldCache.clear();
    }

    /**
     * Publishes the current draft. Nothing changes when the draft has errors;
     * the returned result lists them.
     */
    public ScriptParser.Result publish(String id) {
        ScriptParser.Result result = compileDraft(id);
        if (!result.ok()) return result;
        config.set(path(id, "publishedSource"), getSource(id));
        config.set(path(id, "published"), true);
        config.set(path(id, "version"), getVersion(id) + 1);
        config.set(path(id, "publishedAt"), System.currentTimeMillis());
        save();
        publishedCache.remove(id);
        worldCache.clear();
        return result;
    }

    public boolean unpublish(String id) {
        if (!isPublished(id)) return false;
        config.set(path(id, "published"), false);
        save();
        worldCache.clear();
        return true;
    }

    public boolean delete(String id) {
        if (!exists(id)) return false;
        config.set(ROOT + "." + id, null);
        save();
        draftCache.remove(id);
        publishedCache.remove(id);
        worldCache.clear();
        return true;
    }

    // -------------------------------------------------------------------------
    // Installation
    // -------------------------------------------------------------------------

    public enum InstallResult { OK, NOT_FOUND, NOT_PUBLISHED, ALREADY_INSTALLED, LIMIT }

    /** Installed script IDs of a world, skipping scripts that no longer exist. */
    public List<String> getInstalledIds(World w) {
        List<String> result = new ArrayList<>();
        for (String id : new UserWorldManager(w).getInstalledScripts()) {
            if (exists(id)) result.add(id);
        }
        return result;
    }

    public boolean isInstalled(World w, String id) {
        return new UserWorldManager(w).getInstalledScripts().contains(id);
    }

    /**
     * Installs a script into a world. The world owner may install their own
     * drafts; other people's scripts must be published.
     */
    public InstallResult install(World w, String id, Player installer) {
        if (!exists(id)) return InstallResult.NOT_FOUND;
        if (!isAuthor(id, installer) && !isPublished(id)) return InstallResult.NOT_PUBLISHED;
        UserWorldManager worldSettings = new UserWorldManager(w);
        List<String> installed = worldSettings.getInstalledScripts();
        if (installed.contains(id)) return InstallResult.ALREADY_INSTALLED;
        installed.removeIf(s -> !exists(s));
        if (installed.size() >= plugin.configManager.getMaxScriptsPerWorld()) return InstallResult.LIMIT;
        installed.add(id);
        worldSettings.setInstalledScripts(installed);
        config.set(path(id, "installs"), getInstalls(id) + 1);
        save();
        worldCache.remove(w.getName());
        return InstallResult.OK;
    }

    public boolean uninstall(World w, String id) {
        UserWorldManager worldSettings = new UserWorldManager(w);
        List<String> installed = worldSettings.getInstalledScripts();
        if (!installed.remove(id)) return false;
        worldSettings.setInstalledScripts(installed);
        if (exists(id)) {
            config.set(path(id, "installs"), Math.max(0, getInstalls(id) - 1));
            save();
        }
        worldCache.remove(w.getName());
        return true;
    }

    /** Called before a world's files are deleted so install counters stay accurate. */
    public void onWorldDeleted(World w) {
        boolean changed = false;
        for (String id : getInstalledIds(w)) {
            config.set(path(id, "installs"), Math.max(0, getInstalls(id) - 1));
            changed = true;
        }
        if (changed) save();
        worldCache.remove(w.getName());
    }

    // -------------------------------------------------------------------------
    // Runtime lookup
    // -------------------------------------------------------------------------

    /** Scripts that should run in the given world (cached until something changes). */
    public WorldScripts getWorldScripts(World w) {
        if (w == null) return WorldScripts.EMPTY;
        WorldScripts cached = worldCache.get(w.getName());
        if (cached != null) return cached;
        WorldScripts resolved = resolve(w);
        worldCache.put(w.getName(), resolved);
        return resolved;
    }

    public boolean hasScripts(World w) {
        return plugin.configManager.isScriptEnabled() && !getWorldScripts(w).isEmpty();
    }

    private WorldScripts resolve(World w) {
        UUID owner = ownerOf(w);
        if (owner == null) return WorldScripts.EMPTY;

        UserWorldManager worldSettings = new UserWorldManager(w);
        List<LoadedScript> scripts = new ArrayList<>();
        for (String id : worldSettings.getInstalledScripts()) {
            if (!exists(id)) continue;
            UUID author = getAuthor(id);
            CompiledScript compiled = null;
            if (owner.equals(author)) {
                // Author's own world: run the draft, fall back to the last published version
                ScriptParser.Result draft = compileDraft(id);
                if (draft.ok()) compiled = draft.script();
            }
            if (compiled == null && isPublished(id)) {
                ScriptParser.Result published = compilePublished(id);
                if (published.ok()) compiled = published.script();
            }
            if (compiled != null) scripts.add(new LoadedScript(id, getName(id), author, compiled));
        }
        if (scripts.isEmpty()) return WorldScripts.EMPTY;
        String name = worldSettings.getWorldName();
        return new WorldScripts(owner, name != null ? name : w.getName(), scripts);
    }

    /** Owner of a private world ({@code <uuid>--<n>}), or {@code null} for other worlds. */
    public static UUID ownerOf(World w) {
        String name = w.getName();
        int idx = name.indexOf("--");
        if (idx <= 0) return null;
        try {
            return UUID.fromString(name.substring(0, idx));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
