package prs.parkour.Function;

import org.bukkit.entity.Player;

import java.util.HashMap;

public class status {
    private Player p;
    private HashMap<Player,Boolean> playing = new HashMap<>();
    private HashMap<Player, Integer> checkpoint = new HashMap<>();

    public status(Player pl) {
        this.p = pl;
    }

    public void SetPlaying(Boolean b) {
    playing.put(this.p,b);
    }
    public Boolean getPlaying(){
        if (playing.get(this.p) != null) {
            return playing.get(this.p);
        }
        else{
            return false;
        }
    }
    public void CheckPoint(){
        
    }

}
