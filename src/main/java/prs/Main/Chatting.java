package prs.Main;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import prs.Data.UserWorldManager;

import java.util.HashMap;
import java.util.Objects;

public class Chatting implements Listener {
    static HashMap<Player, Boolean> Chooseing = new HashMap<>();
    static HashMap<Player, String> msg = new HashMap<>();
    static HashMap<Player, World> world = new HashMap<>();

    @EventHandler
    public void Chating(AsyncPlayerChatEvent e) {
        if (!Boolean.TRUE.equals(Chooseing.get(e.getPlayer()))) return;
        if (!Objects.equals(msg.get(e.getPlayer()), "Name")) return;
        e.setCancelled(true);
        UserWorldManager uwm = new UserWorldManager(world.get(e.getPlayer()));
        if (!e.getMessage().equals("Quit")) {
            String name = ChatColor.translateAlternateColorCodes('&', e.getMessage());
            uwm.SetWorldName(name);
            e.getPlayer().sendMessage(ChatColor.GREEN + "성공적으로 월드 이름을 설정했습니다");
        } else {
            e.getPlayer().sendMessage(ChatColor.RED + "취소했습니다");
        }
        Chooseing.put(e.getPlayer(), false);
    }

    public void Chatset(Player p, String s, World w) {
        Chooseing.put(p, true);
        msg.put(p, s);
        world.put(p, w);
    }
}
