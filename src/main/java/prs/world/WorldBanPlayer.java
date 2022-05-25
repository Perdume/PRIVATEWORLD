package prs.world;

import org.bukkit.*;
import org.bukkit.entity.Player;
import prs.Data.UserWorldManager;
import prs.privateworld.PrivateWorld;

import java.util.ArrayList;
import java.util.List;

public class WorldBanPlayer {
    WorldManager wrm = new WorldManager();
    private PrivateWorld wm = PrivateWorld.getPlugin(PrivateWorld.class);
    public void BanPlayer(Player player, OfflinePlayer Criminal){
        if (IsPlayerBanned(Criminal, player.getWorld()).equals(false)) {
            if (Criminal.isOnline()) {
                Player Criminal2 = (Player) Criminal;
                if (wrm.wcon(Criminal2.getWorld()) == player) {
                    Location loc = (Location) wm.worldManager.getConfig().get("Lobby");
                    Criminal2.teleport(loc);
                    Criminal2.sendMessage(ChatColor.RED + "해당 맵에 들어갈 수 없습니다");
                }
            }
            try {
                UserWorldManager uwm = new UserWorldManager(player.getWorld());
                List<String> temp = (List<String>) uwm.getWorldFile().getList("Banned");
                temp.add(Criminal.getUniqueId().toString());
                player.sendMessage(ChatColor.GREEN + "성공적으로 밴했습니다");
                uwm.getWorldFile().set("Banned", temp);
                uwm.saveUserFile();
            }
            catch(Exception e) {
                UserWorldManager uwm = new UserWorldManager(player.getWorld());
                List<String> temp2 = new ArrayList<>();
                temp2.add(Criminal.getUniqueId().toString());
                uwm.getWorldFile().set("Banned", temp2);
                player.sendMessage(ChatColor.GREEN + "성공적으로 밴했습니다");
                uwm.saveUserFile();
            }
        }
        else{
            player.sendMessage(ChatColor.RED + "이미 밴되어있습니다");
        }
    }
    public void UnbanPlayer(Player player, OfflinePlayer Criminal){
        if (IsPlayerBanned(Criminal, player.getWorld()).equals(true)){
            UserWorldManager uwm = new UserWorldManager(player.getWorld());
            List<String> temp = (List<String>) uwm.getWorldFile().getList("Banned");
            temp.remove(Criminal.getUniqueId().toString());
            player.sendMessage(ChatColor.GREEN + "성공적으로 언밴했습니다");
            uwm.getWorldFile().set("Banned", temp);
            uwm.saveUserFile();
        }
        else{
            player.sendMessage(ChatColor.RED + "이미 언밴되어있습니다");
        }
    }
    public Boolean IsPlayerBanned(OfflinePlayer p, World w){
        try {
            UserWorldManager uwm = new UserWorldManager(w);
            List<String> temp = (List<String>) uwm.getWorldFile().getList("Banned");
            if (temp.contains(p.getUniqueId().toString())) {
                return true;
            } else {
                return false;
            }
        }
        catch(Exception e) {
            return false;
        }
    }
}
