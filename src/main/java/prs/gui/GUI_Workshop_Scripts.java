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
import prs.data.ScriptManager;
import prs.main.Chatting;
import prs.privateworld.PrivateWorld;
import prs.script.ScriptBooks;
import prs.script.ScriptListener;
import prs.script.ScriptParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Workshop GUI for player-written scripts.
 *
 * <h3>List screen</h3>
 * Three tabs: published scripts, the player's own scripts, and scripts
 * installed in the current world. Clicking a script opens its detail screen.
 * <ul>
 *   <li>Slots 0-44 – paginated script list</li>
 *   <li>Slot 45 / 53 – previous / next page</li>
 *   <li>Slots 46-48 – tabs</li>
 *   <li>Slot 49 – back to the Workshop main screen</li>
 *   <li>Slot 50 – create a new script</li>
 *   <li>Slot 52 – syntax guide</li>
 * </ul>
 *
 * <h3>Detail screen</h3>
 * Edit (Book &amp; Quill), view code, install into / remove from the current
 * world, publish / update, rename, unpublish and delete.
 */
public class GUI_Workshop_Scripts implements Listener {

    public enum Tab {
        PUBLIC   ("공개 스크립트",    Material.COMMAND_BLOCK),
        MINE     ("내 스크립트",      Material.FEATHER),
        INSTALLED("이 월드에 설치됨", Material.GRASS_BLOCK);

        final String title;
        final Material icon;

        Tab(String title, Material icon) {
            this.title = title;
            this.icon = icon;
        }
    }

    private enum Screen { LIST, DETAIL }

    // List screen
    private static final int PAGE_SIZE          = 45;
    private static final int SLOT_PREV          = 45;
    private static final int[] SLOT_TABS        = {46, 47, 48};
    private static final int SLOT_BACK          = 49;
    private static final int SLOT_NEW           = 50;
    private static final int SLOT_GUIDE         = 52;
    private static final int SLOT_NEXT          = 53;

    // Detail screen
    private static final int SLOT_INFO          = 4;
    private static final int SLOT_EDIT          = 19;
    private static final int SLOT_VIEW          = 21;
    private static final int SLOT_INSTALL       = 23;
    private static final int SLOT_PUBLISH       = 25;
    private static final int SLOT_RENAME        = 38;
    private static final int SLOT_UNPUBLISH     = 40;
    private static final int SLOT_DELETE        = 42;

    private final Inventory inv;
    private final PrivateWorld plugin = PrivateWorld.getPlugin(PrivateWorld.class);
    private final ScriptManager scripts = plugin.scriptManager;
    private final Player p;

    private Screen screen = Screen.LIST;
    private Tab tab;
    private int page = 1;
    private List<String> ids = new ArrayList<>();
    private String selected;
    private boolean confirmDelete;

    public GUI_Workshop_Scripts(Player p, Tab tab) {
        inv = Bukkit.createInventory(null, 54, "워크샵 스크립트");
        this.p = p;
        this.tab = tab;
        showList();
    }

    public void openInventory(HumanEntity ent) {
        ent.openInventory(inv);
    }

    // -------------------------------------------------------------------------
    // List screen
    // -------------------------------------------------------------------------

