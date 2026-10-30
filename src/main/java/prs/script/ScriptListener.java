package prs.script;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.BookMeta;
import prs.data.ScriptManager;
import prs.privateworld.PrivateWorld;

import java.util.List;

/**
 * Bridges Bukkit events to Workshop scripts:
 * <ul>
 *   <li>saving code written in a script edit book</li>
 *   <li>turning player activity into script triggers</li>
 * </ul>
 */
public class ScriptListener implements Listener {

    private final PrivateWorld plugin;
    private final ScriptManager scripts;
    private final ScriptRunner runner;

    public ScriptListener(PrivateWorld plugin, ScriptManager scripts, ScriptRunner runner) {
        this.plugin = plugin;
        this.scripts = scripts;
        this.runner = runner;
    }

    // -------------------------------------------------------------------------
    // Code editing
    // -------------------------------------------------------------------------

    @EventHandler(ignoreCancelled = true)
    public void onEditBook(PlayerEditBookEvent e) {
        String id = ScriptBooks.getScriptId(e.getPreviousBookMeta());
        if (id == null) return;
        Player p = e.getPlayer();

        if (!scripts.exists(id)) {
            p.sendMessage(ChatColor.RED + "삭제된 스크립트의 책입니다");
            return;
        }
        if (!scripts.isAuthor(id, p)) {
            e.setCancelled(true);
            p.sendMessage(ChatColor.RED + "다른 사람의 스크립트는 수정할 수 없습니다");
            return;
        }
        if (e.isSigning()) {
            e.setSigning(false);
            p.sendMessage(ChatColor.GRAY + "스크립트 책은 서명하지 않아도 '완료'만 누르면 저장됩니다");
        }

        BookMeta meta = e.getNewBookMeta();
        ScriptBooks.tag(meta, id);
        e.setNewBookMeta(meta);

        String source = ScriptBooks.readSource(meta);
        int max = plugin.configManager.getMaxScriptLength();
        if (source.length() > max) {
            p.sendMessage(ChatColor.RED + "코드가 너무 깁니다 (" + source.length() + "/" + max + "자). 저장하지 않았습니다");
            return;
        }

        scripts.updateSource(id, source);
        ScriptParser.Result result = scripts.compileDraft(id);
        String name = scripts.getName(id);
        if (result.ok()) {
            p.sendMessage(ChatColor.GREEN + "'" + ChatColor.YELLOW + name + ChatColor.GREEN + "' 저장 완료! "
                    + ChatColor.GRAY + "(오류 없음, 이벤트 " + result.script().getTriggers().size() + "개)");
            if (scripts.isInstalled(p.getWorld(), id) && p.getUniqueId().equals(ScriptManager.ownerOf(p.getWorld()))) {
                p.sendMessage(ChatColor.AQUA + "현재 월드에 바로 적용되었습니다");
            } else {
                p.sendMessage(ChatColor.GRAY + "/pv script 에서 내 월드에 설치해 테스트해보세요");
            }
            if (scripts.isPublished(id) && scripts.hasUnpublishedChanges(id)) {
                p.sendMessage(ChatColor.GRAY + "공개 버전에 반영하려면 '공개 / 업데이트' 를 누르세요");
            }
        } else {
            sendErrors(p, name, result.errors());
        }
    }

    /** Prints compile errors with line numbers. */
    public static void sendErrors(Player p, String name, List<ScriptParser.ScriptError> errors) {
        p.sendMessage(ChatColor.RED + "'" + ChatColor.YELLOW + name + ChatColor.RED + "' 에 오류가 "
                + errors.size() + "개 있습니다:");
        int shown = Math.min(errors.size(), 8);
        for (int i = 0; i < shown; i++) {
            p.sendMessage(ChatColor.GRAY + " - " + ChatColor.WHITE + errors.get(i));
        }
        if (errors.size() > shown) p.sendMessage(ChatColor.GRAY + " ... 외 " + (errors.size() - shown) + "개");
    }

    // -------------------------------------------------------------------------
    // Triggers
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChangedWorld(PlayerChangedWorldEvent e) {
        runner.handleLeave(e.getPlayer(), e.getFrom());
        runner.handleJoin(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        // Wait until the lobby teleport (1 tick) has happened
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) runner.handleJoin(p);
        }, 3L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        runner.handleLeave(e.getPlayer(), e.getPlayer().getWorld());
        runner.forget(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Location from = e.getFrom();
        Location to = e.getTo();
        if (to == null || from.getWorld() != to.getWorld()) return;
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) return;
        if (!scripts.hasScripts(to.getWorld())) return;
        runner.handleMove(e.getPlayer(), from, to);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND) return;
        Block b = e.getClickedBlock();
        if (b == null || !scripts.hasScripts(b.getWorld())) return;
        runner.handleClick(e.getPlayer(), b.getType(), b.getX(), b.getY(), b.getZ());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        if (!scripts.hasScripts(victim.getWorld())) return;
        runner.markDeath(victim);
        Player killer = victim.getKiller();
        if (killer != null && !killer.equals(victim) && killer.getWorld().equals(victim.getWorld())) {
            runner.handleKill(killer);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        Location loc = runner.respawnLocation(p);
        if (loc != null) e.setRespawnLocation(loc);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) runner.handleRespawned(p);
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        String msg = e.getMessage();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (p.isOnline()) runner.handleChat(p, msg);
        });
    }
}
