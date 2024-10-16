package prs.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
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

import java.util.Arrays;

public class GUI_GAMEMODE implements Listener {
    private final Inventory inv;
    private PrivateWorld wm = PrivateWorld.getPlugin(PrivateWorld.class);
    private Player p;
    UserWorldManager uwm;

    public GUI_GAMEMODE(Player p) {
        // Create a new inventory, with no owner (as this isn't a real inventory), a size of nine, called example
        inv = Bukkit.createInventory(null, 9, "Option -> gamemode");
        this.p = p;
        uwm = new UserWorldManager(p.getWorld());
        // Put the items into the inventory
        initializeItems();
    }

    // You can call this whenever you want to put the items in
    public void initializeItems() {
        inv.setItem(0, createGuiItem(Material.GRASS_BLOCK, "크리에이티브"));
        inv.setItem(2, createGuiItem(Material.GRASS_BLOCK, "서바이벌"));
        inv.setItem(4, createGuiItem(Material.GRASS_BLOCK, "어드벤처"));
        inv.setItem(6, createGuiItem(Material.GRASS_BLOCK, "관전자"));
        inv.setItem(8, createGuiItem(Material.BARRIER, "취소"));
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
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e){
        InventoryDragEvent.getHandlerList().unregister(this);
        InventoryClickEvent.getHandlerList().unregister(this);
        InventoryCloseEvent.getHandlerList().unregister(this);
    }

    // Check for clicks on items
    @EventHandler
    public void onInventoryClick(final InventoryClickEvent e) {
        boolean isChanged = false;
        if (!e.getInventory().equals(inv)) return;

        e.setCancelled(true);

        final ItemStack clickedItem = e.getCurrentItem();

        // verify current item is not null
        if (clickedItem == null || clickedItem.getType().isAir()) return;

        final Player p = (Player) e.getWhoClicked();
        if(e.getRawSlot() == 0) isChanged = uwm.SetGamemode(GameMode.CREATIVE);
        if(e.getRawSlot() == 2) isChanged = uwm.SetGamemode(GameMode.SURVIVAL);
        if(e.getRawSlot() == 4) isChanged = uwm.SetGamemode(GameMode.ADVENTURE);
        if(e.getRawSlot() == 6) isChanged = uwm.SetGamemode(GameMode.SPECTATOR);
        if(e.getRawSlot() == 8) e.getWhoClicked().closeInventory();
        if(isChanged){
            e.getWhoClicked().sendMessage(ChatColor.GREEN + "변경되었습니다");
            e.getWhoClicked().closeInventory();
        }
    }

    // Cancel dragging in our inventory
    @EventHandler
    public void onInventoryClick(final InventoryDragEvent e) {
        if (e.getInventory().equals(inv)) {
            e.setCancelled(true);
        }
    }
}
