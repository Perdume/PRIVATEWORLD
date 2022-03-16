package prs.Main;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;

public class PreventSteal implements Listener {
    Player player;
    ItemStack Item;
    public PreventSteal (Player p){
        this.player = p;
    }
    @EventHandler
    public void InvOpen(InventoryOpenEvent event) {
        this.Item = player.getInventory().getItemInOffHand();
    }
    @EventHandler
    public void InvClose(InventoryCloseEvent event) {
        player.getInventory().setItemInOffHand(this.Item);
    }
}
