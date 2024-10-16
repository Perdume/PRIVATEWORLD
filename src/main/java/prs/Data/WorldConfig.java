package prs.Data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import prs.privateworld.PrivateWorld;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

public class WorldConfig {
    private PrivateWorld plugin;
    private FileConfiguration dataconfig = null;
    private File configfile = null;
    List<World> worldlist = new LinkedList<>();
    List<String> DefaultWorlds = new ArrayList<>();
    Location lobby = null;

    public WorldConfig(PrivateWorld plugin){
        this.plugin = plugin;
        saveDefaultConfig();
    }
    public void reloadConfig(){
        if (this.configfile == null) {
            this.configfile = new File(this.plugin.getDataFolder(), "Worldyml");
        }
        this.dataconfig = YamlConfiguration.loadConfiguration(this.configfile);

        InputStream defaultStream = this.plugin.getResource("world.yml");
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
    public void saveconfig(){
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
            this.configfile = new File(this.plugin.getDataFolder(), "world.yml");
        }
        if (!this.configfile.exists()){
            this.plugin.saveResource("world.yml", false);
        }
    }
    public void init(){
        getWorldList();
        getLobby();
    }
    //PlayerWorlds
    public List<World> getWorldList(){
        if(!worldlist.isEmpty()) return worldlist;
        List<String> Templist = (List<String>) getConfig().getList("Locs.PlayerWorlds");
        for(String s: Templist){
            worldlist.add(Bukkit.getWorld(s));
        }
        return worldlist;
    }
    public boolean Removeworld(World w){
        if(!worldlist.contains(w)) return false;
        worldlist.remove(w);
        return true;
    }
    public boolean Addworld(World w){
        if(worldlist.contains(w)) return false;
        worldlist.add(w);
        return true;
    }
    public boolean save(){
        List<String> WorldNameList = new ArrayList<>();
        for(World w: worldlist){
            WorldNameList.add(w.getName());
        }
        getConfig().set("Locs.PlayerWorlds", WorldNameList);
        getConfig().set("Locs.DefaultWorlds", DefaultWorlds);
        getConfig().set("Locs.Lobby", lobby);
        saveconfig();
        return true;
    }

    //Default Worlds
    public List<String> getDefaultWorldList(){
        if(DefaultWorlds.isEmpty()) return DefaultWorlds;
        DefaultWorlds = (List<String>) getConfig().getList("Locs.DefaultWorlds");
        return DefaultWorlds;
    }
    public boolean RemoveDefaultworld(String worldname){
        if(!DefaultWorlds.contains(worldname)) return false;
        DefaultWorlds.remove(worldname);
        return true;
    }
    public boolean AddDefaultworld(String worldname){
        if(DefaultWorlds.contains(worldname)) return false;
        DefaultWorlds.add(worldname);
        return true;
    }
    public Location getLobby(){
        if(lobby == null){
            lobby = (Location) getConfig().get("Locs.Lobby");
        }
        return lobby;
    }
    public Boolean isLobbySet(){
        return getLobby()!=null;
    }
    public boolean setLobby(Location loc){
        lobby=loc;
        return true;
    }
}
