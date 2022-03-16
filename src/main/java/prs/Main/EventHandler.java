package prs.Main;

import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import prs.Data.UserWorldManager;
import prs.privateworld.PrivateWorld;
import prs.world.WorldManager;

import java.util.*;

public class EventHandler implements Listener {
    private PrivateWorld wm = PrivateWorld.getPlugin(PrivateWorld.class);
    WorldManager wrm = new WorldManager();
    @org.bukkit.event.EventHandler
    public void PlayerJoin(PlayerLoginEvent e) {
        e.getPlayer().getInventory().clear();
        if (wm.worldManager.getConfig().get("Lobby") != null){
            Location loc = (Location) wm.worldManager.getConfig().get("Lobby");
            e.getPlayer().teleport(loc);
        }
    }
    @org.bukkit.event.EventHandler
    public void Onjoin(PlayerJoinEvent e){
        e.getPlayer().getInventory().clear();
        if (wm.worldManager.getConfig().get("Lobby") != null){
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(wm, new Runnable() {
                @Override
                public void run() {
                    Location loc = (Location) wm.worldManager.getConfig().get("Lobby");
                    e.getPlayer().teleport(loc);
                }
            }, 1);
        }
    }
    @org.bukkit.event.EventHandler
    public void OnDrop(PlayerDropItemEvent e){
        UserWorldManager uwm = new UserWorldManager(e.getPlayer().getWorld());
        if (wrm.wcon(e.getPlayer().getWorld()) != null) {
            if (uwm.getWorldFile().getBoolean("Option.canDrop") != true) {
                e.setCancelled(true);
            }
        }
        if (wrm.wcon(e.getPlayer().getWorld()) == e.getPlayer()){
            e.setCancelled(false);
        }
    }
    @org.bukkit.event.EventHandler
    public void OnBreak(BlockBreakEvent e){
        if (wrm.wcon(e.getPlayer().getWorld()) != null) {
            UserWorldManager uwm = new UserWorldManager(e.getPlayer().getWorld());
            if (uwm.getWorldFile().getBoolean("Option.canBreak") != true) {
                if (wrm.wcon(e.getPlayer().getWorld()) != e.getPlayer()){
                    e.setCancelled(true);
                }
            }
        }
    }
    @org.bukkit.event.EventHandler
    public void OnPlace(BlockPlaceEvent e){
        if (wrm.wcon(e.getPlayer().getWorld()) != null) {

            UserWorldManager uwm = new UserWorldManager(e.getPlayer().getWorld());
            if (uwm.getWorldFile().getBoolean("Option.canPlace") != true) {
                e.setCancelled(true);
            }
        }
        if (wrm.wcon(e.getPlayer().getWorld()) == e.getPlayer()){
            e.setCancelled(false);
        }
    }
    @org.bukkit.event.EventHandler
    public void OnShoot(EntityShootBowEvent e){
        if (e.getEntity().getType() == EntityType.PLAYER){
            Player p = Bukkit.getPlayer(e.getEntity().getUniqueId());
            if (wrm.wcon(p.getWorld()) != null) {
                UserWorldManager uwm = new UserWorldManager(p.getWorld());
                if (uwm.getWorldFile().getBoolean("Option.canShoot") != true) {
                    e.setCancelled(true);
                }
            }
            if (wrm.wcon(p.getWorld()) == p){
                e.setCancelled(false);
            }
        }
    }
    @org.bukkit.event.EventHandler
    public void OnExplode(EntityExplodeEvent e){
        e.setCancelled(true);
    }
    @org.bukkit.event.EventHandler
    public void OnGUIOPEN(InventoryOpenEvent e){
        int reved = 0;
        if (wm.inv.getPlayerInventoryOpenned((Player) e.getPlayer()) != true) {
            for (ItemStack i : e.getInventory().getContents()) {
                try {
                    String a = new NBTItem(i).toString();
                    List temp = Arrays.asList(a.split(""));
                    if (temp.size() > 100000) {
                        e.getInventory().remove(i);
                        reved++;
                    }
                } catch (Exception ex) {

                }
            }
            if (reved != 0) {
                Bukkit.broadcastMessage(ChatColor.RED + "REMOVED " + reved + " ITEMS");
            }
        }
    }
    @org.bukkit.event.EventHandler
    public void Close(InventoryCloseEvent e){
        wm.inv.setPlayerInventoryOpenned((Player) e.getPlayer(),null);
    }
    @org.bukkit.event.EventHandler
    public void Interact(PlayerInteractEvent e){
        NBTItem nbti = new NBTItem(e.getItem());
        Bukkit.broadcastMessage(nbti.toString());
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
            if (nbti.getCompound().toString().contains("EntityTag") && nbti.getCompound().toString().contains("BlockEntityTag") || nbti.getCompound().toString().contains("Amplifier:29b,Duration:200,Id:6b") || nbti.getCompound().toString().contains("Amplifier:125b,Duration:0,Id:6b")){
                e.setCancelled(true);
                e.getPlayer().sendMessage(ChatColor.RED + "[PREVENTION] 해당 아이템에서 서버나 플레이어에 피해를 끼칠만한 NBT가 발견되었습니다");
            }
        }
        if (e.getClickedBlock().getType() != null) {
            if (e.getClickedBlock().getType() == Material.RESPAWN_ANCHOR) {
                RespawnAnchor Anchor = (RespawnAnchor) e.getClickedBlock().getBlockData();
                if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    if (Anchor.getCharges() != 0) {
                        Bukkit.broadcastMessage(String.valueOf(e.getPlayer().getItemInHand().getType()));
                        try {
                            if (!e.getPlayer().getItemInHand().getType().equals(Material.GLOWSTONE)) {
                                e.setCancelled(true);
                            } else {
                                if (Anchor.getCharges() == 4) {
                                    e.setCancelled(true);
                                }
                            }
                        } catch (Exception e1) {
                            e.setCancelled(true);
                        }
                    }
                }
            }
        }
        if (e.getClickedBlock().getType().toString().contains("SIGN")) {
            if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                e.setCancelled(true);
            }
        }
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
            if (e.getItem().getType().equals(Material.WRITTEN_BOOK)){
                ItemStack book = new ItemStack(Material.WRITTEN_BOOK, 1);
                BookMeta bm = (BookMeta) e.getItem().getItemMeta();
                BookMeta bm2 = (BookMeta) book.getItemMeta();
                for (String k: bm.getPages()){
                    if (k != null) {
                        bm2.addPage(k);
                    }
                }
                bm2.setAuthor(bm.getAuthor());
                bm2.setTitle(bm.getTitle());
                book.setItemMeta(bm2);
                e.getPlayer().setItemInHand(book);
            }
        }
    }
    @org.bukkit.event.EventHandler
    public void OnCommand(PlayerCommandPreprocessEvent e){
        List<String> arr = Arrays.asList(e.getMessage().split(" "));
        List<String> arr2 = new ArrayList<>(Arrays.asList("/privateworld", "/pb", "/hub", "/로비", "/프라이빗월드", "/l", "/코인", "/칭호", "/gamemode"));
        for (String s: arr2){
            if (s.equals(arr.get(0))){
                return;
            }
        }
         if (!e.getPlayer().isOp()) {
             if (wrm.wcon(e.getPlayer().getWorld()) != null){
                UserWorldManager uwm = new UserWorldManager(e.getPlayer().getWorld());
                if (uwm.getWorldFile().getBoolean("Option.canCommand") != true) {
                    if (wrm.wcon(e.getPlayer().getWorld()) != e.getPlayer()) {
                        e.setCancelled(true);
                    }
                }
            } else{
                 e.setCancelled(true);
             }
        }
    }
    @org.bukkit.event.EventHandler
    public void Redstone(BlockRedstoneEvent e) {
        UserWorldManager uwm = new UserWorldManager(e.getBlock().getWorld());
        if (uwm.getWorldFile().getBoolean("Option.Redstone") != true) {
            e.setNewCurrent(e.getOldCurrent());
        }
    }
    @org.bukkit.event.EventHandler
    public void Redstone(BlockDispenseEvent e) {
        NBTItem nbti = new NBTItem(e.getItem());
        Bukkit.broadcastMessage(nbti.toString());
        if (nbti.getCompound().toString().contains("EntityTag") || nbti.getCompound().toString().contains("Amplifier:29b,Duration:200,Id:6b") || nbti.getCompound().toString().contains("Amplifier:125b,Duration:200,Id:6b")){
            e.setCancelled(true);
        }
    }
    @org.bukkit.event.EventHandler
    public void PvP(EntityDamageByEntityEvent e) {
        UserWorldManager uwm = new UserWorldManager(e.getEntity().getWorld());
        if (uwm.getWorldFile().getBoolean("Option.canPVP") != true) {
            if (e.getEntity().getType() == EntityType.PLAYER && e.getDamager().getType() == EntityType.PLAYER){
                e.setCancelled(true);

            }
        }
    }
    @org.bukkit.event.EventHandler
    public void FireWork(FireworkExplodeEvent e) {
        UserWorldManager uwm = new UserWorldManager(e.getEntity().getWorld());
        if (uwm.getWorldFile().getBoolean("Option.canshowFirework") != true){
            e.setCancelled(true);
            e.getEntity().remove();
        }
    }
    @org.bukkit.event.EventHandler
    public void piston(BlockPistonExtendEvent e){
        UserWorldManager uwm = new UserWorldManager(e.getBlock().getWorld());
        if (uwm.getWorldFile().getBoolean("Option.Redstone") != true) {
            e.setCancelled(true);
        }
    }
    @org.bukkit.event.EventHandler
    public void piston(BlockPistonRetractEvent e){
        UserWorldManager uwm = new UserWorldManager(e.getBlock().getWorld());
        if (uwm.getWorldFile().getBoolean("Option.Redstone") != true) {
            e.setCancelled(true);
        }
    }
    @org.bukkit.event.EventHandler
    public void spawning(CreatureSpawnEvent e){
        String a= String.valueOf(e.getSpawnReason());
        Bukkit.broadcastMessage(a);
    }
    //* OFFHAND CHANGE *//
    @org.bukkit.event.EventHandler
    public void onSwapHandInv(InventoryClickEvent event) {
        Inventory inv = event.getClickedInventory();

        if (!(inv instanceof PlayerInventory)) {
            return;
        }

        if (event.getSlot() == 40) { // I think off hand slot is 40
            event.setCancelled(true);
        }
    }
}
