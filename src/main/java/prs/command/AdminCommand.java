package prs.command;

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
import prs.scoreboard.Helper;
import prs.scoreboard.WorldScoreboard;
import prs.gui.WorldList;
import prs.privateworld.PrivateWorld;
import prs.world.WorldManager;

import java.util.ArrayList;
import java.util.List;

public class AdminCommand implements CommandExecutor {
    private final PrivateWorld plugin = PrivateWorld.getPlugin(PrivateWorld.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            WorldScoreboard sc = new WorldScoreboard();
            WorldManager worldMgr = new WorldManager();
            if (args.length == 0) {
                player.sendMessage(ChatColor.GOLD + "=== PrivateWorld Admin ===");
                player.sendMessage(ChatColor.YELLOW + "addmap, delmap, delall, SetLobby, Worlds, ReloadScore, TestSpawn, reload, workshoplist, workshopdelete <name>, workshopunpublish <world>");
                return true;
            }
            try {
                if (args[0].equalsIgnoreCase("addmap")) {
                    if (plugin.worldManager.addDefaultWorld(player.getWorld().getName())) {
                        player.sendMessage(ChatColor.GREEN + "Successfully added");
                    } else player.sendMessage(ChatColor.RED + "Already added!");
                }
                else if (args[0].equalsIgnoreCase("delmap")) {
                    if (plugin.worldManager.removeDefaultWorld(player.getWorld().getName())) {
                        player.sendMessage(ChatColor.GREEN + "Successfully deleted");
                    } else player.sendMessage(ChatColor.RED + "There is no map!");
                }
                else if (args[0].equalsIgnoreCase("delall")) {
                    List<World> temp = new ArrayList<>(plugin.worldManager.getWorldList());
                    player.sendMessage(String.valueOf(temp));
                    for (World s : temp) {
                        worldMgr.deleteWorld(s);
                    }
                }
                else if (args[0].equalsIgnoreCase("SetLobby")) {
                    plugin.worldManager.setLobby(player.getLocation());
                    player.sendMessage(ChatColor.GREEN + "SuccessFully Location Set!");
                    plugin.worldManager.saveConfig();
                }
                else if (args[0].equalsIgnoreCase("Worlds")) {
                    WorldList wl = new WorldList(player);
                    Bukkit.getPluginManager().registerEvents(wl, plugin);
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
                    plugin.configManager.reloadConfig();
                    player.sendMessage(ChatColor.GREEN + "설정 파일을 다시 로드했습니다");
                }
                else if (args[0].equalsIgnoreCase("workshoplist")) {
                    // Show published worlds
                    java.util.List<String> worlds = plugin.workshopManager.getAllPublishedWorlds();
                    if (worlds.isEmpty()) {
                        player.sendMessage(ChatColor.YELLOW + "워크샵에 등록된 월드가 없습니다");
                    } else {
                        player.sendMessage(ChatColor.GOLD + "=== 워크샵 등록 월드 (" + worlds.size() + "개) ===");
                        for (String wn : worlds) {
                            prs.data.WorkshopManager.ContentType ct = plugin.workshopManager.getPublishedType(wn);
                            player.sendMessage(ct.color + "[" + ct.displayName + "] "
                                    + ChatColor.YELLOW + plugin.workshopManager.getPublishedTitle(wn)
                                    + ChatColor.GRAY + " | " + ChatColor.GREEN
                                    + plugin.workshopManager.getPublishedAuthorName(wn)
                                    + ChatColor.GRAY + " (" + wn + ")");
                        }
                    }
                    // Also show option presets
                    java.util.List<String> ids = plugin.workshopManager.getAllPresetIds();
                    if (!ids.isEmpty()) {
                        player.sendMessage(ChatColor.GOLD + "=== 설정 프리셋 (" + ids.size() + "개) ===");
                        for (String id : ids) {
                            player.sendMessage(ChatColor.YELLOW + plugin.workshopManager.getPresetName(id)
                                    + ChatColor.GRAY + " | " + ChatColor.GREEN
                                    + plugin.workshopManager.getPresetAuthorName(id));
                        }
                    }
                }
                else if (args[0].equalsIgnoreCase("workshopdelete")) {
                    if (args.length < 2) {
                        player.sendMessage(ChatColor.RED + "사용법: /prsadmin workshopdelete <프리셋이름>");
                        return true;
                    }
                    String presetId = plugin.workshopManager.findPresetIdByName(args[1]);
                    if (presetId == null) {
                        player.sendMessage(ChatColor.RED + "해당 이름의 프리셋을 찾을 수 없습니다: " + args[1]);
                    } else {
                        plugin.workshopManager.deletePreset(presetId);
                        player.sendMessage(ChatColor.GREEN + "프리셋 '" + args[1] + "' 을 삭제했습니다");
                    }
                }
                else if (args[0].equalsIgnoreCase("workshopunpublish")) {
                    if (args.length < 2) {
                        player.sendMessage(ChatColor.RED + "사용법: /prsadmin workshopunpublish <월드이름>");
                        return true;
                    }
                    if (plugin.workshopManager.unpublishWorld(args[1])) {
                        player.sendMessage(ChatColor.GREEN + "'" + args[1] + "' 의 워크샵 등록을 취소했습니다");
                    } else {
                        player.sendMessage(ChatColor.RED + "해당 월드를 워크샵에서 찾을 수 없습니다: " + args[1]);
                    }
                }
            } catch (Exception e) {
                player.sendMessage(ChatColor.RED + e.getMessage() + "\n" + ChatColor.GREEN + e.getCause());
            }
        }
        return true;
    }
}


