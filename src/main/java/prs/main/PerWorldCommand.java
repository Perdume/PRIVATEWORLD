package prs.main;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CommandBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import prs.privateworld.PrivateWorld;
import prs.world.WorldManager;

public class PerWorldCommand implements Listener {
    private PrivateWorld plugin = PrivateWorld.getPlugin(PrivateWorld.class);
    private WorldManager worldMgr = new WorldManager();

    @EventHandler
    public void redstoneChanges(BlockRedstoneEvent e){
        Block block = e.getBlock();

        if(e.getOldCurrent() == 0 && e.getNewCurrent() > 0){
            if (block.getType() == Material.AIR) return;
            BlockState state = block.getState();
            if (!(state instanceof CommandBlock cb)) return;
            if (worldMgr.getWorldOwner(block.getWorld()) == null) return;
            e.setNewCurrent(e.getOldCurrent());
        }
    }
}