    private void showList() {
        screen = Screen.LIST;
        selected = null;
        ids = loadIds();
        int maxPage = Math.max(1, (ids.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page > maxPage) page = maxPage;
        renderList();
    }

    private List<String> loadIds() {
        List<String> result;
        switch (tab) {
            case PUBLIC -> {
                result = scripts.getPublishedScriptIds();
                result.sort(Comparator.comparingInt(scripts::getInstalls).reversed());
            }
            case MINE -> result = scripts.getScriptsByAuthor(p.getUniqueId());
            default -> result = isInOwnWorld() ? scripts.getInstalledIds(p.getWorld()) : new ArrayList<>();
        }
        return result;
    }

    private void renderList() {
        inv.clear();
        int start = (page - 1) * PAGE_SIZE;
        int end   = Math.min(start + PAGE_SIZE, ids.size());

        if (ids.isEmpty()) {
            inv.setItem(22, emptyItem());
        } else {
            for (int i = start; i < end; i++) {
                inv.setItem(i - start, makeScriptItem(ids.get(i), true));
            }
        }

        if (page > 1) inv.setItem(SLOT_PREV, makeItem(Material.ARROW, ChatColor.GREEN + "이전 페이지"));
        Tab[] tabs = Tab.values();
        for (int i = 0; i < tabs.length; i++) {
            boolean active = tabs[i] == tab;
            inv.setItem(SLOT_TABS[i], makeItem(tabs[i].icon,
                    (active ? ChatColor.GREEN + "▶ " : ChatColor.GRAY.toString()) + tabs[i].title,
                    active ? ChatColor.DARK_GRAY + "현재 탭" : ChatColor.AQUA + "클릭하여 이동"));
        }
        inv.setItem(SLOT_BACK, makeItem(Material.NETHER_STAR, ChatColor.YELLOW + "워크샵으로"));
        int mine = scripts.getScriptsByAuthor(p.getUniqueId()).size();
        inv.setItem(SLOT_NEW, makeItem(Material.WRITABLE_BOOK, ChatColor.GOLD + "새 스크립트 만들기",
                ChatColor.GRAY + "책과 깃펜에 코드를 써서",
                ChatColor.GRAY + "나만의 게임 규칙을 만들어보세요",
                ChatColor.DARK_GRAY + "(" + mine + "/" + plugin.configManager.getMaxScriptsPerPlayer() + ")"));
        inv.setItem(SLOT_GUIDE, makeItem(Material.KNOWLEDGE_BOOK, ChatColor.AQUA + "문법 가이드",
                ChatColor.GRAY + "이벤트·명령어·예제를 확인합니다"));
        if (end < ids.size()) inv.setItem(SLOT_NEXT, makeItem(Material.ARROW, ChatColor.GREEN + "다음 페이지"));
    }

    private ItemStack emptyItem() {
        return switch (tab) {
            case PUBLIC -> makeItem(Material.BARRIER, ChatColor.RED + "공개된 스크립트가 없습니다",
                    ChatColor.GRAY + "첫 번째 스크립트를 만들어 공개해보세요!");
            case MINE -> makeItem(Material.BARRIER, ChatColor.RED + "아직 만든 스크립트가 없습니다",
                    ChatColor.GRAY + "'새 스크립트 만들기' 를 눌러 시작하세요");
            default -> isInOwnWorld()
                    ? makeItem(Material.BARRIER, ChatColor.RED + "이 월드에 설치된 스크립트가 없습니다")
                    : makeItem(Material.BARRIER, ChatColor.RED + "본인 월드에서 확인할 수 있습니다");
        };
    }

    private ItemStack makeScriptItem(String id, boolean clickHint) {
        boolean published = scripts.isPublished(id);
        boolean mine = scripts.isAuthor(id, p);
        String source = mine ? scripts.getSource(id) : scripts.getPublishedSource(id);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "제작자: " + ChatColor.GREEN + scripts.getAuthorName(id));
        if (published) {
            String status = ChatColor.GRAY + "상태: " + ChatColor.GREEN + "공개됨 v" + scripts.getVersion(id);
            if (mine && scripts.hasUnpublishedChanges(id)) status += ChatColor.YELLOW + " (미반영 수정 있음)";
            lore.add(status);
        } else {
            lore.add(ChatColor.GRAY + "상태: " + ChatColor.WHITE + "비공개 (작성 중)");
        }
        lore.add(ChatColor.GRAY + "코드: " + ChatColor.WHITE + ScriptManager.countLines(source) + "줄"
                + ChatColor.GRAY + " · 설치: " + ChatColor.WHITE + scripts.getInstalls(id) + "회");
        if (mine) {
            ScriptParser.Result result = scripts.compileDraft(id);
            if (!result.ok()) lore.add(ChatColor.RED + "오류 " + result.errors().size() + "개 (코드 편집으로 수정)");
        }
        if (isInOwnWorld()) {
            lore.add(scripts.isInstalled(p.getWorld(), id)
                    ? ChatColor.AQUA + "✔ 이 월드에 설치됨"
                    : ChatColor.DARK_GRAY + "이 월드에 미설치");
        }
        if (clickHint) lore.add(ChatColor.AQUA + "클릭: " + ChatColor.WHITE + "자세히 보기");

        Material icon = published ? Material.ENCHANTED_BOOK : Material.WRITABLE_BOOK;
        return makeItem(icon, ChatColor.YELLOW + scripts.getName(id), lore.toArray(new String[0]));
    }

