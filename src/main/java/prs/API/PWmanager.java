package prs.API;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import prs.Data.ConfigManager;
import prs.Data.UserWorldManager;
import prs.privateworld.PrivateWorld;
import prs.world.WorldManager;

import java.util.List;

public final class PWmanager {
    public PWmanager() {}
    WorldManager wrm = new WorldManager();
    private PrivateWorld wm = PrivateWorld.getPlugin(PrivateWorld.class);

    public int GetPlayerWorldCount(OfflinePlayer p){
        int num = wrm.GetNum(p);
        return num;
    }
    public boolean PlayerHasWorld(OfflinePlayer p){
        int num = wrm.GetNum(p);
        if (num > 0){
            return true;
        }
        else{
            return false;
        }
    }
    public boolean PlayerInWorld(Player p){
        List<World> world = wrm.GetPrivateWorlds();
        for(World w: world){
            if (p.getWorld().getName() == w.getName()){
                return true;
            }
        }
        return false;
    }
    public int HowManyPlayers(World w){
        int i = 0;
        for(Player p: Bukkit.getOnlinePlayers()){
            if (p.getWorld() == w){
                i++;
            }
        }
        return i;
    }
    public OfflinePlayer GetWorldOwner(Player p){
        try {
            OfflinePlayer pr = wrm.wcon(p.getWorld());
            return pr;
        }
        catch (Exception e){
            return null;
        }
    }
    public WorldManager getsWorldManager() {
        return wrm;
    }
    public UserWorldManager getUserWorldManager(World w) {
        return new UserWorldManager(w);
    }
    public prs.Data.WorldManager getWorldFile(){
        return wm.worldManager;
    }
    public ConfigManager getConfigFile(){
        return wm.ConfigManager;
    }
}
