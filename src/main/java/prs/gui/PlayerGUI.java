package prs.gui;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import prs.Data.UserWorldManager;
import prs.privateworld.PrivateWorld;
import prs.world.WorldBanPlayer;
import prs.world.WorldManager;
import redempt.redlib.inventorygui.InventoryGUI;
import redempt.redlib.inventorygui.ItemButton;
import redempt.redlib.itemutils.ItemBuilder;

import java.util.*;

public class PlayerGUI {
    HashMap<UUID, Integer> page = new HashMap<>();
    private PrivateWorld wm = PrivateWorld.getPlugin(PrivateWorld.class);
    WorldManager wrm = new WorldManager();
    public ItemStack getHead(OfflinePlayer player) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta skull = (SkullMeta) item.getItemMeta();
        skull.setDisplayName(ChatColor.GREEN + player.getName());
        ArrayList<String> lore = new ArrayList<>();
        lore.add(ChatColor.GREEN + "클릭하여 월드를 확인합니다");
        skull.setLore(lore);
        skull.setOwner(player.getName());
        item.setItemMeta(skull);
        return item;
    }
    public void PlayerWorldList(Player p){

        wm.inv.setPlayerInventoryOpenned(p, true);
        wrm.WorldCheck();
        InventoryGUI gui = new InventoryGUI(Bukkit.createInventory(null, 54, "WorldList"));
        List<String> temp = (List<String>) wm.worldManager.getConfig().getList("PlayerWorlds");
        List<OfflinePlayer> temp2 = new ArrayList<>();

        for (String s: temp){
            if (wrm.wcon(Bukkit.getWorld(s)) != null) {
                if (!temp2.contains(wrm.wcon(Bukkit.getWorld(s)))) {
                    temp2.add(wrm.wcon(Bukkit.getWorld(s)));
                }
            }
        }
        int i = 0;
        for(OfflinePlayer s: temp2) {
            if (page.get(p.getUniqueId()) == null){
                page.put(p.getUniqueId(), 1);
            }
            if (i >= (page.get(p.getUniqueId()) - 1) * 45) {
                if (i <= (page.get(p.getUniqueId())) * 45) {

                    ItemButton button = ItemButton.create(getHead(s), e -> {
                        WorldList((Player) e.getWhoClicked(), s);
                    });
                    gui.addButton(button, i);
                    i++;
                }
            }
            //Previous Page
            if (page.get(p.getUniqueId()) - 1 > 0) {
                ItemButton button = ItemButton.create(new ItemBuilder(Material.ARROW)
                        .setName(ChatColor.GREEN + "이전페이지").setLore(ChatColor.GREEN + "이전 페이지로 이동합니다"), e -> {
                    page.put(p.getUniqueId(), page.get(p.getUniqueId()) - 1);
                    PlayerWorldList(p);
                });
                gui.addButton(button, 45);
            }
            //Next Page
            ItemButton button = ItemButton.create(new ItemBuilder(Material.ARROW)
                    .setName(ChatColor.GREEN + "다음페이지").setLore(ChatColor.GREEN + "다음페이지로 이동합니다"), e -> {
                page.put(p.getUniqueId(), page.get(p.getUniqueId()) + 1);
                PlayerWorldList(p);

            });
            gui.addButton(button, 53);
        }
        gui.open(p);
    }
    public void WorldList(Player p, OfflinePlayer p1) {

        wm.inv.setPlayerInventoryOpenned(p, true);
        wrm.WorldCheck();
        InventoryGUI gui = new InventoryGUI(Bukkit.createInventory(null, 9, "WorldList"));
        List<String> temp = (List<String>) wm.worldManager.getConfig().getList("PlayerWorlds");
        int i = 0;
        for (String s : temp) {
            if (page.get(p.getUniqueId()) == null) {
                page.put(p.getUniqueId(), 1);
            }
            if (wrm.wcon(Bukkit.getWorld(s)) == p1) {
                UserWorldManager uwm = new UserWorldManager(Bukkit.getWorld(s));
                ItemButton button;
                if (uwm.getWorldFile().get("Option.Name") != null) {
                    String name = (String) uwm.getWorldFile().get("Option.Name");
                    String a = ChatColor.translateAlternateColorCodes('&', name);
                    button = ItemButton.create(new ItemBuilder(Material.GRASS_BLOCK)
                            .setName(a), e -> {
                        PlayerTeleport(p, Bukkit.getWorld(s));
                    });
                }
                else{
                    button = ItemButton.create(new ItemBuilder(Material.GRASS_BLOCK)
                            .setName(ChatColor.GREEN + wrm.wcon(Bukkit.getWorld(s)).getName() + "의 월드"), e -> {
                        PlayerTeleport(p, Bukkit.getWorld(s));
                    });
                }
                gui.addButton(button, i);
                i++;
            }

        }
        gui.open(p);
    }
    public void PlayerTeleport(Player p, World w){
        UserWorldManager uwm = new UserWorldManager(w);
        WorldBanPlayer wb = new WorldBanPlayer();
        if (uwm.getWorldFile().getBoolean("Option.Private") != true || wrm.wcon(w) == p) {
            if (!wb.IsPlayerBanned(p, w)) {
                if (uwm.getWorldFile().get("Option.TeleportLocation") != null) {
                    p.teleport((Location) uwm.getWorldFile().get("Option.TeleportLocation"));
                    if (uwm.getWorldFile().get("Option.Gamemode") != null) {
                        p.setGameMode((GameMode) uwm.getWorldFile().get("Option.Gamemode"));
                    }
                } else {
                    Location loc2 = new Location(w, 0, 64, 0);
                    p.teleport(loc2);
                    if (uwm.getWorldFile().get("Option.Gamemode") != null) {
                        p.setGameMode((GameMode) uwm.getWorldFile().get("Option.Gamemode"));
                    }
                }
            }
            else{
                p.sendMessage(ChatColor.RED + "해당 월드로 들어갈 수 없습니다");
            }
        }
        else{
            p.sendMessage(ChatColor.RED + "해당 월드는 비공개상태입니다");
        }
    }


}
