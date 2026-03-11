package prs.world;

import org.bukkit.*;
import org.bukkit.entity.Player;
import prs.data.UserWorldManager;
import prs.privateworld.PrivateWorld;

import java.util.ArrayList;
import java.util.List;

public class WorldBanPlayer {
    WorldManager worldMgr = new WorldManager();
    private PrivateWorld plugin = PrivateWorld.getPlugin(PrivateWorld.class);
    public void banPlayer(Player player, OfflinePlayer Criminal){
        if (isPlayerBanned(Criminal, player.getWorld()).equals(true)) {
            player.sendMessage(ChatColor.RED + "이미 밴되어있습니다");
            return;
        }
        if (Criminal.isOnline()) {
            Player Criminal2 = (Player) Criminal;
            if (worldMgr.getWorldOwner(Criminal2.getWorld()) == player) {
                Location loc = (Location) plugin.worldManager.getConfig().get("Lobby");
                Criminal2.teleport(loc);
                Criminal2.sendMessage(ChatColor.RED + "해당 맵에 들어갈 수 없습니다");
            }
        }
        UserWorldManager worldSettings = new UserWorldManager(player.getWorld());
        List<String> temp = (List<String>) worldSettings.getConfig().getList("Banned");
        if(temp == null) temp = new ArrayList<>();
        temp.add(Criminal.getUniqueId().toString());
        player.sendMessage(ChatColor.GREEN + "성공적으로 밴했습니다");
        worldSettings.getConfig().set("Banned", temp);
        worldSettings.save();
    }
    public void unbanPlayer(Player player, OfflinePlayer Criminal){
        if (!isPlayerBanned(Criminal, player.getWorld()).equals(true)){
            player.sendMessage(ChatColor.RED + "이미 언밴되어있습니다");
            return;
        }
        UserWorldManager worldSettings = new UserWorldManager(player.getWorld());
        List<String> temp = (List<String>) worldSettings.getConfig().getList("Banned");
        temp.remove(Criminal.getUniqueId().toString());
        player.sendMessage(ChatColor.GREEN + "성공적으로 언밴했습니다");
        worldSettings.getConfig().set("Banned", temp);
        worldSettings.save();
    }
    public Boolean isPlayerBanned(OfflinePlayer p, World w){
        UserWorldManager worldSettings = new UserWorldManager(w);
        List<String> temp = (List<String>) worldSettings.getConfig().getList("Banned");
        if(temp == null) return false;
        return temp.contains(p.getUniqueId().toString());
    }
}
