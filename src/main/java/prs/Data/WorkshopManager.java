package prs.Data;

import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import prs.privateworld.PrivateWorld;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Manages Workshop presets: named snapshots of a world's option settings
 * that any player can share and apply to their own world.
 *
 * Data is persisted in {@code plugins/Prs/workshop/presets.yml}.
 */
public class WorkshopManager {

    private static final String PRESETS_ROOT = "presets";

    private final PrivateWorld plugin;
    private final File presetFile;
    private FileConfiguration presetConfig;

    public WorkshopManager(PrivateWorld plugin) {
        this.plugin = plugin;
        File dir = new File(plugin.getDataFolder(), "workshop");
        if (!dir.exists() && !dir.mkdirs()) {
            plugin.getLogger().severe("워크샵 디렉토리를 생성할 수 없습니다: " + dir.getAbsolutePath());
        }
        presetFile = new File(dir, "presets.yml");
        reload();
    }

    /** Reload presets from disk. */
    public void reload() {
        presetConfig = YamlConfiguration.loadConfiguration(presetFile);
    }

    private void save() {
        try {
            presetConfig.save(presetFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "워크샵 프리셋을 저장할 수 없습니다", e);
        }
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /** Returns all preset IDs in insertion order. */
    public List<String> getAllPresetIds() {
        if (!presetConfig.contains(PRESETS_ROOT)) return new ArrayList<>();
        var section = presetConfig.getConfigurationSection(PRESETS_ROOT);
        if (section == null) return new ArrayList<>();
        return new ArrayList<>(section.getKeys(false));
    }

    /** Returns preset IDs authored by the given player UUID. */
    public List<String> getPresetsByAuthor(UUID authorId) {
        List<String> result = new ArrayList<>();
        for (String id : getAllPresetIds()) {
            if (authorId.toString().equals(presetConfig.getString(PRESETS_ROOT + "." + id + ".author"))) {
                result.add(id);
            }
        }
        return result;
    }

    public String getPresetName(String id) {
        return presetConfig.getString(PRESETS_ROOT + "." + id + ".name", id);
    }

    public String getPresetAuthorName(String id) {
        return presetConfig.getString(PRESETS_ROOT + "." + id + ".authorName", "Unknown");
    }

    /** Returns the author UUID, or {@code null} if the field is missing or malformed. */
    public UUID getPresetAuthor(String id) {
        String s = presetConfig.getString(PRESETS_ROOT + "." + id + ".author");
        if (s == null) return null;
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Find the first preset whose display name matches {@code name}
     * (case-insensitive). Returns {@code null} when not found.
     */
    public String findPresetIdByName(String name) {
        for (String id : getAllPresetIds()) {
            if (name.equalsIgnoreCase(getPresetName(id))) return id;
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Mutations
    // -------------------------------------------------------------------------

    /**
     * Snapshot the world's current option settings and persist them as a new
     * preset. Returns the newly generated preset ID.
     */
    public String savePreset(Player author, String presetName, World world) {
        UserWorldManager uwm = new UserWorldManager(world);
        String id = UUID.randomUUID().toString();
        String base = PRESETS_ROOT + "." + id;
        presetConfig.set(base + ".name", presetName);
        presetConfig.set(base + ".author", author.getUniqueId().toString());
        presetConfig.set(base + ".authorName", author.getName());
        for (UserWorldManager.WorldOption opt : UserWorldManager.WorldOption.values()) {
            presetConfig.set(base + ".options." + opt.name(), uwm.getOption(opt));
        }
        save();
        return id;
    }

    /**
     * Copy every option stored in the preset onto the target world.
     * Returns {@code false} if the preset ID does not exist.
     */
    public boolean applyPreset(String presetId, World world) {
        String base = PRESETS_ROOT + "." + presetId;
        if (!presetConfig.contains(base)) return false;
        UserWorldManager uwm = new UserWorldManager(world);
        for (UserWorldManager.WorldOption opt : UserWorldManager.WorldOption.values()) {
            boolean val = presetConfig.getBoolean(base + ".options." + opt.name(), false);
            uwm.setOption(opt, val);
        }
        return true;
    }

    /**
     * Delete the preset with the given ID.
     * Returns {@code false} if the ID does not exist.
     */
    public boolean deletePreset(String presetId) {
        if (!presetConfig.contains(PRESETS_ROOT + "." + presetId)) return false;
        presetConfig.set(PRESETS_ROOT + "." + presetId, null);
        save();
        return true;
    }
}
