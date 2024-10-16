package prs.Main;

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

public class perworldcommand implements Listener {
    private PrivateWorld wm = PrivateWorld.getPlugin(PrivateWorld.class);
    @EventHandler
    public void redstoneChanges(BlockRedstoneEvent e){
        Block block = e.getBlock();

        if(e.getOldCurrent() == 0 && e.getNewCurrent() > 0){
            if (block.getType() == Material.AIR) return;
            BlockState state = block.getState();
            if (!(state instanceof CommandBlock cb)) return;
            if (cb.getCommand().contains("execute") && cb.getCommand().contains("run")){
                e.setNewCurrent(e.getOldCurrent());
            }
        }
    }
}
