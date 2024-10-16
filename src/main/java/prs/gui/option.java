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
import prs.Data.UserWorldManager;
import prs.Main.Chatting;
import prs.privateworld.PrivateWorld;

import java.util.Arrays;

public class option implements Listener {
    private final Inventory inv;
    private PrivateWorld wm = PrivateWorld.getPlugin(PrivateWorld.class);
    private Player p;
    UserWorldManager uwm;
    World world;

    public option(Player p, World world) {
        inv = Bukkit.createInventory(null, 18, "Option");
        this.p = p;
        this.world = (world == null)?p.getWorld(): world;
        uwm = new UserWorldManager(this.world);
        initializeItems();
    }

    // You can call this whenever you want to put the items in
    public void initializeItems() {
        inv.addItem(createGuiItem(Material.BARRIER, "월드비공개", uwm.getOption(UserWorldManager.WorldOption.PRIVATE)));
        inv.addItem(createGuiItem(Material.STICK, "아이템 드롭", uwm.getOption(UserWorldManager.WorldOption.CAN_DROP)));
        inv.addItem(createGuiItem(Material.NETHERITE_PICKAXE, "블럭 파괴", uwm.getOption(UserWorldManager.WorldOption.CAN_BREAK)));
        inv.addItem(createGuiItem(Material.BEDROCK, "블럭 설치", uwm.getOption(UserWorldManager.WorldOption.CAN_PLACE)));
        inv.addItem(createGuiItem(Material.BOW, "활쏘기", uwm.getOption(UserWorldManager.WorldOption.CAN_SHOOT)));
        inv.addItem(createGuiItem(Material.COMMAND_BLOCK, "커맨드", uwm.getOption(UserWorldManager.WorldOption.CAN_COMMAND)));
        inv.addItem(createGuiItem(Material.DISPENSER, "블럭 상호작용", uwm.getOption(UserWorldManager.WorldOption.CAN_INTERACT)));
        inv.addItem(createGuiItem(Material.REDSTONE, "레드스톤 작동", uwm.getOption(UserWorldManager.WorldOption.REDSTONE)));
        inv.addItem(createGuiItem(Material.DIAMOND_SWORD, "PVP", uwm.getOption(UserWorldManager.WorldOption.CAN_PVP)));
        inv.addItem(createGuiItem(Material.FIREWORK_ROCKET, "폭죽 터뜨리기", uwm.getOption(UserWorldManager.WorldOption.CAN_SHOW_FIREWORK)));
        inv.addItem(createGuiItem(Material.NAME_TAG, "월드 이름 변경", null));
        inv.addItem(createGuiItem(Material.COMPASS, "텔레포트 위치 설정", null));
        inv.addItem(createGuiItem(Material.GRASS_BLOCK, "게임모드 설정", null));
    }

    // Nice little method to create a gui item with a custom name, and description
    protected ItemStack createGuiItem(final Material material, final String name, Boolean isRed, final String... lore) {
        final ItemStack item = new ItemStack(material, 1);
        final ItemMeta meta = item.getItemMeta();
        String _name = (isRed==null)?name:((isRed?ChatColor.RED:ChatColor.GREEN) + name);
        // Set the name of the item
        meta.setDisplayName(_name);

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
        if(e.getRawSlot() == 0) uwm.ChangeOption(UserWorldManager.WorldOption.PRIVATE);
        if(e.getRawSlot() == 1) uwm.ChangeOption(UserWorldManager.WorldOption.CAN_DROP);
        if(e.getRawSlot() == 2) uwm.ChangeOption(UserWorldManager.WorldOption.CAN_BREAK);
        if(e.getRawSlot() == 3) uwm.ChangeOption(UserWorldManager.WorldOption.CAN_PLACE);
        if(e.getRawSlot() == 4) uwm.ChangeOption(UserWorldManager.WorldOption.CAN_SHOOT);
        if(e.getRawSlot() == 5) uwm.ChangeOption(UserWorldManager.WorldOption.CAN_COMMAND);
        if(e.getRawSlot() == 6) uwm.ChangeOption(UserWorldManager.WorldOption.CAN_INTERACT);
        if(e.getRawSlot() == 7) uwm.ChangeOption(UserWorldManager.WorldOption.REDSTONE);
        if(e.getRawSlot() == 8) uwm.ChangeOption(UserWorldManager.WorldOption.CAN_PVP);
        if(e.getRawSlot() == 9) uwm.ChangeOption(UserWorldManager.WorldOption.CAN_SHOW_FIREWORK);
        if(e.getRawSlot() <= 9){
            e.getWhoClicked().sendMessage(ChatColor.GREEN + "설정되었습니다");
            e.getWhoClicked().closeInventory();
        }
        if(e.getRawSlot() == 10){
            e.getWhoClicked().sendMessage(ChatColor.GREEN + "채팅으로 자신의 월드 이름을 적어주세요, Quit 메세지로 나올 수 있습니다(이 채팅은 다른 사람들에게 안보여집니다)");
            Chatting cht = new Chatting();
            cht.Chatset((Player) e.getWhoClicked(), "Name", e.getWhoClicked().getWorld());
            e.getWhoClicked().closeInventory();
        }
        if(e.getRawSlot() == 11){
            e.getWhoClicked().sendMessage(ChatColor.GREEN + "스폰 위치를 본인 위치로 설정했습니다");
            uwm.SetTeleportLocation(e.getWhoClicked().getLocation());
            e.getWhoClicked().closeInventory();
        }
        if(e.getRawSlot() == 12){
            GUI_GAMEMODE guiGamemode = new GUI_GAMEMODE(p);
            Bukkit.getPluginManager().registerEvents(guiGamemode, wm);
            guiGamemode.openInventory(e.getWhoClicked());
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
