package prs.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import prs.privateworld.PrivateWorld;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

public class ConfigManager {
    private PrivateWorld plugin;
    private FileConfiguration dataconfig = null;
    private File configfile = null;

    public ConfigManager(PrivateWorld plugin){
        this.plugin = plugin;
        saveDefaultConfig();
    }
    public void reloadConfig(){
        if (this.configfile == null) {
            this.configfile = new File(this.plugin.getDataFolder(), "config.yml");
        }
        this.dataconfig = YamlConfiguration.loadConfiguration(this.configfile);

        InputStream defaultStream = this.plugin.getResource("config.yml");
        if(defaultStream != null){
            YamlConfiguration defaultconfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
            this.dataconfig.setDefaults(defaultconfig);
        }
    }
    public FileConfiguration getConfig(){
        if (this.dataconfig == null){
            reloadConfig();
        }
        return this.dataconfig;
    }
    public void saveConfig(){
        if (this.dataconfig == null || this.configfile == null){
            return;
        }
        try {
            this.getConfig().save(this.configfile);
        }
        catch(IOException e){
            plugin.getLogger().log(Level.SEVERE, "Could not save config to " + this.configfile, e);
        }
    }
    public void saveDefaultConfig(){
        if(this.configfile == null){
            this.configfile = new File(this.plugin.getDataFolder(), "config.yml");
        }
        if (!this.configfile.exists()){
            this.plugin.saveResource("config.yml", false);
        }
    }

    /**
     * Returns the maximum number of private worlds a single player may own.
     * Configured via {@code max-worlds} in config.yml (default: 3).
     */
    public int getMaxWorlds() {
        return getConfig().getInt("max-worlds", 3);
    }

    // ---- Workshop scripts (config.yml "script" section) ----------------------

    public boolean isScriptEnabled() {
        return getConfig().getBoolean("script.enabled", true);
    }

    /** Maximum number of scripts a single player may author. */
    public int getMaxScriptsPerPlayer() {
        return getConfig().getInt("script.max-scripts-per-player", 10);
    }

    /** Maximum number of scripts installed in one world at the same time. */
    public int getMaxScriptsPerWorld() {
        return getConfig().getInt("script.max-installed-per-world", 5);
    }

    /** Maximum script source length in characters. */
    public int getMaxScriptLength() {
        return getConfig().getInt("script.max-source-length", 8000);
    }

    /** Maximum statements one trigger may execute (including after waits). */
    public int getMaxActionsPerRun() {
        return getConfig().getInt("script.max-actions-per-run", 200);
    }

    /** Maximum number of paused (waiting) script runs per world. */
    public int getMaxPendingPerWorld() {
        return getConfig().getInt("script.max-pending-per-world", 100);
    }
}
