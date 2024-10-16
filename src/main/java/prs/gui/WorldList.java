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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import prs.Data.UserWorldManager;
import prs.Main.Chatting;
import prs.privateworld.PrivateWorld;
import prs.world.WorldManager;
import redempt.redlib.inventorygui.InventoryGUI;
import redempt.redlib.inventorygui.ItemButton;
import redempt.redlib.itemutils.ItemBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WorldList implements Listener {
    private final Inventory inv;
    private PrivateWorld wm = PrivateWorld.getPlugin(PrivateWorld.class);
    private Player p;
    UserWorldManager uwm;
    WorldManager wrm = new WorldManager();
    int page = 1;
    List<World> worldList = new ArrayList<>();

    public WorldList(Player p) {
        // Create a new inventory, with no owner (as this isn't a real inventory), a size of nine, called example
        inv = Bukkit.createInventory(null, 54, "WorldList");
        this.p = p;
        uwm = new UserWorldManager(p.getWorld());
        // Put the items into the inventory
        initializeItems();
    }

    // You can call this whenever you want to put the items in
    public void initializeItems() {
        List<World> worlds = wm.worldManager.getWorldList();
        int startIndex = (page - 1) * 45;
        int endIndex = Math.min(page * 45, worlds.size());
        for (int i = startIndex; i < endIndex; i++) {
            String worldName = worlds.get(i).getName();
            World world = worlds.get(i);
            worldList.add(world);
            if (world != null && wrm.wcon(world) != null) {
                inv.setItem(i - startIndex, createGuiItem(Material.GRASS_BLOCK, worldName,
                        ChatColor.GREEN + "Owner: " + wrm.wcon(world).getName()));
            }
        }
        if(page > 1) inv.setItem(45, createGuiItem(Material.ARROW, ChatColor.GREEN + "previous", ChatColor.GREEN + "Go previous Page"));
        inv.setItem(53, createGuiItem(Material.ARROW, ChatColor.GREEN + "next", ChatColor.GREEN + "Go next Page"));

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

        if(e.getRawSlot() == 45){
            page++;
            initializeItems();
        }
        if(e.getRawSlot() == 53){
            page--;
            initializeItems();
        }
        if(e.getRawSlot() >= 0 && worldList.get(e.getRawSlot()) != null){
            p.closeInventory();
            Admin_Backdoor cpw = new Admin_Backdoor(p, worldList.get(e.getRawSlot()), this);
            Bukkit.getPluginManager().registerEvents(cpw, wm);
            cpw.openInventory(p);
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

