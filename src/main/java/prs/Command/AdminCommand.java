package prs.Command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import prs.ScoreBoard.Helper;
import prs.ScoreBoard.scoreboard;
import prs.gui.redrib;
import prs.privateworld.PrivateWorld;
import prs.world.WorldManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdminCommand implements CommandExecutor {
    private PrivateWorld wm = PrivateWorld.getPlugin(PrivateWorld.class);
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            redrib redrib = new redrib();
            scoreboard sc = new scoreboard();
            WorldManager wrm = new WorldManager();
            try {
                //Addmap
                if (args[0].equalsIgnoreCase("addmap")) {
                    List<String> temp = (List<String>) wm.worldManager.getConfig().getList("DefaultWorlds");
                    boolean contains = temp.contains(player.getWorld().getName());
                    if (temp == null) {
                        ArrayList<String> temp1 = new ArrayList<>(Arrays.asList(player.getWorld().getName()));
                        temp = temp1;
                        wm.worldManager.getConfig().set("DefaultWorlds", temp);
                        player.sendMessage(ChatColor.GREEN + "Already added!");
                    } else if (contains == false) {
                        temp.add(player.getWorld().getName());
                        wm.worldManager.getConfig().set("DefaultWorlds", temp);
                        player.sendMessage(ChatColor.GREEN + "Successfully added");
                    } else {
                        player.sendMessage(ChatColor.RED + "Already added!");
                    }
                    wm.worldManager.saveconfig();
                }
                //Delmap
                if (args[0].equalsIgnoreCase("delmap")) {
                    List<String> temp = (List<String>) wm.worldManager.getConfig().getList("DefaultWorlds");
                    boolean contains = temp.contains(player.getWorld().getName());
                    if (temp == null) {
                        player.sendMessage(ChatColor.RED + "File is null");
                    } else if (contains == true) {
                        temp.remove(player.getWorld().getName());
                        wm.worldManager.getConfig().set("DefaultWorlds", temp);
                        player.sendMessage(ChatColor.GREEN + "Successfully deleted");
                    } else {
                        player.sendMessage(ChatColor.RED + "Already deleted!");
                    }
                    wm.worldManager.saveconfig();
                }
                if (args[0].equalsIgnoreCase("worldlist")) {
                    redrib.PlayerWorldList(player);
                }
                if (args[0].equalsIgnoreCase("delall")) {
                    List<String> temp = (List<String>) wm.worldManager.getConfig().getList("PlayerWorlds");
                    player.sendMessage(String.valueOf(temp));
                    for (String s : temp) {
                        wrm.Deleteworld(s);
                    }
                }
                if (args[0].equalsIgnoreCase("SetLobby")) {
                    wm.worldManager.getConfig().set("Lobby", player.getLocation());
                    player.sendMessage(ChatColor.GREEN + "SuccessFully Location Set!");
                    wm.worldManager.saveconfig();
                }
                if (args[0].equalsIgnoreCase("ReloadScore")) {
                    for (Player p: Bukkit.getOnlinePlayers()) {
                        if (Helper.hasScore(p)) {
                            Helper.removeScore(p);
                        }
                        sc.createScoreboard(p);
                    }
                }
                if (args[0].equalsIgnoreCase("TestSpawn")) {
                    Entity am = player.getWorld().spawnEntity(player.getLocation(), EntityType.ZOMBIE);
                    am.setGlowing(true);
                    am.setCustomName("IM TEST");
                    am.setCustomNameVisible(true);
                    ((Monster) am).setTarget(player);
                    ((Monster) am).getEquipment().setHelmet(new ItemStack(Material.GOLDEN_HELMET));
                }
            }
            catch(Exception e){
                player.sendMessage(ChatColor.RED + e.getMessage() + "\n" + ChatColor.GREEN + e.getCause());
            }
        }
        return true;
    }
}

