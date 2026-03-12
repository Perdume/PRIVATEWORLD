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
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import prs.data.UserWorldManager;
import prs.main.Chatting;
import prs.privateworld.PrivateWorld;
import prs.world.WorldBanPlayer;
import prs.world.WorldManager;
import redempt.redlib.inventorygui.InventoryGUI;
import redempt.redlib.inventorygui.ItemButton;
import redempt.redlib.itemutils.ItemBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChoosePlayerWorld implements Listener {
    private final Inventory inv;
    private PrivateWorld plugin = PrivateWorld.getPlugin(PrivateWorld.class);
    private Player p;
    UserWorldManager worldSettings;
    WorldManager worldMgr = new WorldManager();
    List<World> WorldTPList = new ArrayList<>();
    OfflinePlayer SelectPlayer;

    public ChoosePlayerWorld(Player p, OfflinePlayer SelectPlayer) {
        // Create a new inventory, with no owner (as this isn't a real inventory), a size of nine, called example
        inv = Bukkit.createInventory(null, 9, "WorldList");
        this.p = p;
        this.SelectPlayer = SelectPlayer;
        worldSettings = new UserWorldManager(p.getWorld());
        // Put the items into the inventory
        initializeItems();
    }

    // You can call this whenever you want to put the items in
    public void initializeItems() {
        List<World> worlds = plugin.worldManager.getWorldList();
        int i = 0;
        for(World w: worlds){
            if(worldMgr.getWorldOwner(w) != SelectPlayer) continue;
            WorldTPList.add(w);
            String name = (String) worldSettings.getConfig().get("Option.Name");
            if(name == null) name = SelectPlayer.getName() + "의 월드";
            inv.setItem(i, createGuiItem(Material.GRASS_BLOCK, name, ""));
        }
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
        if(e.getRawSlot() >= 0 && WorldTPList.get(e.getRawSlot()) != null){
            PlayerTeleport(WorldTPList.get(e.getRawSlot()));
        }
    }
    public void PlayerTeleport(World w){
        UserWorldManager worldSettings = new UserWorldManager(w);
        WorldBanPlayer banManager = new WorldBanPlayer();
        if (worldSettings.getOption(UserWorldManager.WorldOption.PRIVATE) && SelectPlayer != p) {
            p.sendMessage(ChatColor.RED + "해당 월드는 비공개상태입니다");
            return;
        }
        if (banManager.isPlayerBanned(p, w)) {
            p.sendMessage(ChatColor.RED + "해당 월드로 들어갈 수 없습니다");
            return;
        }
        GameMode gameMode = (GameMode) worldSettings.getConfig().get("Option.Gamemode");
        if(gameMode == null) gameMode = GameMode.ADVENTURE;
        Location TPloc = worldSettings.getSpawnLocation();
        if(TPloc == null) TPloc = new Location(w, 0, 64, 0);
        p.teleport(TPloc);
        p.setGameMode(gameMode);
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


