package prs.Main;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import prs.Data.UserWorldManager;

import java.util.HashMap;
import java.util.Objects;

public class Chatting implements Listener {
    static HashMap<Player,Boolean> Chooseing = new HashMap<>();
    static HashMap<Player,String> msg = new HashMap<>();
    static HashMap<Player,World> world = new HashMap<>();
    @EventHandler
    public void Chating(PlayerChatEvent e){
        try {
            if (!Chooseing.get(e.getPlayer())) return;
            UserWorldManager uwm = new UserWorldManager(world.get(e.getPlayer()));
            if (!Objects.equals(msg.get(e.getPlayer()), "Name")) return;
            if (!e.getMessage().equals("Quit")) {
                uwm.SetWorldName(e.getMessage());
                ChatColor.translateAlternateColorCodes('&', e.getMessage());
                e.getPlayer().sendMessage(ChatColor.GREEN + "성공적으로 월드 이름을 설정했습니다");
                Chooseing.put(e.getPlayer(), null);
                e.setMessage(null);
            } else {
                e.getPlayer().sendMessage(ChatColor.RED + "취소했습니다");
                Chooseing.put(e.getPlayer(), null);
                e.setMessage(null);
            }
        }
        catch (Exception el) {

        }
    }
    public void Chatset(Player p, String s, World w){
        Chooseing.put(p, true);
        msg.put(p, s);
        world.put(p, w);
    }
}
