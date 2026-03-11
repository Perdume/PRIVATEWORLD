package prs.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
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
import prs.Data.WorkshopManager;
import prs.Data.WorkshopManager.ContentType;
import prs.privateworld.PrivateWorld;
import prs.world.WorldBanPlayer;
import prs.world.WorldManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Workshop GUI — a Steam-Workshop-style content browser.
 *
 * <h3>Main screen (MAIN)</h3>
 * Shows one icon per {@link ContentType}.  Clicking a category opens the
 * matching world list.  If the player is in their own world a "Register"
 * button is shown so they can publish their world.
 *
 * <h3>Category screen (CATEGORY)</h3>
 * Shows all worlds published under the selected category (paginated, 45 per
 * page).
 * <ul>
 *   <li>Left-click → teleport into the world</li>
 *   <li>Right-click → unpublish (owner or op only)</li>
 *   <li>Slot 45 → previous page</li>
 *   <li>Slot 49 → back to main</li>
 *   <li>Slot 53 → next page</li>
 * </ul>
 *
 * <h3>Option-preset feature</h3>
 * The old option-preset sub-system is still accessible through a dedicated
 * button on the main screen (slot 53).
 */
public class GUI_Workshop implements Listener {

    // -------------------------------------------------------------------------
    // Layout constants
    // -------------------------------------------------------------------------

    // Main screen — category button slots (match ContentType ordinals)
    private static final int[] CATEGORY_SLOTS = {10, 12, 14, 28, 30, 32};
    private static final int SLOT_REGISTER     = 40; // "내 월드 등록"
    private static final int SLOT_PRESET_OPEN  = 44; // "설정 프리셋"

    // Category screen
    private static final int PAGE_SIZE   = 45;
    private static final int SLOT_PREV   = 45;
    private static final int SLOT_BACK   = 49;
    private static final int SLOT_NEXT   = 53;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private enum Screen { MAIN, CATEGORY }

    private final Inventory inv;
    private final PrivateWorld wm  = PrivateWorld.getPlugin(PrivateWorld.class);
    private final WorldManager wrm = new WorldManager();
    private final Player p;

    private Screen screen = Screen.MAIN;
    private ContentType selectedCategory;
    private List<String> categoryWorlds = new ArrayList<>();
    private int page = 1;

    // -------------------------------------------------------------------------
    // Constructor / open
    // -------------------------------------------------------------------------

    public GUI_Workshop(Player p) {
        inv = Bukkit.createInventory(null, 54, "워크샵");
        this.p = p;
        showMain();
    }

    public void openInventory(HumanEntity ent) {
        ent.openInventory(inv);
    }

    // -------------------------------------------------------------------------
    // Main screen
    // -------------------------------------------------------------------------

    private void showMain() {
        inv.clear();
        screen = Screen.MAIN;

        ContentType[] types = ContentType.values();
        for (int i = 0; i < types.length; i++) {
            ContentType ct = types[i];
            int count = wm.workshopManager.getPublishedWorldsByType(ct).size();
            inv.setItem(CATEGORY_SLOTS[i], makeTypeItem(ct, count));
        }

        // "Register" button – only shown when the player is in their own world
        if (isInOwnWorld()) {
            String statusLore = wm.workshopManager.isPublished(p.getWorld().getName())
                    ? ChatColor.YELLOW + "이미 등록된 월드입니다. 클릭하여 재등록"
                    : ChatColor.GRAY  + "현재 월드를 워크샵에 등록합니다";
            inv.setItem(SLOT_REGISTER, makeItem(Material.WRITABLE_BOOK,
                    ChatColor.GOLD + "내 월드 등록",
                    statusLore,
                    ChatColor.GRAY + "(콘텐츠 유형을 선택한 뒤 제목을 입력하세요)"));
        }

        // Option-preset button (secondary feature)
        inv.setItem(SLOT_PRESET_OPEN, makeItem(Material.ENDER_CHEST,
                ChatColor.LIGHT_PURPLE + "설정 프리셋",
                ChatColor.GRAY + "월드 설정을 프리셋으로 저장/적용합니다"));
    }