    // -------------------------------------------------------------------------
    // Detail screen
    // -------------------------------------------------------------------------

    private void showDetail(String id) {
        screen = Screen.DETAIL;
        selected = id;
        renderDetail();
    }

    private void renderDetail() {
        inv.clear();
        String id = selected;
        boolean mine = scripts.isAuthor(id, p);
        boolean canManage = mine || p.isOp();
        boolean published = scripts.isPublished(id);

        inv.setItem(SLOT_INFO, makeScriptItem(id, false));

        if (mine) {
            inv.setItem(SLOT_EDIT, makeItem(Material.WRITABLE_BOOK, ChatColor.GOLD + "코드 편집",
                    ChatColor.GRAY + "책과 깃펜을 받습니다",
                    ChatColor.GRAY + "코드를 쓰고 '완료'를 누르면 저장"));
        }
        inv.setItem(SLOT_VIEW, makeItem(Material.BOOK, ChatColor.AQUA + "코드 보기",
                ChatColor.GRAY + (mine ? "작성 중인 코드를 읽습니다" : "공개된 코드를 읽습니다")));

        if (isInOwnWorld()) {
            boolean installed = scripts.isInstalled(p.getWorld(), id);
            inv.setItem(SLOT_INSTALL, installed
                    ? makeItem(Material.RED_DYE, ChatColor.RED + "이 월드에서 제거",
                            ChatColor.GRAY + "현재 월드에서 스크립트를 끕니다")
                    : makeItem(Material.LIME_DYE, ChatColor.GREEN + "이 월드에 설치",
                            ChatColor.GRAY + "현재 월드에서 스크립트가 실행됩니다",
                            mine ? ChatColor.DARK_GRAY + "(내 월드에서는 작성 중인 코드로 실행)"
                                 : ChatColor.DARK_GRAY + "(공개된 버전으로 실행)"));
        } else {
            inv.setItem(SLOT_INSTALL, makeItem(Material.GRAY_DYE, ChatColor.GRAY + "월드에 설치",
                    ChatColor.RED + "본인 월드에서만 설치할 수 있습니다"));
        }

        if (mine) {
            inv.setItem(SLOT_PUBLISH, published
                    ? makeItem(Material.NETHER_STAR, ChatColor.LIGHT_PURPLE + "공개 버전 업데이트",
                            ChatColor.GRAY + "작성 중인 코드를 v" + (scripts.getVersion(id) + 1) + " 로 공개합니다",
                            scripts.hasUnpublishedChanges(id)
                                    ? ChatColor.YELLOW + "반영되지 않은 수정이 있습니다"
                                    : ChatColor.DARK_GRAY + "공개 버전과 같습니다")
                    : makeItem(Material.NETHER_STAR, ChatColor.LIGHT_PURPLE + "워크샵에 공개",
                            ChatColor.GRAY + "다른 플레이어가 자기 월드에",
                            ChatColor.GRAY + "이 스크립트를 설치할 수 있게 됩니다",
                            ChatColor.DARK_GRAY + "(오류가 없어야 공개됩니다)"));
            inv.setItem(SLOT_RENAME, makeItem(Material.NAME_TAG, ChatColor.YELLOW + "이름 변경",
                    ChatColor.GRAY + "채팅으로 새 이름을 입력합니다"));
        }
        if (canManage && published) {
            inv.setItem(SLOT_UNPUBLISH, makeItem(Material.GRAY_DYE, ChatColor.GRAY + "공개 취소",
                    ChatColor.GRAY + "공개 목록에서 내립니다",
                    ChatColor.DARK_GRAY + "(다른 월드에서도 더 이상 실행되지 않습니다)"));
        }
        if (canManage) {
            inv.setItem(SLOT_DELETE, confirmDelete
                    ? makeItem(Material.TNT, ChatColor.DARK_RED + "정말 삭제할까요?",
                            ChatColor.RED + "한 번 더 클릭하면 영구 삭제됩니다")
                    : makeItem(Material.BARRIER, ChatColor.RED + "스크립트 삭제",
                            ChatColor.GRAY + "되돌릴 수 없습니다"));
        }
        inv.setItem(SLOT_BACK, makeItem(Material.NETHER_STAR, ChatColor.YELLOW + "목록으로"));
    }

