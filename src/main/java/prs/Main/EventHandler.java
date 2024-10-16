package prs.Main;

import de.tr7zw.nbtapi.NBTBlock;
import de.tr7zw.nbtapi.NBTEntity;
import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
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
        if (!wm.worldManager.isLobbySet()) return;
        Location loc = wm.worldManager.getLobby();
        e.getPlayer().teleport(loc);
    }
    @org.bukkit.event.EventHandler
    public void Onjoin(PlayerJoinEvent e){
        e.getPlayer().getInventory().clear();
        if (!wm.worldManager.isLobbySet()) return;
        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(wm, new Runnable() {
            @Override
            public void run() {
                Location loc = wm.worldManager.getLobby();
                e.getPlayer().teleport(loc);
            }
        }, 1);
    }
    @org.bukkit.event.EventHandler
    public void OnDrop(PlayerDropItemEvent e){
        UserWorldManager uwm = new UserWorldManager(e.getPlayer().getWorld());
        if (wrm.wcon(e.getPlayer().getWorld()) != null) return;
        if (!uwm.getOption(UserWorldManager.WorldOption.CAN_DROP) && wrm.wcon(e.getPlayer().getWorld()) != e.getPlayer()) {
            e.setCancelled(true);
        }
    }
    @org.bukkit.event.EventHandler
    public void OnBreak(BlockBreakEvent e){
        if (wrm.wcon(e.getPlayer().getWorld()) == null) return;
        UserWorldManager uwm = new UserWorldManager(e.getPlayer().getWorld());
        if (uwm.getOption(UserWorldManager.WorldOption.CAN_BREAK)) return;
        if (wrm.wcon(e.getPlayer().getWorld()) != e.getPlayer()){
            e.setCancelled(true);
        }
    }
    @org.bukkit.event.EventHandler
    public void OnPlace(BlockPlaceEvent e){
        if (wrm.wcon(e.getPlayer().getWorld()) == null) return;
        UserWorldManager uwm = new UserWorldManager(e.getPlayer().getWorld());
        if (wrm.wcon(e.getPlayer().getWorld()) == e.getPlayer()) return;
        if (uwm.getOption(UserWorldManager.WorldOption.CAN_PLACE)) {
            e.setCancelled(true);
        }
    }
    @org.bukkit.event.EventHandler
    public void OnShoot(EntityShootBowEvent e){
        if (!(e.getEntity() instanceof Player p)) return;
        if (wrm.wcon(p.getWorld()) == null) return;
        UserWorldManager uwm = new UserWorldManager(p.getWorld());
        if (wrm.wcon(p.getWorld()) == p) return;
        if (uwm.getOption(UserWorldManager.WorldOption.CAN_SHOOT)) {
            e.setCancelled(true);
        }
    }
    @org.bukkit.event.EventHandler
    public void OnExplode(EntityExplodeEvent e){
        e.setCancelled(true);
    }
    @org.bukkit.event.EventHandler
    public void OnGUIOPEN(InventoryOpenEvent e){
        int reved = 0;
        for (ItemStack i : e.getInventory().getContents()) {
            if(i == null) continue;
            if(i.getType() == Material.AIR) continue;
            String a = new NBTItem(i).toString();
            List temp = Arrays.asList(a.split(""));
            if (temp.size() > 100000) {
                e.getInventory().remove(i);
                reved++;
            }
        }
    }
    @org.bukkit.event.EventHandler
    public void CusInteract(PlayerInteractEvent e) {
        NBTItem nbti = new NBTItem(e.getItem());
        for (String s : nbti.getKeys()) {
            if (Objects.equals(s, "CustomPotionEffects") || Objects.equals(s, "EntityTag") || Objects.equals(s, "BlockEntityTag")) {
                e.setCancelled(true);
                e.getPlayer().sendMessage(ChatColor.RED + "[PREVENTION] 해당 아이템에서 서버나 플레이어에 피해를 끼칠만한 NBT가 발견되었습니다");
                return;
            }
        }
    }

    @org.bukkit.event.EventHandler
    public void Interact(PlayerInteractEvent e){
        if (e.getClickedBlock() == null) return;
        if (!(e.getClickedBlock() instanceof RespawnAnchor Anchor)) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (Anchor.getCharges() == 0) return;
        if (e.getPlayer().getItemInHand().getType().equals(Material.GLOWSTONE) && Anchor.getCharges() != 4) return;
        e.setCancelled(true);
        if (!e.getClickedBlock().getType().toString().contains("SIGN")) return;
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) e.setCancelled(true);
    }

    @org.bukkit.event.EventHandler
    public void BookChecker(PlayerInteractEvent e){
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK && e.getAction() != Action.RIGHT_CLICK_AIR) return;
        if (e.getItem() == null) return;
        if (e.getItem().getType().equals(Material.WRITTEN_BOOK)) {
            ItemStack book = new ItemStack(Material.WRITTEN_BOOK, 1);
            BookMeta bm = (BookMeta) e.getItem().getItemMeta();
            BookMeta bm2 = (BookMeta) book.getItemMeta();
            for (String k : bm.getPages()) {
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
    @org.bukkit.event.EventHandler
    public void OnCommand(PlayerCommandPreprocessEvent e){
        List<String> arr = Arrays.asList(e.getMessage().split(" "));
        List<String> arr2 = new ArrayList<>(Arrays.asList("/privateworld", "/pb", "/hub", "/로비", "/프라이빗월드", "/l", "/코인", "/칭호", "/gamemode"));
        for (String s: arr2){
            if (s.equals(arr.get(0))) return;
        }
        if (e.getPlayer().isOp()) return;
        if (wrm.wcon(e.getPlayer().getWorld()) != null){
            UserWorldManager uwm = new UserWorldManager(e.getPlayer().getWorld());
            if (!uwm.getOption(UserWorldManager.WorldOption.CAN_COMMAND) && wrm.wcon(e.getPlayer().getWorld()) != e.getPlayer()) {
                e.setCancelled(true);
            }
        } else e.setCancelled(true);
    }
    @org.bukkit.event.EventHandler
    public void Redstone(BlockRedstoneEvent e) {
        UserWorldManager uwm = new UserWorldManager(e.getBlock().getWorld());
        if (!uwm.getOption(UserWorldManager.WorldOption.REDSTONE)) {
            e.setNewCurrent(e.getOldCurrent());
        }
    }
    @org.bukkit.event.EventHandler
    public void Redstone(BlockDispenseEvent e) {
        NBTItem nbti = new NBTItem(e.getItem());
        for (String s: nbti.getKeys()){
            if (Objects.equals(s, "CustomPotionEffects") || Objects.equals(s, "EntityTag")){
                e.setCancelled(true);
                return;
            }
        }
    }
    @org.bukkit.event.EventHandler
    public void PvP(EntityDamageByEntityEvent e) {
        UserWorldManager uwm = new UserWorldManager(e.getEntity().getWorld());
        if (uwm.getOption(UserWorldManager.WorldOption.CAN_PVP)) return;
        if (e.getEntity().getType() == EntityType.PLAYER && e.getDamager().getType() == EntityType.PLAYER) e.setCancelled(true);
    }
    @org.bukkit.event.EventHandler
    public void FireWork(FireworkExplodeEvent e) {
        UserWorldManager uwm = new UserWorldManager(e.getEntity().getWorld());
        if (uwm.getOption(UserWorldManager.WorldOption.CAN_SHOW_FIREWORK)) return;
        e.setCancelled(true);
        e.getEntity().remove();
    }
    @org.bukkit.event.EventHandler
    public void piston(BlockPistonExtendEvent e){
        UserWorldManager uwm = new UserWorldManager(e.getBlock().getWorld());
        if (uwm.getOption(UserWorldManager.WorldOption.REDSTONE)) return;
        e.setCancelled(true);
    }
    @org.bukkit.event.EventHandler
    public void piston(BlockPistonRetractEvent e){
        UserWorldManager uwm = new UserWorldManager(e.getBlock().getWorld());
        if (uwm.getOption(UserWorldManager.WorldOption.REDSTONE)) return;
        e.setCancelled(true);
    }
    //* OFFHAND CHANGE *//
    @org.bukkit.event.EventHandler
    public void onSwapHandInv(InventoryClickEvent event) {
        Inventory inv = event.getClickedInventory();
        if (!(inv instanceof PlayerInventory)) return;
        if (event.getSlot() == 40) { // I think off hand slot is 40 --> RLY?
            event.setCancelled(true);
        }
    }

    @org.bukkit.event.EventHandler
    public void onSpawn(EntitySpawnEvent e){
        NBTEntity nbtent = new NBTEntity(e.getEntity());
        for (String s: nbtent.getKeys()) {
            if (Objects.equals(s, "Effects")) {
                e.getEntity().remove();
            }
            if (Objects.equals(s, "Particle")) {
                e.getEntity().remove();
            }
            if (Objects.equals(s, "ActiveEffects")) {
                e.getEntity().remove();
            }
            if (Objects.equals(s, "TileEntityData")) {
                e.getEntity().remove();
            }
        }

    }
}
