package prs.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import prs.data.WorkshopManager.ContentType;
import prs.main.Chatting;
import prs.privateworld.PrivateWorld;

import java.util.Arrays;

/**
 * A 27-slot GUI that lets the world owner pick a {@link ContentType} for their
 * Workshop submission.  After clicking a type the player is prompted via chat
 * to enter a title, after which the world is published.
 */
public class GUI_TypeSelect implements Listener {

    // Slot positions for each content type (3×3 center block of a 27-slot inv)
    private static final int[] TYPE_SLOTS = {10, 12, 14, 19, 21, 23};

    private final Inventory inv;
    private final PrivateWorld plugin = PrivateWorld.getPlugin(PrivateWorld.class);
    private final Player p;

    public GUI_TypeSelect(Player p) {
        inv = Bukkit.createInventory(null, 27, "콘텐츠 유형 선택");
        this.p = p;
        fillItems();
    }

    private void fillItems() {
        ContentType[] types = ContentType.values();
        for (int i = 0; i < types.length; i++) {
            ContentType ct = types[i];
            ItemStack item = new ItemStack(ct.icon);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ct.color + ct.displayName);
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "클릭하면 이 유형으로 등록합니다",
                    ChatColor.GRAY + "이후 채팅으로 제목을 입력하세요"
            ));
            item.setItemMeta(meta);
            inv.setItem(TYPE_SLOTS[i], item);
        }
    }

    public void openInventory(HumanEntity ent) {
        ent.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!e.getInventory().equals(inv)) return;
        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        Player player = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();

        // Determine which ContentType was clicked
        ContentType[] types = ContentType.values();
        for (int i = 0; i < types.length; i++) {
            if (TYPE_SLOTS[i] == slot) {
                ContentType chosen = types[i];
                player.closeInventory();
                player.sendMessage(ChatColor.GREEN + "'"
                        + chosen.color + chosen.displayName
                        + ChatColor.GREEN + "' 유형이 선택되었습니다.");
                player.sendMessage(ChatColor.GREEN + "채팅으로 콘텐츠 제목을 입력하세요. 취소하려면 Quit 을 입력하세요.");
                Chatting cht = new Chatting();
                cht.startChatInput(player, "WorkshopPublish:" + chosen.name(), player.getWorld());
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        InventoryDragEvent.getHandlerList().unregister(this);
        InventoryClickEvent.getHandlerList().unregister(this);
        InventoryCloseEvent.getHandlerList().unregister(this);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getInventory().equals(inv)) e.setCancelled(true);
    }
}
