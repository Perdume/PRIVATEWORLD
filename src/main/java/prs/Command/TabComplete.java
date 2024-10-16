package prs.Command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.WorldBorder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import prs.world.WorldBanPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class TabComplete implements TabCompleter {
    WorldBanPlayer wb = new WorldBanPlayer();
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (command.getName().equalsIgnoreCase("프라이빗월드")) {
                if (args.length == 1) {
                    return new ArrayList<>(Arrays.asList("생성", "삭제", "방문", "옵션"));
                }
            }
            if (command.getName().equalsIgnoreCase("privateworld")) {
                if (args.length == 1) {
                    return new ArrayList<>(Arrays.asList("create", "delete", "visit", "option", "myworld", "ban", "unban", "lobby", "head"));
                }
                if (args.length == 2) {
                    if (args[0].equalsIgnoreCase("ban")) return getBannedPlayer(player, true);
                    if (args[0].equalsIgnoreCase("unban")) return getBannedPlayer(player, false);
                }
            }
            if (command.getName().equalsIgnoreCase("privateworldAdmin")) {
                if (args.length == 1) {
                    return new ArrayList<>(Arrays.asList("worldlist", "delall"));
                }
            }
        }
        return null;
    }

    public List<String> getBannedPlayer(Player player, Boolean isBanned){
        List<String> list = new ArrayList<>();
        for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
            if (wb.IsPlayerBanned(p, player.getWorld()) == isBanned) continue;
            list.add(p.getName());
        }
        return list;
    }
}
