package prs.Command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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
        if (sender instanceof Player player) {
            if (command.getName().equalsIgnoreCase("프라이빗월드")) {
                if (args.length == 1) {
                    return new ArrayList<>(Arrays.asList("생성", "삭제", "방문", "옵션", "내월드", "로비", "밴", "언밴", "머리", "도움말"));
                }
                if (args.length == 2) {
                    if (args[0].equalsIgnoreCase("밴")) return getUnbannedPlayers(player);
                    if (args[0].equalsIgnoreCase("언밴")) return getBannedPlayers(player);
                }
            }
            if (command.getName().equalsIgnoreCase("privateworld")) {
                if (args.length == 1) {
                    return new ArrayList<>(Arrays.asList("create", "delete", "visit", "option", "myworld", "ban", "unban", "lobby", "head", "help"));
                }
                if (args.length == 2) {
                    if (args[0].equalsIgnoreCase("ban")) return getUnbannedPlayers(player);
                    if (args[0].equalsIgnoreCase("unban")) return getBannedPlayers(player);
                }
            }
            if (command.getName().equalsIgnoreCase("privateworldAdmin")) {
                if (args.length == 1) {
                    return new ArrayList<>(Arrays.asList("addmap", "delmap", "delall", "SetLobby", "Worlds", "ReloadScore", "TestSpawn", "reload"));
                }
            }
        }
        return null;
    }

    /** Returns names of players who are NOT yet banned in the given player's current world (candidates for /ban). */
    public List<String> getUnbannedPlayers(Player player) {
        List<String> list = new ArrayList<>();
        for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
            if (p.getName() == null) continue;
            if (!wb.IsPlayerBanned(p, player.getWorld())) {
                list.add(p.getName());
            }
        }
        return list;
    }

    /** Returns names of players who ARE banned in the given player's current world (candidates for /unban). */
    public List<String> getBannedPlayers(Player player) {
        List<String> list = new ArrayList<>();
        for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
            if (p.getName() == null) continue;
            if (wb.IsPlayerBanned(p, player.getWorld())) {
                list.add(p.getName());
            }
        }
        return list;
    }
}

