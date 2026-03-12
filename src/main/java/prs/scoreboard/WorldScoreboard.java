package prs.scoreboard;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;
import prs.world.WorldManager;

import java.util.ArrayList;

public class WorldScoreboard implements Listener {
    WorldManager worldMgr = new WorldManager();
    public void createScoreboard(Player player) {
        Helper helper = Helper.createScore(player);
        helper.setTitle("&aPRIVATE WORLD");
    }
    @EventHandler
    public void join(PlayerJoinEvent e){
        createScoreboard(e.getPlayer());
    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if(Helper.hasScore(player)) {
            Helper.removeScore(player);
        }
    }
    public void updateScoreboard(Player player) {
        if(Helper.hasScore(player)) {
            Helper helper = Helper.getByPlayer(player);
            ArrayList<String> list = new ArrayList<>();
            list.add("&7&m--------------------------------");
            list.add("&aPlayer: &f" + player.getName());
            if (worldMgr.getWorldOwner(player.getWorld()) != null){
                list.add("맵 주인:" + worldMgr.getWorldOwner(player.getWorld()).getName());
            }
            list.add("&7&m--------------------------------");
            list.add("&6프라이빗월드: /privateworld");
            helper.setSlotsFromList(list);
        }
    }
}
