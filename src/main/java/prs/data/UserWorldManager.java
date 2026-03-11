package prs.data;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class UserWorldManager {
    private World w;
    private File userFile;
    private FileConfiguration userConfig;

    public enum WorldOption {
        PRIVATE("Option.Private"),
        CAN_DROP("Option.canDrop"),
        CAN_BREAK("Option.canBreak"),
        CAN_PLACE("Option.canPlace"),
        CAN_SHOOT("Option.canShoot"),
        CAN_COMMAND("Option.canCommand"),
        CAN_INTERACT("Option.canInteract"),
        REDSTONE("Option.Redstone"),
        CAN_PVP("Option.canPVP"),
        CAN_SHOW_FIREWORK("Option.canshowFirework"),
        WEATHER("Option.Weather"),
        TIME_LOCK("Option.TimeLock");

        private final String path;

        WorldOption(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }
    }

    public UserWorldManager(World w) {
        this.w = w;
        userFile = new File("plugins/PRSUSERSETTING/" + w.getName() + ".yml");
        userConfig = YamlConfiguration.loadConfiguration(userFile);
    }

    public void createConfigFile() {
        if (!userFile.exists()) {
            try {
                YamlConfiguration userConfig = YamlConfiguration.loadConfiguration(userFile);
                userConfig.save(userFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public FileConfiguration getConfig() {
        return userConfig;
    }

    public File getConfigFile() {
        return userFile;
    }

    public void setDefaults() {
        getConfig().set("MCEnhanced.Info.IsInfected", false);
        for (WorldOption option : WorldOption.values()) {
            getConfig().set(option.getPath(), false);
        }
        saveConfig();
    }

    private void saveConfig() {
        try {
            getConfig().save(userFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void save() {
        try {
            getConfig().save(userFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean getOption(WorldOption option) {
        //* THIS WAY IS TERRIBLE
        //  IDK WHAT CAN I DO
        // *//
        return getConfig().getBoolean(option.getPath(), false);
    }

    public void setOption(WorldOption option, boolean value) {
        getConfig().set(option.getPath(), value);
        saveConfig();
    }
    public void toggleOption(WorldOption option) {
        boolean value = getOption(option);
        getConfig().set(option.getPath(), !value);
        saveConfig();
    }
    public boolean setGameMode(GameMode gameMode){
        getConfig().set("Option.Gamemode", gameMode);
        saveConfig();
        return true;
    }
    public GameMode getGameMode(){
        return (GameMode) getConfig().get("Option.Gamemode");
    }
    public boolean setWorldName(String name){
        getConfig().set("Option.Name", name);
        saveConfig();
        return true;
    }
    public String getWorldName(){
        return getConfig().getString("Option.Name");
    }

    public boolean setSpawnLocation(Location location){
        getConfig().set("Option.TeleportLocation", location);
        saveConfig();
        return true;
    }
    public Location getSpawnLocation(){
        return (Location) getConfig().get("Option.TeleportLocation");
    }
}