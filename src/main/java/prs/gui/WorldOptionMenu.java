package prs.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
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
import prs.data.UserWorldManager;
import prs.main.Chatting;
import prs.privateworld.PrivateWorld;

import java.util.Arrays;

public class WorldOptionMenu implements Listener {
    private final Inventory inv;
    private PrivateWorld plugin = PrivateWorld.getPlugin(PrivateWorld.class);
    private Player p;
    UserWorldManager worldSettings;
    World world;

    public WorldOptionMenu(Player p, World world) {
        inv = Bukkit.createInventory(null, 27, "Option");
        this.p = p;
        this.world = (world == null) ? p.getWorld() : world;
        worldSettings = new UserWorldManager(this.world);
        initializeItems();
    }

    public void initializeItems() {
        inv.setItem(0, createGuiItem(Material.BARRIER, "월드비공개", worldSettings.getOption(UserWorldManager.WorldOption.PRIVATE)));
        inv.setItem(1, createGuiItem(Material.STICK, "아이템 드롭", worldSettings.getOption(UserWorldManager.WorldOption.CAN_DROP)));
        inv.setItem(2, createGuiItem(Material.NETHERITE_PICKAXE, "블럭 파괴", worldSettings.getOption(UserWorldManager.WorldOption.CAN_BREAK)));
        inv.setItem(3, createGuiItem(Material.BEDROCK, "블럭 설치", worldSettings.getOption(UserWorldManager.WorldOption.CAN_PLACE)));
        inv.setItem(4, createGuiItem(Material.BOW, "활쏘기", worldSettings.getOption(UserWorldManager.WorldOption.CAN_SHOOT)));
        inv.setItem(5, createGuiItem(Material.COMMAND_BLOCK, "커맨드", worldSettings.getOption(UserWorldManager.WorldOption.CAN_COMMAND)));
        inv.setItem(6, createGuiItem(Material.DISPENSER, "블럭 상호작용", worldSettings.getOption(UserWorldManager.WorldOption.CAN_INTERACT)));
        inv.setItem(7, createGuiItem(Material.REDSTONE, "레드스톤 작동", worldSettings.getOption(UserWorldManager.WorldOption.REDSTONE)));
        inv.setItem(8, createGuiItem(Material.DIAMOND_SWORD, "PVP", worldSettings.getOption(UserWorldManager.WorldOption.CAN_PVP)));
        inv.setItem(9, createGuiItem(Material.FIREWORK_ROCKET, "폭죽 터뜨리기", worldSettings.getOption(UserWorldManager.WorldOption.CAN_SHOW_FIREWORK)));
        inv.setItem(10, createGuiItem(Material.WATER_BUCKET, "날씨 허용", worldSettings.getOption(UserWorldManager.WorldOption.WEATHER)));
        inv.setItem(11, createGuiItem(Material.CLOCK, "시간 고정(낮)", worldSettings.getOption(UserWorldManager.WorldOption.TIME_LOCK)));
        inv.setItem(18, createGuiItem(Material.NAME_TAG, "월드 이름 변경", null));
        inv.setItem(19, createGuiItem(Material.COMPASS, "텔레포트 위치 설정", null));
        inv.setItem(20, createGuiItem(Material.GRASS_BLOCK, "게임모드 설정", null));
    }

    protected ItemStack createGuiItem(final Material material, final String name, Boolean isEnabled, final String... lore) {
        final ItemStack item = new ItemStack(material, 1);
        final ItemMeta meta = item.getItemMeta();
        String displayName = (isEnabled == null) ? name : ((isEnabled ? ChatColor.GREEN : ChatColor.RED) + name);
        meta.setDisplayName(displayName);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    public void openInventory(final HumanEntity ent) {
        ent.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(final InventoryClickEvent e) {
        if (!e.getInventory().equals(inv)) return;
        e.setCancelled(true);
        final ItemStack clickedItem = e.getCurrentItem();
        if (clickedItem == null || clickedItem.getType().isAir()) return;

        final Player p = (Player) e.getWhoClicked();
        boolean toggled = false;
        if (e.getRawSlot() == 0)  { worldSettings.toggleOption(UserWorldManager.WorldOption.PRIVATE);          toggled = true; }
        if (e.getRawSlot() == 1)  { worldSettings.toggleOption(UserWorldManager.WorldOption.CAN_DROP);         toggled = true; }
        if (e.getRawSlot() == 2)  { worldSettings.toggleOption(UserWorldManager.WorldOption.CAN_BREAK);        toggled = true; }
        if (e.getRawSlot() == 3)  { worldSettings.toggleOption(UserWorldManager.WorldOption.CAN_PLACE);        toggled = true; }
        if (e.getRawSlot() == 4)  { worldSettings.toggleOption(UserWorldManager.WorldOption.CAN_SHOOT);        toggled = true; }
        if (e.getRawSlot() == 5)  { worldSettings.toggleOption(UserWorldManager.WorldOption.CAN_COMMAND);      toggled = true; }
        if (e.getRawSlot() == 6)  { worldSettings.toggleOption(UserWorldManager.WorldOption.CAN_INTERACT);     toggled = true; }
        if (e.getRawSlot() == 7)  { worldSettings.toggleOption(UserWorldManager.WorldOption.REDSTONE);         toggled = true; }
        if (e.getRawSlot() == 8)  { worldSettings.toggleOption(UserWorldManager.WorldOption.CAN_PVP);          toggled = true; }
        if (e.getRawSlot() == 9)  { worldSettings.toggleOption(UserWorldManager.WorldOption.CAN_SHOW_FIREWORK); toggled = true; }
        if (e.getRawSlot() == 10) { worldSettings.toggleOption(UserWorldManager.WorldOption.WEATHER);          toggled = true; }
        if (e.getRawSlot() == 11) { worldSettings.toggleOption(UserWorldManager.WorldOption.TIME_LOCK);        toggled = true; }
        if (toggled) {
            e.getWhoClicked().sendMessage(ChatColor.GREEN + "설정되었습니다");
            e.getWhoClicked().closeInventory();
        }
        if (e.getRawSlot() == 18) {
            e.getWhoClicked().sendMessage(ChatColor.GREEN + "채팅으로 자신의 월드 이름을 적어주세요, Quit 메세지로 나올 수 있습니다(이 채팅은 다른 사람들에게 안보여집니다)");
            Chatting cht = new Chatting();
            cht.startChatInput((Player) e.getWhoClicked(), "Name", e.getWhoClicked().getWorld());
            e.getWhoClicked().closeInventory();
        }
        if (e.getRawSlot() == 19) {
            e.getWhoClicked().sendMessage(ChatColor.GREEN + "스폰 위치를 본인 위치로 설정했습니다");
            worldSettings.setSpawnLocation(e.getWhoClicked().getLocation());
            e.getWhoClicked().closeInventory();
        }
        if (e.getRawSlot() == 20) {
            GUI_GAMEMODE guiGamemode = new GUI_GAMEMODE(p);
            Bukkit.getPluginManager().registerEvents(guiGamemode, plugin);
            guiGamemode.openInventory(e.getWhoClicked());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        InventoryDragEvent.getHandlerList().unregister(this);
        InventoryClickEvent.getHandlerList().unregister(this);
        InventoryCloseEvent.getHandlerList().unregister(this);
    }

    @EventHandler
    public void onInventoryClick(final InventoryDragEvent e) {
        if (e.getInventory().equals(inv)) {
            e.setCancelled(true);
        }
    }
}

