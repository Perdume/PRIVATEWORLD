package prs.Command;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import prs.gui.ChoosePlayerWorld;
import prs.gui.PlayerWorldList;
import prs.gui.WorldList;
import prs.gui.option;
import prs.privateworld.PrivateWorld;
import prs.world.WorldBanPlayer;
import prs.world.WorldManager;

import java.util.Objects;

public class UserCommand implements CommandExecutor {
    private final PrivateWorld wm = PrivateWorld.getPlugin(PrivateWorld.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player player)) return false;
        WorldList wl = new WorldList(player);
        WorldBanPlayer wb = new WorldBanPlayer();
        WorldManager wrm = new WorldManager();
        if (args[0].equalsIgnoreCase("생성") || args[0].equalsIgnoreCase("create")) {
            if (wrm.PlayerWorldCount(player) < 3) {
                wm.Worlds.CreatePlayerWorld(player);
            } else {
                player.sendMessage(ChatColor.RED + "3개 이상 만들 수 없습니다");
            }
        }
        if (args[0].equalsIgnoreCase("삭제") || args[0].equalsIgnoreCase("delete")) {
            if (Objects.equals(wrm.wcon(player.getWorld()).getName(), player.getName())) {
                wrm.Deleteworld(player.getWorld());
            } else {
                player.sendMessage(ChatColor.RED + "본인 월드에서 시도해주세요");
            }
        }
        if (args[0].equalsIgnoreCase("옵션") || args[0].equalsIgnoreCase("option")) {
            if (Objects.equals(wrm.wcon(player.getWorld()).getName(), player.getName())) {
                option option = new option(player, player.getWorld());
                Bukkit.getPluginManager().registerEvents(option, wm);
                option.openInventory(player);
            } else {
                player.sendMessage(ChatColor.RED + "본인 월드에서 시도해주세요");
            }
        }

        if (args[0].equalsIgnoreCase("방문") || args[0].equalsIgnoreCase("visit")) {
            PlayerWorldList pg = new PlayerWorldList(player);
            Bukkit.getPluginManager().registerEvents(pg, wm);
            pg.openInventory(player);
        }
        if (args[0].equalsIgnoreCase("내월드") || args[0].equalsIgnoreCase("myworld")) {
            ChoosePlayerWorld cpw = new ChoosePlayerWorld(player, player);
            Bukkit.getPluginManager().registerEvents(cpw, wm);
            cpw.openInventory(player);
        }
        if (args[0].equalsIgnoreCase("로비") || args[0].equalsIgnoreCase("lobby")) {
            player.teleport(wm.worldManager.getLobby());

        }
        if (args[0].equalsIgnoreCase("밴") || args[0].equalsIgnoreCase("ban")) {
            if (Objects.equals(wrm.wcon(player.getWorld()).getName(), player.getName())) {
                player.sendMessage(ChatColor.RED + "본인 월드에서 시도해주세요");
                return true;
            }
            for (OfflinePlayer p1 : Bukkit.getOfflinePlayers()) {
                if (args[1].equalsIgnoreCase(p1.getName())) {
                    wb.BanPlayer(player, p1);
                    return true;
                }
            }
        }
        if (args[0].equalsIgnoreCase("언밴") || args[0].equalsIgnoreCase("unban")) {
            if (Objects.equals(wrm.wcon(player.getWorld()).getName(), player.getName())) {
                player.sendMessage(ChatColor.RED + "본인 월드에서 시도해주세요");
                return true;
            }
            for (OfflinePlayer p1 : Bukkit.getOfflinePlayers()) {
                if (args[1].equalsIgnoreCase(p1.getName())) {
                    wb.UnbanPlayer(player, p1);
                    return true;
                }
            }
        }
        if (args[0].equalsIgnoreCase("머리") || args[0].equalsIgnoreCase("head")) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skull = (SkullMeta) head.getItemMeta();
            skull.setOwner(args[1]);
            head.setItemMeta(skull);
            player.getInventory().addItem(head);
        }

        return true;
    }
}
