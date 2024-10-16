package prs.world;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import prs.Data.UserWorldManager;
import prs.privateworld.PrivateWorld;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class WorldManager implements Listener {
    private PrivateWorld wm = PrivateWorld.getPlugin(PrivateWorld.class);
    public HashMap<UUID,Integer> Worldnumber = new HashMap<>();
    public World Createworld(Player p){
        //CreateWorld
        int a = GetNum(p);
        Worldnumber.put(p.getUniqueId(), a);
        WorldCreator wc2 = new WorldCreator(p.getUniqueId().toString() + "--" + a);
        wc2.environment(World.Environment.NORMAL);
        wc2.type(WorldType.NORMAL);
        wc2.generateStructures(false);
        wc2.generator("VoidGen");
        World wn = wc2.createWorld();
        wm.worldManager.Addworld(wn);
        //SetConfig
        UserWorldManager wm = new UserWorldManager(wn);
        wm.createWorld();
        return wn;
    }
    public void CreatePlayerWorld(Player p){
        if (PlayerWorldCount(p) == 9) {
            p.sendMessage(ChatColor.RED + "9개 이상 만들 수 없습니다");
        }
        WorldCheck();
        World w = Createworld(p);
        wm.worldManager.Addworld(w);
        World checkingWorld = p.getWorld();
        Location loc = new Location(w, 0, 65, 0);
        Location loc2 = new Location(w, 0, 64, 0);
        loc2.getBlock().setType(Material.BEDROCK);
        new BukkitRunnable() {
            @Override
            public void run() {
                p.teleport(loc);
                if(p.getWorld() != checkingWorld) this.cancel();
            }
        }.runTaskTimer(wm, 20, 20);
    }
    public int PlayerWorldCount(Player p){
        int i = 0;
        List<World> temp = wm.worldManager.getWorldList();
        if(temp == null) return 0;
        for (World s1 : temp) {
            if (wcon(s1).getUniqueId() == p.getUniqueId()) i++;
        }
        return i;
    }
    public void Deleteworld(World w) {
        wm.worldManager.Removeworld(w);
        for (Player p: w.getPlayers()){
            p.teleport(wm.worldManager.getLobby());
        }
        File UserFile = new File("plugins/PRSUSERSETTING/" + w.getName() + ".yml");
        UserFile.delete();
        Path releaseFolder = Paths.get(w.getName());
        Bukkit.unloadWorld(w.getName(), false);
        WorldManage.deleteFilesRecursively(releaseFolder.toFile());
        wm.worldManager.saveconfig();
    }
    public Integer WorldCongigNumCheck(World w){
        int i = 0;
        List<World> temp = wm.worldManager.getWorldList();
        if (temp == null) return 0;
        for (World s1: temp){
            if (Objects.equals(s1, w)) i++;
        }
        return i;
    }
    public Integer GetNum(OfflinePlayer p){
        if(Worldnumber.get(p) != null) return Worldnumber.get(p);
        int i = 1;
        while (true){
            if (Bukkit.getWorld(p.getUniqueId() + "--" + i) == null){
                Worldnumber.put(p.getUniqueId(), i);
                return i;
            }
            i++;
        }
    }
    public void WorldCheck(){
        List<World> temp = wm.worldManager.getWorldList();
        if (temp == null) return;
        for (Iterator<World> itr = temp.iterator(); itr.hasNext();){
            World s2 = itr.next();
            if (s2 == null) continue;
            if (WorldCongigNumCheck(s2) >= 1) itr.remove();
        }
        for (World w: Bukkit.getWorlds()){
            if (!w.getName().contains("--")) continue;
            if (!temp.contains(w)) wm.worldManager.Addworld(w);
        }
    }
    public OfflinePlayer wcon(World w){
        if (!w.getName().contains("--")) return null;
        String[] arr = w.getName().split("--");
        OfflinePlayer p = Bukkit.getServer().getOfflinePlayer(UUID.fromString(arr[0]));
        return p;
    }
    public Boolean isPlayerhasWorld(OfflinePlayer p){
        for(World w: Bukkit.getWorlds()){
            if (wcon(w) == p) return true;
        }
        return false;
    }
}