    // -------------------------------------------------------------------------
    // Helpers
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
        UUID owner = ScriptManager.ownerOf(p.getWorld());
        return owner != null && owner.equals(p.getUniqueId());
    }

    /** Opens a book UI on the next tick (after the inventory has closed). */
    private void openBookLater(Player player, ItemStack book) {
        player.closeInventory();
        Bukkit.getScheduler().runTask(plugin, () -> player.openBook(book));
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

        if (screen == Screen.LIST) {
            handleListClick(player, slot);
        } else {
            handleDetailClick(player, slot);
        }
    }

    private void handleListClick(Player player, int slot) {
        if (slot == SLOT_PREV && page > 1) {
            page--;
            renderList();
            return;
        }
        if (slot == SLOT_NEXT && ids.size() > page * PAGE_SIZE) {
            page++;
            renderList();
            return;
        }
        for (int i = 0; i < SLOT_TABS.length; i++) {
            if (SLOT_TABS[i] == slot) {
                tab = Tab.values()[i];
                page = 1;
                showList();
                return;
            }
        }
        if (slot == SLOT_BACK) {
            player.closeInventory();
            GUI_Workshop ws = new GUI_Workshop(player);
            Bukkit.getPluginManager().registerEvents(ws, plugin);
            ws.openInventory(player);
            return;
        }
        if (slot == SLOT_NEW) {
            if (scripts.getScriptsByAuthor(player.getUniqueId()).size() >= plugin.configManager.getMaxScriptsPerPlayer()) {
                player.sendMessage(ChatColor.RED + "스크립트는 최대 " + plugin.configManager.getMaxScriptsPerPlayer()
                        + "개까지 만들 수 있습니다");
                return;
            }
            player.closeInventory();
            player.sendMessage(ChatColor.GREEN + "채팅으로 스크립트 이름을 입력하세요. 취소하려면 Quit 을 입력하세요.");
            new Chatting().startChatInput(player, "ScriptCreate", player.getWorld());
            return;
        }
        if (slot == SLOT_GUIDE) {
            openBookLater(player, ScriptBooks.guideBook());
            return;
        }
        if (slot >= 0 && slot < PAGE_SIZE) {
            int idx = (page - 1) * PAGE_SIZE + slot;
            if (idx >= ids.size()) return;
            String id = ids.get(idx);
            if (!scripts.exists(id)) {
                showList();
                return;
            }
            confirmDelete = false;
            showDetail(id);
        }
    }

    private void handleDetailClick(Player player, int slot) {
        String id = selected;
        if (slot == SLOT_BACK) {
            confirmDelete = false;
            showList();
            return;
        }
        if (!scripts.exists(id)) {
            player.sendMessage(ChatColor.RED + "이미 삭제된 스크립트입니다");
            showList();
            return;
        }
        boolean mine = scripts.isAuthor(id, player);
        boolean canManage = mine || player.isOp();
        if (slot != SLOT_DELETE) confirmDelete = false;

        if (slot == SLOT_EDIT && mine) {
            player.closeInventory();
            ScriptBooks.removeEditBooks(player, id);
            ItemStack book = ScriptBooks.editBook(id, scripts.getName(id), scripts.getSource(id));
            if (!player.getInventory().addItem(book).isEmpty()) {
                player.sendMessage(ChatColor.RED + "인벤토리에 빈 칸이 필요합니다");
                return;
            }
            player.sendMessage(ChatColor.GREEN + "책과 깃펜을 받았습니다. 코드를 쓰고 '완료'를 누르면 저장됩니다.");
            player.sendMessage(ChatColor.GRAY + "문법은 /pv script guide 에서 볼 수 있습니다");
            return;
        }
        if (slot == SLOT_VIEW) {
            String source = mine ? scripts.getSource(id) : scripts.getPublishedSource(id);
            openBookLater(player, ScriptBooks.viewBook(scripts.getName(id), scripts.getAuthorName(id), source));
            return;
        }
        if (slot == SLOT_INSTALL) {
            if (!isInOwnWorld()) {
                player.sendMessage(ChatColor.RED + "본인 월드에서만 설치할 수 있습니다");
                return;
            }
            if (scripts.isInstalled(player.getWorld(), id)) {
                scripts.uninstall(player.getWorld(), id);
                player.sendMessage(ChatColor.YELLOW + "'" + scripts.getName(id) + "' 를 이 월드에서 제거했습니다");
            } else {
                switch (scripts.install(player.getWorld(), id, player)) {
                    case OK -> {
                        player.sendMessage(ChatColor.GREEN + "'" + ChatColor.YELLOW + scripts.getName(id)
                                + ChatColor.GREEN + "' 를 이 월드에 설치했습니다!");
                        if (mine && !scripts.compileDraft(id).ok() && !scripts.isPublished(id)) {
                            player.sendMessage(ChatColor.RED + "코드에 오류가 있어 수정 전까지는 실행되지 않습니다");
                        }
                    }
                    case NOT_PUBLISHED -> player.sendMessage(ChatColor.RED + "공개되지 않은 스크립트입니다");
                    case LIMIT -> player.sendMessage(ChatColor.RED + "한 월드에는 최대 "
                            + plugin.configManager.getMaxScriptsPerWorld() + "개까지 설치할 수 있습니다");
                    default -> player.sendMessage(ChatColor.RED + "설치할 수 없습니다");
                }
            }
            renderDetail();
            return;
        }
        if (slot == SLOT_PUBLISH && mine) {
            ScriptParser.Result result = scripts.publish(id);
            if (!result.ok()) {
                player.closeInventory();
                player.sendMessage(ChatColor.RED + "오류가 있어 공개할 수 없습니다.");
                ScriptListener.sendErrors(player, scripts.getName(id), result.errors());
                return;
            }
            player.sendMessage(ChatColor.GREEN + "'" + ChatColor.YELLOW + scripts.getName(id) + ChatColor.GREEN
                    + "' v" + scripts.getVersion(id) + " 이(가) 워크샵에 공개되었습니다!");
            renderDetail();
            return;
        }
        if (slot == SLOT_RENAME && mine) {
            player.closeInventory();
            player.sendMessage(ChatColor.GREEN + "채팅으로 새 이름을 입력하세요. 취소하려면 Quit 을 입력하세요.");
            new Chatting().startChatInput(player, "ScriptRename:" + id, player.getWorld());
            return;
        }
        if (slot == SLOT_UNPUBLISH && canManage) {
            if (scripts.unpublish(id)) {
                player.sendMessage(ChatColor.YELLOW + "'" + scripts.getName(id) + "' 의 공개를 취소했습니다");
            }
            renderDetail();
            return;
        }
        if (slot == SLOT_DELETE && canManage) {
            if (!confirmDelete) {
                confirmDelete = true;
                renderDetail();
                return;
            }
            confirmDelete = false;
            String name = scripts.getName(id);
            scripts.delete(id);
            ScriptBooks.removeEditBooks(player, id);
            player.sendMessage(ChatColor.YELLOW + "'" + name + "' 스크립트를 삭제했습니다");
            showList();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!e.getInventory().equals(inv)) return;
        InventoryDragEvent.getHandlerList().unregister(this);
        InventoryClickEvent.getHandlerList().unregister(this);
        InventoryCloseEvent.getHandlerList().unregister(this);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getInventory().equals(inv)) e.setCancelled(true);
    }
}
