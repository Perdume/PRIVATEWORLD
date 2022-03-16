package prs.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class Inv {
    HashMap<UUID, Boolean> map= new HashMap<>();
    public Boolean getPlayerInventoryOpenned(Player p){
        if (map.get(p.getUniqueId()) != null) {
            return map.get(p.getUniqueId());
        }
        else{
            return false;
        }
    }
    public void setPlayerInventoryOpenned(Player p, Boolean b){
        map.put(p.getUniqueId(), b);
    }
}
