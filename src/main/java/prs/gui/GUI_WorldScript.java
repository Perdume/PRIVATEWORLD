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
import prs.main.Chatting;
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
 *       (left-click = edit, right-click = clear)</li>
 *   <li>Slot 22 – usage reference / info item</li>
 *   <li>Slot 49 – back to Workshop</li>
 * </ul>
 */
public class GUI_WorldScript implements Listener {

    private static final int[] TRIGGER_SLOTS = {10, 12, 14, 16};
    private static final int SLOT_INFO = 22;
    private static final int SLOT_BACK = 49;

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
            List<String> actions = scriptMgr.getActions(world.getName(), trigger);
            Material mat = triggerMaterial(trigger);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + trigger.description);
            lore.add(ChatColor.GRAY + "액션 수: " + ChatColor.WHITE + actions.size() + "개");
            if (!actions.isEmpty()) {
                lore.add(ChatColor.DARK_GRAY + "--- 미리보기 ---");
                for (int j = 0; j < Math.min(3, actions.size()); j++) {
                    lore.add(ChatColor.GRAY + actions.get(j));
                }
                if (actions.size() > 3) {
                    lore.add(ChatColor.DARK_GRAY + "... +" + (actions.size() - 3) + "개 더");
                }
            }
            lore.add(ChatColor.AQUA + "좌클릭: " + ChatColor.WHITE + "스크립트 편집");
            lore.add(ChatColor.RED  + "우클릭: " + ChatColor.WHITE + "스크립트 초기화");

            inv.setItem(TRIGGER_SLOTS[i], makeItem(mat,
                    ChatColor.GOLD + trigger.displayName + " 이벤트",
                    lore.toArray(new String[0])));
        }

        inv.setItem(SLOT_INFO, makeItem(Material.BOOK,
                ChatColor.YELLOW + "스크립트 사용법",
                ChatColor.GRAY  + "이벤트 버튼을 클릭하면 스크립트를 편집합니다.",
                ChatColor.GRAY  + "한 줄씩 액션을 입력하고 done 으로 저장하세요.",
                ChatColor.WHITE + "지원 액션:",
                ChatColor.AQUA  + "broadcast <메시지>",
                ChatColor.AQUA  + "message {player} <메시지>",
                ChatColor.AQUA  + "title {player}:<제목>:<부제목>",
                ChatColor.AQUA  + "sound {player} <효과음>",
                ChatColor.AQUA  + "effect {player} <효과> <틱> <레벨>",
                ChatColor.AQUA  + "give {player} <아이템> [수량]",
                ChatColor.AQUA  + "teleport {player} <x> <y> <z>",
                ChatColor.AQUA  + "teleport {player} spawn",
                ChatColor.AQUA  + "kill {player}",
                ChatColor.AQUA  + "gamemode {player} <모드>",
                ChatColor.AQUA  + "command <명령어>",
                ChatColor.YELLOW + "변수: {player}, {world}, {owner}"));

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

        ScriptManager.Trigger[] triggers = ScriptManager.Trigger.values();
        for (int i = 0; i < triggers.length; i++) {
            if (TRIGGER_SLOTS[i] != slot) continue;

            ScriptManager.Trigger trigger = triggers[i];

            if (e.isRightClick()) {
                plugin.scriptManager.setActions(world.getName(), trigger, new ArrayList<>());
                player.sendMessage(ChatColor.GREEN + trigger.displayName
                        + " 이벤트 스크립트가 초기화되었습니다.");
                loadPage();
            } else {
                if (worldMgr.getWorldOwner(world) == null
                        || !worldMgr.getWorldOwner(world).getUniqueId().equals(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "본인 월드에서만 스크립트를 편집할 수 있습니다.");
                    return;
                }
                player.closeInventory();
                showScriptEditPrompt(player, trigger);
                Chatting cht = new Chatting();
                cht.startChatInput(player,
                        "ScriptEdit:" + world.getName() + ":" + trigger.name(),
                        world);
            }
            return;
        }
    }

    /** Prints the current script and instructions to the player's chat. */
    private void showScriptEditPrompt(Player player, ScriptManager.Trigger trigger) {
        List<String> existing = plugin.scriptManager.getActions(world.getName(), trigger);
        player.sendMessage(ChatColor.GOLD + "=== " + trigger.displayName + " 이벤트 스크립트 편집 ===");
        if (existing.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "(현재 스크립트 없음)");
        } else {
            player.sendMessage(ChatColor.GRAY + "현재 스크립트 (" + existing.size() + "줄):");
            for (int j = 0; j < existing.size(); j++) {
                player.sendMessage(ChatColor.DARK_GRAY + (j + 1) + ". "
                        + ChatColor.WHITE + existing.get(j));
            }
        }
        player.sendMessage(ChatColor.GREEN + "액션을 한 줄씩 입력하세요.");
        player.sendMessage(ChatColor.YELLOW + "  done"  + ChatColor.GRAY + " - 저장");
        player.sendMessage(ChatColor.RED    + "  Quit"  + ChatColor.GRAY + " - 취소 (기존 스크립트 유지)");
        player.sendMessage(ChatColor.BLUE   + "  clear" + ChatColor.GRAY + " - 새 목록으로 초기화");
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