    private ItemStack makeTypeItem(ContentType ct, int count) {
        ItemStack item = new ItemStack(ct.icon);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ct.color + ct.displayName);
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "등록된 월드: " + ChatColor.WHITE + count + "개",
                ChatColor.AQUA  + "클릭하여 목록을 봅니다"
        ));
        item.setItemMeta(meta);
        return item;
    }

    // -------------------------------------------------------------------------
    // Category screen
    // -------------------------------------------------------------------------

    private void showCategory(ContentType type) {
        inv.clear();
        screen = Screen.CATEGORY;
        selectedCategory = type;
        page = 1;
        categoryWorlds = wm.workshopManager.getPublishedWorldsByType(type);
        renderCategoryPage();
    }

    private void renderCategoryPage() {
        inv.clear();

        int start = (page - 1) * PAGE_SIZE;
        int end   = Math.min(start + PAGE_SIZE, categoryWorlds.size());

        if (categoryWorlds.isEmpty()) {
            inv.setItem(22, makeItem(Material.BARRIER,
                    ChatColor.RED + "등록된 " + selectedCategory.displayName + " 월드가 없습니다",
                    ChatColor.GRAY + "본인 월드를 건축하고 메인 화면에서 등록해보세요!"));
        } else {
            for (int i = start; i < end; i++) {
                String worldName = categoryWorlds.get(i);
                inv.setItem(i - start, makeWorldItem(worldName));
            }
        }

        // Navigation
        if (page > 1) {
            inv.setItem(SLOT_PREV, makeItem(Material.ARROW, ChatColor.GREEN + "이전 페이지"));
        }
        inv.setItem(SLOT_BACK, makeItem(Material.NETHER_STAR, ChatColor.YELLOW + "메인으로"));
        if (end < categoryWorlds.size()) {
            inv.setItem(SLOT_NEXT, makeItem(Material.ARROW, ChatColor.GREEN + "다음 페이지"));
        }
    }

    private ItemStack makeWorldItem(String worldName) {
        WorkshopManager wsm = wm.workshopManager;
        String title      = wsm.getPublishedTitle(worldName);
        String authorName = wsm.getPublishedAuthorName(worldName);
        UUID   authorId   = wsm.getPublishedAuthor(worldName);

        boolean isOwn  = authorId != null && authorId.equals(p.getUniqueId());
        boolean online = Bukkit.getWorld(worldName) != null;

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY  + "제작자: " + ChatColor.GREEN + authorName);
        lore.add(ChatColor.GRAY  + "월드 상태: " + (online ? ChatColor.GREEN + "온라인" : ChatColor.RED + "오프라인"));
        lore.add(ChatColor.AQUA  + "좌클릭: " + ChatColor.WHITE + "입장하기");
        if (isOwn || p.isOp()) {
            lore.add(ChatColor.RED + "우클릭: " + ChatColor.WHITE + "등록 취소");
        }

        Material icon = online ? Material.GRASS_BLOCK : Material.STONE;
        return makeItem(icon, selectedCategory.color + title, lore.toArray(new String[0]));
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private ItemStack makeItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat, 1);
        ItemMeta meta  = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private boolean isInOwnWorld() {
        if (wrm.wcon(p.getWorld()) == null) return false;
        return wrm.wcon(p.getWorld()).getUniqueId().equals(p.getUniqueId());
    }

    private void teleportToWorld(String worldName) {
        World w = Bukkit.getWorld(worldName);
        if (w == null) {
            p.sendMessage(ChatColor.RED + "해당 월드가 현재 오프라인입니다");
            return;
        }
        UserWorldManager uwm = new UserWorldManager(w);
        WorldBanPlayer   wb  = new WorldBanPlayer();
        UUID ownerUuid = wm.workshopManager.getPublishedAuthor(worldName);

        if (uwm.getOption(UserWorldManager.WorldOption.PRIVATE) &&
                (ownerUuid == null || !ownerUuid.equals(p.getUniqueId()))) {
            p.sendMessage(ChatColor.RED + "해당 월드는 비공개 상태입니다");
            return;
        }
        if (wb.IsPlayerBanned(p, w)) {
            p.sendMessage(ChatColor.RED + "해당 월드에 입장할 수 없습니다");
            return;
        }
        GameMode gm = (GameMode) uwm.getWorldFile().get("Option.Gamemode");
        if (gm == null) gm = GameMode.ADVENTURE;
        Location loc = (Location) uwm.getWorldFile().get("Option.TeleportLocation");
        if (loc == null) loc = new Location(w, 0, 64, 0);
        p.teleport(loc);
        p.setGameMode(gm);
        p.closeInventory();
    }

    // -------------------------------------------------------------------------
    // Preset sub-screen (re-opens the old preset GUI)
    // -------------------------------------------------------------------------

    private void openPresetGui() {
        GUI_Workshop_Presets pg = new GUI_Workshop_Presets(p);
        Bukkit.getPluginManager().registerEvents(pg, wm);
        pg.openInventory(p);
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

        if (screen == Screen.MAIN) {
            handleMainClick(player, slot);
        } else {
            handleCategoryClick(player, slot, e.isRightClick());
        }
    }

    private void handleMainClick(Player player, int slot) {
        // Category buttons
        ContentType[] types = ContentType.values();
        for (int i = 0; i < types.length; i++) {
            if (CATEGORY_SLOTS[i] == slot) {
                showCategory(types[i]);
                return;
            }
        }

        if (slot == SLOT_REGISTER) {
            if (!isInOwnWorld()) {
                player.sendMessage(ChatColor.RED + "본인 월드에서만 등록할 수 있습니다");
                return;
            }
            player.closeInventory();
            GUI_TypeSelect ts = new GUI_TypeSelect(player);
            Bukkit.getPluginManager().registerEvents(ts, wm);
            ts.openInventory(player);
            return;
        }

        if (slot == SLOT_PRESET_OPEN) {
            player.closeInventory();
            openPresetGui();
        }
    }

    private void handleCategoryClick(Player player, int slot, boolean rightClick) {
        if (slot == SLOT_PREV && page > 1) {
            page--;
            renderCategoryPage();
            return;
        }
        if (slot == SLOT_BACK) {
            showMain();
            return;
        }
        if (slot == SLOT_NEXT && !categoryWorlds.isEmpty()
                && categoryWorlds.size() > page * PAGE_SIZE) {
            page++;
            renderCategoryPage();
            return;
        }

        if (slot >= 0 && slot < PAGE_SIZE) {
            int idx = (page - 1) * PAGE_SIZE + slot;
            if (idx >= categoryWorlds.size()) return;
            String worldName = categoryWorlds.get(idx);

            if (rightClick) {
                UUID authorId = wm.workshopManager.getPublishedAuthor(worldName);
                if ((authorId == null || !authorId.equals(player.getUniqueId())) && !player.isOp()) {
                    player.sendMessage(ChatColor.RED + "자신의 월드만 등록 취소할 수 있습니다");
                    return;
                }
                String title = wm.workshopManager.getPublishedTitle(worldName);
                wm.workshopManager.unpublishWorld(worldName);
                player.sendMessage(ChatColor.GREEN + "'" + ChatColor.YELLOW + title
                        + ChatColor.GREEN + "' 의 워크샵 등록이 취소되었습니다");
                categoryWorlds = wm.workshopManager.getPublishedWorldsByType(selectedCategory);
                renderCategoryPage();
            } else {
                teleportToWorld(worldName);
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

