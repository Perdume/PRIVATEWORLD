package prs.command;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import prs.gui.ChoosePlayerWorld;
import prs.gui.GUI_Workshop;
import prs.gui.PlayerWorldList;
import prs.gui.WorldOptionMenu;
import prs.privateworld.PrivateWorld;
import prs.world.WorldBanPlayer;
import prs.world.WorldManager;

import java.util.Objects;

public class UserCommand implements CommandExecutor {
    private final PrivateWorld plugin = PrivateWorld.getPlugin(PrivateWorld.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return false;
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        WorldBanPlayer banManager = new WorldBanPlayer();
        WorldManager worldMgr = new WorldManager();

        if (args[0].equalsIgnoreCase("생성") || args[0].equalsIgnoreCase("create")) {
            int maxWorlds = plugin.configManager.getMaxWorlds();
            if (worldMgr.playerWorldCount(player) < maxWorlds) {
                plugin.worlds.createPlayerWorld(player);
            } else {
                player.sendMessage(ChatColor.RED + maxWorlds + "개 이상 만들 수 없습니다");
            }
        }
        else if (args[0].equalsIgnoreCase("삭제") || args[0].equalsIgnoreCase("delete")) {
            if (worldMgr.getWorldOwner(player.getWorld()) == null
                    || !worldMgr.getWorldOwner(player.getWorld()).getUniqueId().equals(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "본인 월드에서 시도해주세요");
            } else {
                worldMgr.deleteWorld(player.getWorld());
            }
        }
        else if (args[0].equalsIgnoreCase("옵션") || args[0].equalsIgnoreCase("option")) {
            if (worldMgr.getWorldOwner(player.getWorld()) == null
                    || !worldMgr.getWorldOwner(player.getWorld()).getUniqueId().equals(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "본인 월드에서 시도해주세요");
            } else {
                WorldOptionMenu option = new WorldOptionMenu(player, player.getWorld());
                Bukkit.getPluginManager().registerEvents(option, plugin);
                option.openInventory(player);
            }
        }
        else if (args[0].equalsIgnoreCase("방문") || args[0].equalsIgnoreCase("visit")) {
            PlayerWorldList pg = new PlayerWorldList(player);
            Bukkit.getPluginManager().registerEvents(pg, plugin);
            pg.openInventory(player);
        }
        else if (args[0].equalsIgnoreCase("내월드") || args[0].equalsIgnoreCase("myworld")) {
            ChoosePlayerWorld cpw = new ChoosePlayerWorld(player, player);
            Bukkit.getPluginManager().registerEvents(cpw, plugin);
            cpw.openInventory(player);
        }
        else if (args[0].equalsIgnoreCase("로비") || args[0].equalsIgnoreCase("lobby")) {
            if (plugin.worldManager.isLobbySet()) {
                player.teleport(plugin.worldManager.getLobby());
            } else {
                player.sendMessage(ChatColor.RED + "로비 위치가 설정되지 않았습니다");
            }
        }
        else if (args[0].equalsIgnoreCase("밴") || args[0].equalsIgnoreCase("ban")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "사용법: /privateworld ban <플레이어>");
                return true;
            }
            if (worldMgr.getWorldOwner(player.getWorld()) == null
                    || !worldMgr.getWorldOwner(player.getWorld()).getUniqueId().equals(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "본인 월드에서 시도해주세요");
                return true;
            }
            for (OfflinePlayer p1 : Bukkit.getOfflinePlayers()) {
                if (args[1].equalsIgnoreCase(p1.getName())) {
                    banManager.banPlayer(player, p1);
                    return true;
                }
            }
            player.sendMessage(ChatColor.RED + "해당 플레이어를 찾을 수 없습니다");
        }
        else if (args[0].equalsIgnoreCase("언밴") || args[0].equalsIgnoreCase("unban")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "사용법: /privateworld unban <플레이어>");
                return true;
            }
            if (worldMgr.getWorldOwner(player.getWorld()) == null
                    || !worldMgr.getWorldOwner(player.getWorld()).getUniqueId().equals(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "본인 월드에서 시도해주세요");
                return true;
            }
            for (OfflinePlayer p1 : Bukkit.getOfflinePlayers()) {
                if (args[1].equalsIgnoreCase(p1.getName())) {
                    banManager.unbanPlayer(player, p1);
                    return true;
                }
            }
            player.sendMessage(ChatColor.RED + "해당 플레이어를 찾을 수 없습니다");
        }
        else if (args[0].equalsIgnoreCase("머리") || args[0].equalsIgnoreCase("head")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "사용법: /privateworld head <플레이어>");
                return true;
            }
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skull = (SkullMeta) head.getItemMeta();
            skull.setOwner(args[1]);
            head.setItemMeta(skull);
            player.getInventory().addItem(head);
        }
        else if (args[0].equalsIgnoreCase("도움말") || args[0].equalsIgnoreCase("help")) {
            sendHelp(player);
        }
        else if (args[0].equalsIgnoreCase("워크샵") || args[0].equalsIgnoreCase("workshop")) {
            GUI_Workshop ws = new GUI_Workshop(player);
            Bukkit.getPluginManager().registerEvents(ws, plugin);
            ws.openInventory(player);
        }
        else {
            player.sendMessage(ChatColor.RED + "알 수 없는 명령어입니다. /privateworld help 를 사용하세요");
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== PrivateWorld 도움말 ===");
        player.sendMessage(ChatColor.YELLOW + "/privateworld create" + ChatColor.WHITE + " - 새 월드를 생성합니다");
        player.sendMessage(ChatColor.YELLOW + "/privateworld delete" + ChatColor.WHITE + " - 현재 월드를 삭제합니다");
        player.sendMessage(ChatColor.YELLOW + "/privateworld option" + ChatColor.WHITE + " - 월드 옵션을 설정합니다");
        player.sendMessage(ChatColor.YELLOW + "/privateworld visit" + ChatColor.WHITE + " - 다른 플레이어 월드를 방문합니다");
        player.sendMessage(ChatColor.YELLOW + "/privateworld myworld" + ChatColor.WHITE + " - 내 월드 목록을 봅니다");
        player.sendMessage(ChatColor.YELLOW + "/privateworld lobby" + ChatColor.WHITE + " - 로비로 이동합니다");
        player.sendMessage(ChatColor.YELLOW + "/privateworld ban <플레이어>" + ChatColor.WHITE + " - 플레이어를 밴합니다");
        player.sendMessage(ChatColor.YELLOW + "/privateworld unban <플레이어>" + ChatColor.WHITE + " - 플레이어 밴을 해제합니다");
        player.sendMessage(ChatColor.YELLOW + "/privateworld head <플레이어>" + ChatColor.WHITE + " - 플레이어 머리를 가져옵니다");
        player.sendMessage(ChatColor.YELLOW + "/privateworld workshop" + ChatColor.WHITE + " - 워크샵을 엽니다 (파쿠르/PVP 등 콘텐츠 탐색/등록)");
    }
}
