package prs.Rollback;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import prs.privateworld.PrivateWorld;
import prs.world.WorldManage;
import prs.world.WorldManager;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WorldRollBack implements Listener {
    private PrivateWorld wm = PrivateWorld.getPlugin(PrivateWorld.class);
    WorldManager wrm = new WorldManager();
    WorldManage wrma = new WorldManage();
    @EventHandler
    private void RollBackJoined(PlayerJoinEvent e) {
        Path releaseFolder = Paths.get(String.valueOf(e.getPlayer().getUniqueId()));
        Player p = e.getPlayer();
        Boolean it = Files.exists(releaseFolder);
        if (it == true){
            List<String> temp = (List<String>) wm.worldManager.getConfig().getList("PlayerWorlds");
            if (!temp.contains(e.getPlayer().getUniqueId())){
                try {

                    int a = wrm.GetNum(e.getPlayer());
                    Path releaseFolder2 = Paths.get(e.getPlayer().getUniqueId().toString() + "--" + a);
                    wrma.copyWorld(releaseFolder.toFile(), releaseFolder2.toFile());
                    WorldCreator wrm1 = new WorldCreator(e.getPlayer().getUniqueId().toString() + "--" + a);
                    wrm1.generator("VoidGenerator");
                    wrm1.createWorld();
                    if (temp == null || wm.worldManager.getConfig().getList("PlayerWorlds") == null) {
                        ArrayList<String> temp1 = new ArrayList<>(Arrays.asList(p.getUniqueId().toString() + "--" + a));
                        temp = temp1;
                        wm.worldManager.getConfig().set("PlayerWorlds", temp);
                    } else {
                        boolean contains = temp.contains(p.getWorld().getName());
                        if (contains == false) {
                            temp.add(p.getUniqueId().toString() + "--" + a);
                            wm.worldManager.getConfig().set("PlayerWorlds", temp);
                        }
                        wm.worldManager.saveconfig();
                    }

                    try {
                        wrm.worldset(Bukkit.getWorld(p.getUniqueId().toString() + "--" + a));
                    } catch (Exception e1) {
                        long time = System.currentTimeMillis();
                        while (System.currentTimeMillis() - time < 100) {
                        }
                        wrm.worldset(Bukkit.getWorld(p.getUniqueId().toString() + "--" + a));
                    }
                    wrma.deleteWorld(releaseFolder.toFile());
                    wrma.deleteFilesRecursively(releaseFolder.toFile());
                    p.sendMessage(ChatColor.GREEN + "월드 복구에 성공하였습니다");
                }
                catch(Exception e2) {
                    p.sendMessage(ChatColor.RED + "월드를 불러올 수 없습니다! 관리자에게 이 일을 알리십시오");

                }
            }
        }


    }
}
