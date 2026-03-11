package prs.Command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import prs.ScoreBoard.Helper;
import prs.ScoreBoard.scoreboard;
import prs.gui.WorldList;
import prs.privateworld.PrivateWorld;
import prs.world.WorldManager;

import java.util.ArrayList;
import java.util.List;

public class AdminCommand implements CommandExecutor {
    private final PrivateWorld wm = PrivateWorld.getPlugin(PrivateWorld.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            scoreboard sc = new scoreboard();
            WorldManager wrm = new WorldManager();
            if (args.length == 0) {
                player.sendMessage(ChatColor.GOLD + "=== PrivateWorld Admin ===");
                player.sendMessage(ChatColor.YELLOW + "addmap, delmap, delall, SetLobby, Worlds, ReloadScore, TestSpawn, reload");
                return true;
            }
            try {
                if (args[0].equalsIgnoreCase("addmap")) {
                    if (wm.worldManager.AddDefaultworld(player.getWorld().getName())) {
                        player.sendMessage(ChatColor.GREEN + "Successfully added");
                    } else player.sendMessage(ChatColor.RED + "Already added!");
                }
                else if (args[0].equalsIgnoreCase("delmap")) {
                    if (wm.worldManager.RemoveDefaultworld(player.getWorld().getName())) {
                        player.sendMessage(ChatColor.GREEN + "Successfully deleted");
                    } else player.sendMessage(ChatColor.RED + "There is no map!");
                }
                else if (args[0].equalsIgnoreCase("delall")) {
                    List<World> temp = new ArrayList<>(wm.worldManager.getWorldList());
                    player.sendMessage(String.valueOf(temp));
                    for (World s : temp) {
                        wrm.Deleteworld(s);
                    }
                }
                else if (args[0].equalsIgnoreCase("SetLobby")) {
                    wm.worldManager.setLobby(player.getLocation());
                    player.sendMessage(ChatColor.GREEN + "SuccessFully Location Set!");
                    wm.worldManager.saveconfig();
                }
                else if (args[0].equalsIgnoreCase("Worlds")) {
                    WorldList wl = new WorldList(player);
                    Bukkit.getPluginManager().registerEvents(wl, wm);
                    wl.openInventory(player);
                }
                else if (args[0].equalsIgnoreCase("ReloadScore")) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (Helper.hasScore(p)) {
                            Helper.removeScore(p);
                        }
                        sc.createScoreboard(p);
                    }
                }
                else if (args[0].equalsIgnoreCase("TestSpawn")) {
                    Entity am = player.getWorld().spawnEntity(player.getLocation(), EntityType.ZOMBIE);
                    am.setGlowing(true);
                    am.setCustomName("IM TEST");
                    am.setCustomNameVisible(true);
                    ((Monster) am).setTarget(player);
                    ((Monster) am).getEquipment().setHelmet(new ItemStack(Material.GOLDEN_HELMET));
                }
                else if (args[0].equalsIgnoreCase("reload")) {
                    wm.configManager.reloadConfig();
                    player.sendMessage(ChatColor.GREEN + "설정 파일을 다시 로드했습니다");
                }
            } catch (Exception e) {
                player.sendMessage(ChatColor.RED + e.getMessage() + "\n" + ChatColor.GREEN + e.getCause());
            }
        }
        return true;
    }
}


