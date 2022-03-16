package prs.Command;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import prs.gui.PlayerGUI;
import prs.gui.option;
import prs.gui.redrib;
import prs.privateworld.PrivateWorld;
import prs.world.WorldBanPlayer;
import prs.world.WorldManager;

public class UserCommand implements CommandExecutor {
    private PrivateWorld wm = PrivateWorld.getPlugin(PrivateWorld.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        redrib redrib = new redrib();
        option option = new option();
        PlayerGUI pg = new PlayerGUI();
        WorldBanPlayer wb = new WorldBanPlayer();
        WorldManager wrm = new WorldManager();
        if (sender instanceof Player) {
            Player player = (Player) sender;
            try {
                if (args[0].equalsIgnoreCase("생성") || args[0].equalsIgnoreCase("create")) {
                    if (wrm.PlayerWorldCount(player) < 3) {
                        redrib.CreatePlayerWorld(player);
                    } else {
                        player.sendMessage(ChatColor.RED + "3개 이상 만들 수 없습니다");
                    }
                }
                if (args[0].equalsIgnoreCase("삭제") || args[0].equalsIgnoreCase("delete")) {
                    try {
                        if (wrm.wcon(player.getWorld()).getName() == player.getName()) {
                            wrm.Deleteworld(player.getWorld().getName());
                        } else {
                            player.sendMessage(ChatColor.RED + "본인 월드에서 시도해주세요");
                        }
                    } catch (Exception e) {
                        player.sendMessage(ChatColor.RED + "본인 월드에서 시도해주세요");
                    }
                }
                if (args[0].equalsIgnoreCase("옵션") || args[0].equalsIgnoreCase("option")) {
                    try {
                        if (wrm.wcon(player.getWorld()).getName() == player.getName()) {
                            option.WorldOption(player, null);
                        } else {
                            player.sendMessage(ChatColor.RED + "본인 월드에서 시도해주세요");
                        }
                    } catch (Exception e) {
                        player.sendMessage(ChatColor.RED + "본인 월드에서 시도해주세요");
                    }

                }

                if (args[0].equalsIgnoreCase("방문") || args[0].equalsIgnoreCase("visit")) {
                    pg.PlayerWorldList(player);
                }
                if (args[0].equalsIgnoreCase("내월드") || args[0].equalsIgnoreCase("myworld")) {
                    pg.WorldList(player, player);
                }
                if (args[0].equalsIgnoreCase("로비") || args[0].equalsIgnoreCase("lobby")) {
                    Location loc = (Location) wm.worldManager.getConfig().get("Lobby");
                    player.teleport(loc);

                }
                if (args[0].equalsIgnoreCase("밴") || args[0].equalsIgnoreCase("ban")) {
                    try {
                        if (wrm.wcon(player.getWorld()).getName() == player.getName()) {
                            for (OfflinePlayer p1: Bukkit.getOfflinePlayers()) {
                                if (args[1].equalsIgnoreCase(p1.getName())) {
                                    wb.BanPlayer(player, p1);
                                    return true;
                                }
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "본인 월드에서 시도해주세요");
                        }
                    } catch (Exception e) {
                        player.sendMessage(ChatColor.RED + "본인 월드에서 시도해주세요");
                    }
                }
                if (args[0].equalsIgnoreCase("언밴") || args[0].equalsIgnoreCase("unban")) {
                    try {
                        if (wrm.wcon(player.getWorld()).getName() == player.getName()) {
                            for (OfflinePlayer p1: Bukkit.getOfflinePlayers()) {
                                if (args[1].equalsIgnoreCase(p1.getName())) {
                                    wb.UnbanPlayer(player, p1);
                                    return true;
                                }
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "본인 월드에서 시도해주세요");
                        }
                    } catch (Exception e) {
                        player.sendMessage(ChatColor.RED + "본인 월드에서 시도해주세요");
                    }
                }
                if (args[0].equalsIgnoreCase("머리") || args[0].equalsIgnoreCase("head")) {
                        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                        SkullMeta skull = (SkullMeta) head.getItemMeta();
                        skull.setOwner(args[1]);
                        head.setItemMeta(skull);
                        player.getInventory().addItem(head);
                }
                else {

                }
            }
            catch(Exception e){

            }

        }

        return true;
    }
}
