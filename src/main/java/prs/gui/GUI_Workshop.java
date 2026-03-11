package prs.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import prs.Data.WorkshopManager;
import prs.Main.Chatting;
import prs.privateworld.PrivateWorld;
import prs.world.WorldManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Workshop GUI – lets players browse, apply, and delete world-option presets
 * shared by the community.
 *
 * <ul>
 *   <li>Slots 0-44 – preset list (paginated, 45 per page)</li>
 *   <li>Slot 45 – previous page (when page > 1)</li>
 *   <li>Slot 46 – "Save current world as preset" button</li>
 *   <li>Slot 53 – next page (when more presets exist)</li>
 * </ul>
 *
 * Left-click a preset → apply it to the player's current world.<br>
 * Right-click a preset → delete it (owner or op only).
 */
public class GUI_Workshop implements Listener {

    private static final int PAGE_SIZE = 45;
    private static final int SLOT_PREV = 45;
    private static final int SLOT_SAVE = 46;
    private static final int SLOT_NEXT = 53;

    private final Inventory inv;
    private final PrivateWorld wm = PrivateWorld.getPlugin(PrivateWorld.class);
    private final Player p;
    private final WorldManager wrm = new WorldManager();

    private int page = 1;
    private List<String> presetIds = new ArrayList<>();

    public GUI_Workshop(Player p) {
        inv = Bukkit.createInventory(null, 54, "워크샵");
        this.p = p;
        loadPage();
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void loadPage() {
        inv.clear();
        WorkshopManager wsm = wm.workshopManager;
        presetIds = wsm.getAllPresetIds();

        if (presetIds.isEmpty()) {
            inv.setItem(22, createGuiItem(Material.BARRIER,
                    ChatColor.RED + "등록된 프리셋이 없습니다",
                    ChatColor.GRAY + "아래 저장 버튼으로 첫 프리셋을 만들어보세요!"));
        } else {
            int startIndex = (page - 1) * PAGE_SIZE;
            int endIndex = Math.min(page * PAGE_SIZE, presetIds.size());
            for (int i = startIndex; i < endIndex; i++) {
                String id = presetIds.get(i);
                String name = wsm.getPresetName(id);
                String authorName = wsm.getPresetAuthorName(id);
                UUID authorId = wsm.getPresetAuthor(id);
                boolean isOwn = authorId != null && authorId.equals(p.getUniqueId());

                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "제작자: " + ChatColor.GREEN + authorName);
                lore.add(ChatColor.AQUA + "좌클릭: " + ChatColor.WHITE + "내 월드에 적용");
                if (isOwn || p.isOp()) {
                    lore.add(ChatColor.RED + "우클릭: " + ChatColor.WHITE + "삭제");
                }
                inv.setItem(i - startIndex, createGuiItem(Material.BOOK,
                        ChatColor.YELLOW + name, lore.toArray(new String[0])));
            }
        }

        // Navigation row
        if (page > 1) {
            inv.setItem(SLOT_PREV, createGuiItem(Material.ARROW, ChatColor.GREEN + "이전 페이지"));
        }
        inv.setItem(SLOT_SAVE, createGuiItem(Material.WRITABLE_BOOK,
                ChatColor.GOLD + "프리셋으로 저장",
                ChatColor.GRAY + "현재 월드 설정을 프리셋으로 저장합니다",
                ChatColor.GRAY + "(본인 월드에서만 가능)"));
        if (presetIds.size() > page * PAGE_SIZE) {
            inv.setItem(SLOT_NEXT, createGuiItem(Material.ARROW, ChatColor.GREEN + "다음 페이지"));
        }
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    public void openInventory(HumanEntity ent) {
        ent.openInventory(inv);
    }

    // -------------------------------------------------------------------------
    // Event handlers
    // -------------------------------------------------------------------------

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!e.getInventory().equals(inv)) return;
        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        Player player = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();

        // Previous page
        if (slot == SLOT_PREV) {
            if (page > 1) {
                page--;
                loadPage();
            }
            return;
        }

        // Save current world as a new preset
        if (slot == SLOT_SAVE) {
            if (wrm.wcon(player.getWorld()) == null
                    || !wrm.wcon(player.getWorld()).getUniqueId().equals(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "본인 월드에서만 프리셋을 저장할 수 있습니다");
                return;
            }
            player.closeInventory();
            player.sendMessage(ChatColor.GREEN + "채팅으로 프리셋 이름을 입력하세요. 취소하려면 Quit 을 입력하세요.");
            Chatting cht = new Chatting();
            cht.Chatset(player, "WorkshopSave", player.getWorld());
            return;
        }

        // Next page
        if (slot == SLOT_NEXT) {
            if (!presetIds.isEmpty() && presetIds.size() > page * PAGE_SIZE) {
                page++;
                loadPage();
            }
            return;
        }

        // Preset slots 0-44
        if (slot >= 0 && slot < PAGE_SIZE) {
            int presetIndex = (page - 1) * PAGE_SIZE + slot;
            if (presetIndex >= presetIds.size()) return;

            String id = presetIds.get(presetIndex);
            WorkshopManager wsm = wm.workshopManager;

            if (e.isRightClick()) {
                // Delete – only the author or an op may do this
                UUID authorId = wsm.getPresetAuthor(id);
                if ((authorId == null || !authorId.equals(player.getUniqueId())) && !player.isOp()) {
                    player.sendMessage(ChatColor.RED + "자신의 프리셋만 삭제할 수 있습니다");
                    return;
                }
                String name = wsm.getPresetName(id);
                wsm.deletePreset(id);
                player.sendMessage(ChatColor.GREEN + "프리셋 '"
                        + ChatColor.YELLOW + name + ChatColor.GREEN + "' 이 삭제되었습니다");
                loadPage();
            } else {
                // Apply – player must be in their own world
                if (wrm.wcon(player.getWorld()) == null
                        || !wrm.wcon(player.getWorld()).getUniqueId().equals(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "본인 월드에서만 프리셋을 적용할 수 있습니다");
                    return;
                }
                if (wsm.applyPreset(id, player.getWorld())) {
                    player.sendMessage(ChatColor.GREEN + "프리셋 '"
                            + ChatColor.YELLOW + wsm.getPresetName(id)
                            + ChatColor.GREEN + "' 이 적용되었습니다!");
                    player.closeInventory();
                } else {
                    player.sendMessage(ChatColor.RED + "프리셋을 찾을 수 없습니다");
                }
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
