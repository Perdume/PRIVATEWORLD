package prs.Data;

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
        CAN_SHOW_FIREWORK("Option.canshowFirework");

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

    public void createWorld() {
        if (!userFile.exists()) {
            try {
                YamlConfiguration userConfig = YamlConfiguration.loadConfiguration(userFile);
                userConfig.save(userFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public FileConfiguration getWorldFile() {
        return userConfig;
    }

    public File getWorldFiles() {
        return userFile;
    }

    public void setDefaultUserFile() {
        getWorldFile().set("MCEnhanced.Info.IsInfected", false);
        for (WorldOption option : WorldOption.values()) {
            getWorldFile().set(option.getPath(), false);
        }
        saveUserFile();
    }

    private void saveUserFile() {
        try {
            getWorldFile().save(userFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void save() {
        try {
            getWorldFile().save(userFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean getOption(WorldOption option) {
        //* THIS WAY IS TERRIBLE
        //  IDK WHAT CAN I DO
        // *//
        return getWorldFile().getBoolean(option.getPath(), false);
    }

    public void setOption(WorldOption option, boolean value) {
        getWorldFile().set(option.getPath(), value);
        saveUserFile();
    }
    public void ChangeOption(WorldOption option) {
        boolean value = getOption(option);
        getWorldFile().set(option.getPath(), !value);
        saveUserFile();
    }
    public boolean SetGamemode(GameMode gameMode){
        getWorldFile().set("Option.Gamemode", gameMode);
        saveUserFile();
        return true;
    }
    public void GetGamemode(GameMode gameMode){
        getWorldFile().set("Option.Gamemode", gameMode);
        saveUserFile();
    }
    public boolean SetWorldName(String name){
        getWorldFile().set("Option.Name", name);
        saveUserFile();
        return true;
    }
    public String GetWorldName(){
        return getWorldFile().getString("Option.Name");
    }

    public boolean SetTeleportLocation(Location location){
        getWorldFile().set("Option.TeleportLocation", location);
        saveUserFile();
        return true;
    }
    public Location GetTeleportLocation(Location location){
        return (Location) getWorldFile().get("Option.TeleportLocation");
    }
}