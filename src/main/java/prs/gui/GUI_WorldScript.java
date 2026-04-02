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
import prs.data.ScriptManager;
import prs.privateworld.PrivateWorld;
import prs.world.WorldManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * In-game GUI for managing per-world event-driven scripts.
 *
 * <h3>Layout (54 slots)</h3>
 * <ul>
 *   <li>Slots 10, 12, 14, 16 – one button per {@link ScriptManager.Trigger}
 *       (right-click = clear)</li>
 *   <li>Slot 22 – open web editor (get URL)</li>
 *   <li>Slot 49 – back to Workshop</li>
 * </ul>
 *
 * <p>Script editing is done via the web browser editor.
 * Use {@code /privateworld script} in-game or click slot 22 to get the URL.
 */
public class GUI_WorldScript implements Listener {

    private static final int[] TRIGGER_SLOTS = {10, 12, 14, 16};
    private static final int SLOT_WEB_OPEN = 22;
    private static final int SLOT_BACK     = 49;

    private final Inventory inv;
    private final PrivateWorld plugin = PrivateWorld.getPlugin(PrivateWorld.class);
    private final WorldManager worldMgr = new WorldManager();
    private final Player p;
    private final World world;

    public GUI_WorldScript(Player p) {
        inv = Bukkit.createInventory(null, 54, "월드 스크립트");
        this.p = p;
        this.world = p.getWorld();
        loadPage();
    }

    // -------------------------------------------------------------------------
    // Render
    // -------------------------------------------------------------------------

    private void loadPage() {
        inv.clear();
        ScriptManager scriptMgr = plugin.scriptManager;
        ScriptManager.Trigger[] triggers = ScriptManager.Trigger.values();

        for (int i = 0; i < triggers.length; i++) {
            ScriptManager.Trigger trigger = triggers[i];
            String raw = scriptMgr.getRawScript(world.getName(), trigger);
            long lineCount = raw.isBlank() ? 0 : raw.lines().filter(l -> !l.isBlank() && !l.stripLeading().startsWith("#")).count();
            Material mat = triggerMaterial(trigger);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + trigger.description);
            lore.add(ChatColor.GRAY + "스크립트 줄 수: " + ChatColor.WHITE + lineCount + "줄");
            if (!raw.isBlank()) {
                lore.add(ChatColor.DARK_GRAY + "--- 미리보기 ---");
                String[] lines = raw.split("\n", -1);
                int shown = 0;
                for (String line : lines) {
                    if (shown >= 3) break;
                    String t = line.strip();
                    if (!t.isEmpty() && !t.startsWith("#")) {
                        lore.add(ChatColor.GRAY + (t.length() > 38 ? t.substring(0, 35) + "…" : t));
                        shown++;
                    }
                }
                long remaining = lineCount - shown;
                if (remaining > 0) lore.add(ChatColor.DARK_GRAY + "... +" + remaining + "줄 더");
            }
            lore.add(ChatColor.RED + "우클릭: " + ChatColor.WHITE + "스크립트 초기화");

            inv.setItem(TRIGGER_SLOTS[i], makeItem(mat,
                    ChatColor.GOLD + trigger.displayName + " 이벤트",
                    lore.toArray(new String[0])));
        }

        // Web editor button
        boolean hasScript = scriptMgr.hasAnyScript(world.getName());
        boolean webEnabled = plugin.webScriptServer != null;
        inv.setItem(SLOT_WEB_OPEN, makeItem(Material.COMMAND_BLOCK,
                ChatColor.GREEN + "웹 에디터 열기",
                webEnabled
                        ? ChatColor.GRAY + "스크립트를 브라우저에서 편집합니다."
                        : ChatColor.RED  + "(웹 에디터가 비활성화되어 있습니다)",
                hasScript
                        ? ChatColor.AQUA + "스크립트 있음 – 클릭하여 링크 받기"
                        : ChatColor.GRAY + "스크립트 없음 – 클릭하여 링크 받기",
                ChatColor.DARK_GRAY + "또는 /privateworld script 사용"));

        inv.setItem(SLOT_BACK, makeItem(Material.NETHER_STAR, ChatColor.YELLOW + "워크샵으로"));
    }

    private Material triggerMaterial(ScriptManager.Trigger trigger) {
        return switch (trigger) {
            case ENTER   -> Material.LIME_DYE;
            case LEAVE   -> Material.RED_DYE;
            case DEATH   -> Material.BONE;
            case RESPAWN -> Material.TOTEM_OF_UNDYING;
        };
    }

    private ItemStack makeItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat, 1);
        ItemMeta meta  = item.getItemMeta();
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

        if (slot == SLOT_BACK) {
            player.closeInventory();
            GUI_Workshop ws = new GUI_Workshop(player);
            Bukkit.getPluginManager().registerEvents(ws, plugin);
            ws.openInventory(player);
            return;
        }

        if (slot == SLOT_WEB_OPEN) {
            if (!isOwner(player)) {
                player.sendMessage(ChatColor.RED + "본인 월드에서만 스크립트를 편집할 수 있습니다.");
                return;
            }
            if (plugin.webScriptServer == null) {
                player.sendMessage(ChatColor.RED + "웹 에디터가 비활성화되어 있습니다 (config.yml: script-web-port)");
                return;
            }
            player.closeInventory();
            String token = plugin.webScriptServer.issueToken(player, world.getName());
            String host  = plugin.configManager.getScriptWebHost();
            int    port  = plugin.configManager.getScriptWebPort();
            String url   = "http://" + host + ":" + port + "/?token=" + token;
            player.sendMessage(ChatColor.GOLD + "=== 웹 스크립트 에디터 ===");
            player.sendMessage(ChatColor.GRAY + "아래 링크를 클릭하거나 브라우저에 붙여넣으세요:");
            player.sendMessage(ChatColor.AQUA + url);
            player.sendMessage(ChatColor.GRAY + "(링크는 30분 동안 유효합니다)");
            return;
        }

        ScriptManager.Trigger[] triggers = ScriptManager.Trigger.values();
        for (int i = 0; i < triggers.length; i++) {
            if (TRIGGER_SLOTS[i] != slot) continue;
            ScriptManager.Trigger trigger = triggers[i];
            if (e.isRightClick()) {
                if (!isOwner(player)) {
                    player.sendMessage(ChatColor.RED + "본인 월드에서만 스크립트를 수정할 수 있습니다.");
                    return;
                }
                plugin.scriptManager.setRawScript(world.getName(), trigger, "");
                player.sendMessage(ChatColor.GREEN + trigger.displayName + " 이벤트 스크립트가 초기화되었습니다.");
                loadPage();
            }
            return;
        }
    }

    private boolean isOwner(Player player) {
        return worldMgr.getWorldOwner(world) != null
                && worldMgr.getWorldOwner(world).getUniqueId().equals(player.getUniqueId());
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
