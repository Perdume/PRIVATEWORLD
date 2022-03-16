package prs.WorldGuard;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import prs.privateworld.PrivateWorld;

public class Lobby implements Listener {
    private PrivateWorld wm = PrivateWorld.getPlugin(PrivateWorld.class);

    @EventHandler
    public void onmove(PlayerMoveEvent e){
        if (e.getPlayer().getLocation().getY() < 0){
            Location loc = (Location) wm.worldManager.getConfig().get("Lobby");
            if (e.getPlayer().getWorld() == loc.getWorld()){
                e.getPlayer().teleport(loc);
            }
        }
    }
    @EventHandler
    public void ondamage(EntityDamageEvent e){
        if (e.getEntity().getType() == EntityType.PLAYER) {
            Player p = (Player) e.getEntity();
            Location loc = (Location) wm.worldManager.getConfig().get("Lobby");
            if (p.getWorld() == loc.getWorld()) {
                e.setCancelled(true);
            }
        }
    }
    @EventHandler
    public void onhunger(FoodLevelChangeEvent e){
        Player p = (Player) e.getEntity();
        Location loc = (Location) wm.worldManager.getConfig().get("Lobby");
        if (p.getWorld() == loc.getWorld()){
            e.setCancelled(true);
            p.setFoodLevel(20);
        }
    }
    @EventHandler
    public void onbreak(BlockBreakEvent e){
        Location loc = (Location) wm.worldManager.getConfig().get("Lobby");
        if (e.getPlayer().getWorld() == loc.getWorld()){
            if (!e.getPlayer().isOp()) {
                e.setCancelled(true);
            }
        }
    }
    @EventHandler
    public void onplace(BlockPlaceEvent e){
        Location loc = (Location) wm.worldManager.getConfig().get("Lobby");
        if (e.getPlayer().getWorld() == loc.getWorld()){
            if (!e.getPlayer().isOp()) {
                e.setCancelled(true);
            }
        }
    }
}
