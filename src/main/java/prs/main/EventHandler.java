package prs.main;

import de.tr7zw.nbtapi.NBTEntity;
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
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.TimeSkipEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;
import prs.data.UserWorldManager;
import prs.privateworld.PrivateWorld;
import prs.world.WorldManager;

import java.util.*;

public class EventHandler implements Listener {
    private PrivateWorld plugin = PrivateWorld.getPlugin(PrivateWorld.class);
    WorldManager worldMgr = new WorldManager();

    @org.bukkit.event.EventHandler
    public void PlayerJoin(PlayerLoginEvent e) {
        e.getPlayer().getInventory().clear();
        if (!plugin.worldManager.isLobbySet()) return;
        Location loc = plugin.worldManager.getLobby();
        e.getPlayer().teleport(loc);
    }

    @org.bukkit.event.EventHandler
    public void Onjoin(PlayerJoinEvent e) {
        e.getPlayer().getInventory().clear();
        if (!plugin.worldManager.isLobbySet()) return;
        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                Location loc = plugin.worldManager.getLobby();
                e.getPlayer().teleport(loc);
            }
        }, 1);
    }

    @org.bukkit.event.EventHandler
    public void OnDrop(PlayerDropItemEvent e) {
        if (worldMgr.getWorldOwner(e.getPlayer().getWorld()) == null) return;
        UserWorldManager worldSettings = new UserWorldManager(e.getPlayer().getWorld());
        if (worldSettings.getOption(UserWorldManager.WorldOption.CAN_DROP)) return;
        if (!worldMgr.getWorldOwner(e.getPlayer().getWorld()).getUniqueId().equals(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @org.bukkit.event.EventHandler
    public void OnBreak(BlockBreakEvent e) {
        if (worldMgr.getWorldOwner(e.getPlayer().getWorld()) == null) return;
        UserWorldManager worldSettings = new UserWorldManager(e.getPlayer().getWorld());
        if (worldSettings.getOption(UserWorldManager.WorldOption.CAN_BREAK)) return;
        if (!worldMgr.getWorldOwner(e.getPlayer().getWorld()).getUniqueId().equals(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @org.bukkit.event.EventHandler
    public void OnPlace(BlockPlaceEvent e) {
        if (worldMgr.getWorldOwner(e.getPlayer().getWorld()) == null) return;
        UserWorldManager worldSettings = new UserWorldManager(e.getPlayer().getWorld());
        if (worldMgr.getWorldOwner(e.getPlayer().getWorld()).getUniqueId().equals(e.getPlayer().getUniqueId())) return;
        if (!worldSettings.getOption(UserWorldManager.WorldOption.CAN_PLACE)) {
            e.setCancelled(true);
        }
    }

    @org.bukkit.event.EventHandler
    public void OnShoot(EntityShootBowEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (worldMgr.getWorldOwner(p.getWorld()) == null) return;
        UserWorldManager worldSettings = new UserWorldManager(p.getWorld());
        if (worldMgr.getWorldOwner(p.getWorld()).getUniqueId().equals(p.getUniqueId())) return;
        if (!worldSettings.getOption(UserWorldManager.WorldOption.CAN_SHOOT)) {
            e.setCancelled(true);
        }
    }

    @org.bukkit.event.EventHandler
    public void OnExplode(EntityExplodeEvent e) {
        e.setCancelled(true);
    }

    @org.bukkit.event.EventHandler
    public void OnGUIOPEN(InventoryOpenEvent e) {
        int reved = 0;
        for (ItemStack i : e.getInventory().getContents()) {
            if (i == null) continue;
            if (i.getType() == Material.AIR) continue;
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
        if (e.getItem() == null) return;
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
    public void Interact(PlayerInteractEvent e) {
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
    public void BookChecker(PlayerInteractEvent e) {
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
    public void OnCommand(PlayerCommandPreprocessEvent e) {
        List<String> arr = Arrays.asList(e.getMessage().split(" "));
        List<String> arr2 = new ArrayList<>(Arrays.asList(
                "/privateworld", "/pb", "/hub", "/로비",
                "/프라이빗월드", "/pv", "/l", "/코인", "/칭호", "/gamemode"));
        for (String s : arr2) {
            if (s.equals(arr.get(0))) return;
        }
        if (e.getPlayer().isOp()) return;
        if (worldMgr.getWorldOwner(e.getPlayer().getWorld()) != null) {
            UserWorldManager worldSettings = new UserWorldManager(e.getPlayer().getWorld());
            if (!worldSettings.getOption(UserWorldManager.WorldOption.CAN_COMMAND)
                    && !worldMgr.getWorldOwner(e.getPlayer().getWorld()).getUniqueId().equals(e.getPlayer().getUniqueId())) {
                e.setCancelled(true);
            }
        } else e.setCancelled(true);
    }

    @org.bukkit.event.EventHandler
    public void Redstone(BlockRedstoneEvent e) {
        UserWorldManager worldSettings = new UserWorldManager(e.getBlock().getWorld());
        if (!worldSettings.getOption(UserWorldManager.WorldOption.REDSTONE)) {
            e.setNewCurrent(e.getOldCurrent());
        }
    }

    @org.bukkit.event.EventHandler
    public void Redstone(BlockDispenseEvent e) {
        NBTItem nbti = new NBTItem(e.getItem());
        for (String s : nbti.getKeys()) {
            if (Objects.equals(s, "CustomPotionEffects") || Objects.equals(s, "EntityTag")) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @org.bukkit.event.EventHandler
    public void PvP(EntityDamageByEntityEvent e) {
        UserWorldManager worldSettings = new UserWorldManager(e.getEntity().getWorld());
        if (worldSettings.getOption(UserWorldManager.WorldOption.CAN_PVP)) return;
        if (e.getEntity().getType() == EntityType.PLAYER && e.getDamager().getType() == EntityType.PLAYER)
            e.setCancelled(true);
    }

    @org.bukkit.event.EventHandler
    public void FireWork(FireworkExplodeEvent e) {
        UserWorldManager worldSettings = new UserWorldManager(e.getEntity().getWorld());
        if (worldSettings.getOption(UserWorldManager.WorldOption.CAN_SHOW_FIREWORK)) return;
        e.setCancelled(true);
        e.getEntity().remove();
    }

    @org.bukkit.event.EventHandler
    public void piston(BlockPistonExtendEvent e) {
        UserWorldManager worldSettings = new UserWorldManager(e.getBlock().getWorld());
        if (worldSettings.getOption(UserWorldManager.WorldOption.REDSTONE)) return;
        e.setCancelled(true);
    }

    @org.bukkit.event.EventHandler
    public void piston(BlockPistonRetractEvent e) {
        UserWorldManager worldSettings = new UserWorldManager(e.getBlock().getWorld());
        if (worldSettings.getOption(UserWorldManager.WorldOption.REDSTONE)) return;
        e.setCancelled(true);
    }

    @org.bukkit.event.EventHandler
    public void onSwapHandInv(InventoryClickEvent event) {
        Inventory inv = event.getClickedInventory();
        if (!(inv instanceof PlayerInventory)) return;
        if (event.getSlot() == 40) {
            event.setCancelled(true);
        }
    }

    @org.bukkit.event.EventHandler
    public void onSpawn(EntitySpawnEvent e) {
        NBTEntity nbtent = new NBTEntity(e.getEntity());
        for (String s : nbtent.getKeys()) {
            if (Objects.equals(s, "Effects") || Objects.equals(s, "Particle")
                    || Objects.equals(s, "ActiveEffects") || Objects.equals(s, "TileEntityData")) {
                e.getEntity().remove();
            }
        }
    }

    /** Prevent weather changes in worlds where the owner has disabled weather. */
    @org.bukkit.event.EventHandler
    public void onWeatherChange(WeatherChangeEvent e) {
        if (worldMgr.getWorldOwner(e.getWorld()) == null) return;
        UserWorldManager worldSettings = new UserWorldManager(e.getWorld());
        if (!worldSettings.getOption(UserWorldManager.WorldOption.WEATHER) && e.toWeatherState()) {
            e.setCancelled(true);
        }
    }

    /** Freeze time in worlds where TIME_LOCK is enabled. */
    @org.bukkit.event.EventHandler
    public void onTimeSkip(TimeSkipEvent e) {
        if (worldMgr.getWorldOwner(e.getWorld()) == null) return;
        UserWorldManager worldSettings = new UserWorldManager(e.getWorld());
        if (worldSettings.getOption(UserWorldManager.WorldOption.TIME_LOCK)) {
            e.setCancelled(true);
        }
    }
}

