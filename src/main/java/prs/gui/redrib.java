package prs.gui;

import org.bukkit.*;
import org.bukkit.entity.Player;
import prs.privateworld.PrivateWorld;
import prs.world.WorldManager;
import redempt.redlib.inventorygui.InventoryGUI;
import redempt.redlib.inventorygui.ItemButton;
import redempt.redlib.itemutils.ItemBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class redrib {
    HashMap<UUID, Integer> page = new HashMap<UUID, Integer>();
    private PrivateWorld wm = PrivateWorld.getPlugin(PrivateWorld.class);
    WorldManager wrm = new WorldManager();
    option option = new option();
    public void CreatePlayerWorld(Player p){
        if (wrm.PlayerHasWorld(p) < 9) {
            wrm.WorldCheck();
            wrm.Createworld(p);
            try {
                int loa = wrm.map1.get(p);
                World w = Bukkit.getWorld(p.getUniqueId() + "--" + loa);
                Location loc = new Location(w, 0, 64, 0);
                Location loc2 = new Location(w, 0, 63, 0);
                loc2.getBlock().setType(Material.BEDROCK);
                p.teleport(loc);
            } catch (Exception e) {
                Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(wm, new Runnable() {
                    @Override
                    public void run() {
                        int loa = wrm.map1.get(p);

                        World w = Bukkit.getWorld(p.getUniqueId() + "--" + loa);
                        Location loc = new Location(w, 0, 64, 0);
                        Location loc2 = new Location(w, 0, 63, 0);
                        loc2.getBlock().setType(Material.BEDROCK);
                        p.teleport(loc);
                    }
                }, 20);
            }
        }
        else{
            p.sendMessage(ChatColor.RED + "9개 이상 만들 수 없습니다");
        }
    }
    public void PlayerWorldList(Player p){

        wm.inv.setPlayerInventoryOpenned(p, true);
        wrm.WorldCheck();
        InventoryGUI gui = new InventoryGUI(Bukkit.createInventory(null, 54, "WorldList"));
        List<String> temp = (List<String>) wm.worldManager.getConfig().getList("PlayerWorlds");
        int i = 0;
        for(String s: temp) {
            if (page.get(p.getUniqueId()) == null){
                page.put(p.getUniqueId(), 1);
            }
            if (i >= (page.get(p.getUniqueId()) - 1) * 45) {
                if (i <= (page.get(p.getUniqueId())) * 45) {
                    if (wrm.wcon(Bukkit.getWorld(s)) != null) {
                        ItemButton button = ItemButton.create(new ItemBuilder(Material.GRASS_BLOCK)
                                .setName(s).setLore(ChatColor.GREEN + "Owner: " + wrm.wcon(Bukkit.getWorld(s)).getName()), e -> {
                            WorldSetting(p, Bukkit.getWorld(s));

                        });
                        gui.addButton(button, i);
                        i++;
                    }
                }
            }
            //Previous Page
            if (page.get(p.getUniqueId()) - 1 > 0) {
                ItemButton button = ItemButton.create(new ItemBuilder(Material.ARROW)
                        .setName(ChatColor.GREEN + "previous").setLore(ChatColor.GREEN + "Go previous Page"), e -> {
                    page.put(p.getUniqueId(), page.get(p.getUniqueId()) - 1);
                    PlayerWorldList(p);
                });
                gui.addButton(button, 45);
            }
            //Next Page
            ItemButton button = ItemButton.create(new ItemBuilder(Material.ARROW)
                    .setName(ChatColor.GREEN + "Next").setLore(ChatColor.GREEN + "Next"), e -> {
                page.put(p.getUniqueId(), page.get(p.getUniqueId()) + 1);
                PlayerWorldList(p);
            });
            gui.addButton(button, 53);
        }
        gui.open(p);
    }
    public void WorldSetting(Player p, World w){

        wm.inv.setPlayerInventoryOpenned(p, true);
        InventoryGUI gui = new InventoryGUI(Bukkit.createInventory(null, 9, "WorldSet"));
        ItemButton button = ItemButton.create(new ItemBuilder(Material.BARRIER)
                .setName(ChatColor.RED + "DELETE"), e -> {
            wrm.Deleteworld(w.getName());
            PlayerWorldList(p);

        });
        gui.addButton(button, 0);
        ItemButton button2 = ItemButton.create(new ItemBuilder(Material.BONE)
                .setName(ChatColor.RED + "SET WORLD SETTING"), e -> {
            option.WorldOption(p, w.getName());

        });
        gui.addButton(button2, 1);
        gui.open(p);
    }
}
