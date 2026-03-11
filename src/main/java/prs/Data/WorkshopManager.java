package prs.Data;

import org.bukkit.ChatColor;
import org.bukkit.Material;
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
 * Manages two Workshop sub-systems:
 * <ol>
 *   <li><b>Published worlds</b> – players publish their world as a particular
 *       content type (parkour, PvP, etc.) so others can browse and join it.
 *       Persisted in {@code plugins/Prs/workshop/published.yml}.</li>
 *   <li><b>Option presets</b> – named snapshots of world-option settings that
 *       can be applied to any world.
 *       Persisted in {@code plugins/Prs/workshop/presets.yml}.</li>
 * </ol>
 */
public class WorkshopManager {

    // -------------------------------------------------------------------------
    // Content-type catalogue
    // -------------------------------------------------------------------------

    /**
     * Game-content categories a world owner can tag their world with when
     * registering it in the Workshop.
     */
    public enum ContentType {
        PARKOUR  ("파쿠르",    Material.DIAMOND_BOOTS,  ChatColor.AQUA),
        PVP      ("PVP",      Material.DIAMOND_SWORD,  ChatColor.RED),
        SANDBOX  ("자유 건축", Material.GRASS_BLOCK,    ChatColor.GREEN),
        ADVENTURE("어드벤처",  Material.MAP,            ChatColor.GOLD),
        MINIGAME ("미니게임",  Material.COMPARATOR,     ChatColor.LIGHT_PURPLE),
        OTHER    ("기타",      Material.BOOK,           ChatColor.GRAY);

        public final String displayName;
        public final Material icon;
        public final ChatColor color;

        ContentType(String displayName, Material icon, ChatColor color) {
            this.displayName = displayName;
            this.icon = icon;
            this.color = color;
        }
    }

    // -------------------------------------------------------------------------
    // Internal state
    // -------------------------------------------------------------------------

    private static final String PRESETS_ROOT   = "presets";
    private static final String PUBLISHED_ROOT = "worlds";

    private final PrivateWorld plugin;

    /** Stores option-snapshot presets. */
    private final File presetFile;
    private FileConfiguration presetConfig;

    /** Stores published-world entries (content browser). */
    private final File publishedFile;
    private FileConfiguration publishedConfig;

    public WorkshopManager(PrivateWorld plugin) {
        this.plugin = plugin;
        File dir = new File(plugin.getDataFolder(), "workshop");
        if (!dir.exists() && !dir.mkdirs()) {
            plugin.getLogger().severe("워크샵 디렉토리를 생성할 수 없습니다: " + dir.getAbsolutePath());
        }
        presetFile    = new File(dir, "presets.yml");
        publishedFile = new File(dir, "published.yml");
        reload();
    }

    /** Reload both data files from disk. */
    public void reload() {
        presetConfig    = YamlConfiguration.loadConfiguration(presetFile);
        publishedConfig = YamlConfiguration.loadConfiguration(publishedFile);
    }

    private void savePresets() {
        try {
            presetConfig.save(presetFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "워크샵 프리셋을 저장할 수 없습니다", e);
        }
    }

    private void savePublished() {
        try {
            publishedConfig.save(publishedFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "워크샵 등록 데이터를 저장할 수 없습니다", e);
        }
    }

    // Alias kept so that both preset-section methods (savePreset / deletePreset)
    // continue calling a single method without cluttering call sites.
    private void save() { savePresets(); }

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

    // =========================================================================
    // Published-world methods  (content browser)
    // =========================================================================

    /**
     * Publish a world in the Workshop under the given {@link ContentType}.
     *
     * @param author     the player who owns the world
     * @param worldName  Bukkit world name (used as the unique key)
     * @param type       the content category
     * @param title      human-readable title shown in the GUI
     */
    public void publishWorld(Player author, String worldName, ContentType type, String title) {
        String base = PUBLISHED_ROOT + "." + worldName;
        publishedConfig.set(base + ".title",      title);
        publishedConfig.set(base + ".type",        type.name());
        publishedConfig.set(base + ".author",      author.getUniqueId().toString());
        publishedConfig.set(base + ".authorName",  author.getName());
        savePublished();
    }

    /**
     * Remove a world from the Workshop.
     * Returns {@code false} if it was not published.
     */
    public boolean unpublishWorld(String worldName) {
        if (!publishedConfig.contains(PUBLISHED_ROOT + "." + worldName)) return false;
        publishedConfig.set(PUBLISHED_ROOT + "." + worldName, null);
        savePublished();
        return true;
    }

    /** Returns {@code true} if the world is currently published. */
    public boolean isPublished(String worldName) {
        return publishedConfig.contains(PUBLISHED_ROOT + "." + worldName);
    }

    /** Returns all published world names. */
    public List<String> getAllPublishedWorlds() {
        if (!publishedConfig.contains(PUBLISHED_ROOT)) return new ArrayList<>();
        var section = publishedConfig.getConfigurationSection(PUBLISHED_ROOT);
        if (section == null) return new ArrayList<>();
        return new ArrayList<>(section.getKeys(false));
    }

    /**
     * Returns world names that are published under the given {@link ContentType}.
     */
    public List<String> getPublishedWorldsByType(ContentType type) {
        List<String> result = new ArrayList<>();
        for (String worldName : getAllPublishedWorlds()) {
            if (type.name().equals(publishedConfig.getString(PUBLISHED_ROOT + "." + worldName + ".type"))) {
                result.add(worldName);
            }
        }
        return result;
    }

    /** Returns the display title of a published world, or the world name as fallback. */
    public String getPublishedTitle(String worldName) {
        return publishedConfig.getString(PUBLISHED_ROOT + "." + worldName + ".title", worldName);
    }

    /** Returns the {@link ContentType} of a published world, or {@code OTHER} as fallback. */
    public ContentType getPublishedType(String worldName) {
        String s = publishedConfig.getString(PUBLISHED_ROOT + "." + worldName + ".type");
        if (s == null) return ContentType.OTHER;
        try {
            return ContentType.valueOf(s);
        } catch (IllegalArgumentException e) {
            return ContentType.OTHER;
        }
    }

    /** Returns the display name of the author of a published world. */
    public String getPublishedAuthorName(String worldName) {
        return publishedConfig.getString(PUBLISHED_ROOT + "." + worldName + ".authorName", "Unknown");
    }

    /** Returns the UUID of the author of a published world, or {@code null} when absent/invalid. */
    public UUID getPublishedAuthor(String worldName) {
        String s = publishedConfig.getString(PUBLISHED_ROOT + "." + worldName + ".author");
        if (s == null) return null;
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
