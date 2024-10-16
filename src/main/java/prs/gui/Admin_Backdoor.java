package prs.gui;

import org.bukkit.*;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import prs.Data.UserWorldManager;
import prs.privateworld.PrivateWorld;
import prs.world.WorldManager;

import java.util.Arrays;

public class Admin_Backdoor implements Listener {
    private final Inventory inv;
    private PrivateWorld wm = PrivateWorld.getPlugin(PrivateWorld.class);
    private Player p;
    UserWorldManager uwm;
    WorldManager wrm = new WorldManager();
    World world;
    WorldList wl;

    public Admin_Backdoor(Player p, World w, WorldList wl ) {//3rd: 불필요한 클래스 생성 방지
        // Create a new inventory, with no owner (as this isn't a real inventory), a size of nine, called example
        inv = Bukkit.createInventory(null, 54, "WorldList");
        this.p = p;
        world = w;
        uwm = new UserWorldManager(p.getWorld());
        // Put the items into the inventory
        this.wl = wl;
        initializeItems();
    }

    // You can call this whenever you want to put the items in
    public void initializeItems() {
        inv.setItem(0, createGuiItem(Material.BARRIER, "Delete", ""));
        inv.setItem(1, createGuiItem(Material.BONE, "World setting", ""));
    }

    // Nice little method to create a gui item with a custom name, and description
    protected ItemStack createGuiItem(final Material material, final String name, final String... lore) {
        final ItemStack item = new ItemStack(material, 1);
        final ItemMeta meta = item.getItemMeta();
        // Set the name of the item
        meta.setDisplayName(name);

        // Set the lore of the item
        meta.setLore(Arrays.asList(lore));

        item.setItemMeta(meta);

        return item;
    }

    // You can open the inventory with this
    public void openInventory(final HumanEntity ent) {
        ent.openInventory(inv);
    }

    // Check for clicks on items
    @EventHandler
    public void onInventoryClick(final InventoryClickEvent e) {
        if (!e.getInventory().equals(inv)) return;

        e.setCancelled(true);

        final ItemStack clickedItem = e.getCurrentItem();

        // verify current item is not null
        if (clickedItem == null || clickedItem.getType().isAir()) return;

        final Player p = (Player) e.getWhoClicked();

        if(e.getRawSlot() == 0){
            wrm.Deleteworld(world);
        }
        else if(e.getRawSlot() == 1){
            option option = new option(p, world);
            Bukkit.getPluginManager().registerEvents(option, wm);
            option.openInventory(p);
        }

    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e){
        InventoryDragEvent.getHandlerList().unregister(this);
        InventoryClickEvent.getHandlerList().unregister(this);
        InventoryCloseEvent.getHandlerList().unregister(this);
    }

    // Cancel dragging in our inventory
    @EventHandler
    public void onInventoryClick(final InventoryDragEvent e) {
        if (e.getInventory().equals(inv)) {
            e.setCancelled(true);
        }
    }
}
