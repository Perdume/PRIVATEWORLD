package prs.world;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import prs.Data.UserWorldManager;
import prs.privateworld.PrivateWorld;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class WorldManager implements Listener {
    private PrivateWorld wm = PrivateWorld.getPlugin(PrivateWorld.class);
    public HashMap<Player,Integer> map1 = new HashMap<>();
    public void Createworld(Player p){
        int a = GetNum(p);
        map1.put(p, a);
        WorldCreator wc2 = new WorldCreator(p.getUniqueId().toString() + "--" + a);
        wc2.environment(World.Environment.NORMAL);
        wc2.type(WorldType.NORMAL);
        wc2.generateStructures(false);
        wc2.generator("VoidGenerator");
        wc2.createWorld();
        List<String> temp = (List<String>) wm.worldManager.getConfig().getList("PlayerWorlds");

        if (temp == null || wm.worldManager.getConfig().getList("PlayerWorlds") == null) {
            ArrayList<String> temp1 = new ArrayList<>(Arrays.asList(p.getUniqueId().toString() + "--" + a));
            temp = temp1;
            wm.worldManager.getConfig().set("PlayerWorlds", temp);
        }
        else {
            boolean contains = temp.contains(p.getWorld().getName());
            if (contains == false) {
                temp.add(p.getUniqueId().toString() + "--" + a);
                wm.worldManager.getConfig().set("PlayerWorlds", temp);
            }
            wm.worldManager.saveconfig();
        }

        try {
            worldset(Bukkit.getWorld(p.getUniqueId().toString() + "--" + a));
        }
        catch(Exception e) {
            long time = System.currentTimeMillis();
            while (System.currentTimeMillis() - time < 100){}
            worldset(Bukkit.getWorld(p.getUniqueId().toString() + "--" +a));
        }
    }
    public void worldset(World w) {
        UserWorldManager wm = new UserWorldManager(w);
        wm.CreateWorld();
    }
    public int PlayerWorldCount(Player p){
        int i = 0;
        List<String> temp = (List<String>) wm.worldManager.getConfig().getList("DefaultWorlds");
        if (temp != null){
            try {
                for (String s1 : temp) {
                    if (wcon(Bukkit.getWorld(s1)).getUniqueId() == p.getUniqueId()) {
                        i++;
                    }
                }
                System.out.println(i);
                return i;
            }
            catch(Exception e){
                return 0;
            }

        }
        else{
            return 0;
        }
    }
    public void Deleteworld(String s) {
        System.out.println(s);
        List<String> temp = (List<String>) wm.worldManager.getConfig().getList("PlayerWorlds");
        boolean contains = temp.contains(s);
        if (contains == true) {
            temp.remove(s);
            wm.worldManager.getConfig().set("DefaultWorlds", temp);

        }
        for (Player p: Bukkit.getOnlinePlayers()){
            if (p.getWorld().getName() == s){
                p.teleport((Location) wm.worldManager.getConfig().get("Lobby"));
            }
        }
        File UserFile = new File("plugins/PRSUSERSETTING/" + s + ".yml");
        UserFile.delete();
        Path releaseFolder = Paths.get(s);
        Bukkit.unloadWorld(s, false);
        WorldManage.deleteFilesRecursively(releaseFolder.toFile());
        wm.worldManager.saveconfig();
    }
    public Integer WorldCongigNumCheck(String s){
        int i = 0;
        List<String> temp = (List<String>) wm.worldManager.getConfig().getList("PlayerWorlds");
        if (temp != null){
            for (String s1: temp){
                if (s1 == s){
                    i++;
                }
            }
        }
        return i;
    }
    public Integer GetNum(OfflinePlayer p){
        List<String> temp = (List<String>) wm.worldManager.getConfig().getList("PlayerWorlds");
        int i = 1;
        while (true){
            if (!temp.contains(p.getUniqueId() + "--" + i)){
                return i;
            }
            i++;
        }
    }
    public void WorldCheck(){
        List<String> temp = (List<String>) wm.worldManager.getConfig().getList("PlayerWorlds");
        if (temp != null){
            for (Iterator<String> itr = temp.iterator(); itr.hasNext();){
                 String s2 = itr.next();
                if (s2 != null) {
                    try {
                        World w = Bukkit.getWorld(s2);
                        if (WorldCongigNumCheck(s2) >= 1) {
                            itr.remove();
                        }
                    }
                    catch (Exception e){
                        itr.remove();
                    }
                }
            }
        }
        for (World w: Bukkit.getWorlds()){
            if (w.getName().contains("--")) {
                if (temp != null) {
                    if (!temp.contains(w.getName())) {
                        temp.add(w.getName());
                    }
                }
                else{
                    ArrayList<String> temp1 = new ArrayList<>(Arrays.asList(w.getName()));
                    temp = temp1;
                }
            }
        }
        wm.worldManager.getConfig().set("PlayerWorlds", temp);
        wm.worldManager.saveconfig();
    }
    public OfflinePlayer wcon(World w){
        try {
            if (w.getName().contains("--")) {
                String[] arr = w.getName().split("--");
                try {
                    OfflinePlayer p = Bukkit.getServer().getOfflinePlayer(UUID.fromString(arr[0]));
                    return p;
                } catch (Exception e) {
                    return null;
                }
            } else {
                return null;
            }
        }
        catch (Exception e){
            return null;
        }
    }
    public Boolean isPlayerhasWorld(OfflinePlayer p){
        for(World w: Bukkit.getWorlds()){
            if (wcon(w) == p){
                return true;
            }
        }
        return false;
    }
    public List<World> GetPrivateWorlds(){
        List<String> temp = (List<String>) wm.worldManager.getConfig().getList("PlayerWorlds");
        List<World> temp2 = new ArrayList<>();
        for(String s: temp){
            temp2.add(Bukkit.getWorld(s));
        }
        return temp2;
    }
    public Integer PlayerHasWorld(OfflinePlayer p){
        int i = 0;
        for(World w: Bukkit.getWorlds()){
            if (wcon(w) == p){
                i++;
            }
        }
        return i;
    }
}
