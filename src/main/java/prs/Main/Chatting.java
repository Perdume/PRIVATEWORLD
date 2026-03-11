package prs.Main;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import prs.Data.UserWorldManager;
import prs.privateworld.PrivateWorld;

import java.util.HashMap;
import java.util.Objects;

public class Chatting implements Listener {
    static HashMap<Player, Boolean> Chooseing = new HashMap<>();
    static HashMap<Player, String> msg = new HashMap<>();
    static HashMap<Player, World> world = new HashMap<>();

    @EventHandler
    public void Chating(AsyncPlayerChatEvent e) {
        if (!Chooseing.getOrDefault(e.getPlayer(), false)) return;
        String msgType = msg.get(e.getPlayer());
        if (msgType == null) return;
        e.setCancelled(true);

        // Universal cancel word
        if (e.getMessage().equalsIgnoreCase("Quit")) {
            e.getPlayer().sendMessage(ChatColor.RED + "취소했습니다");
            Chooseing.put(e.getPlayer(), false);
            return;
        }

        if (Objects.equals(msgType, "Name")) {
            UserWorldManager uwm = new UserWorldManager(world.get(e.getPlayer()));
            String name = ChatColor.translateAlternateColorCodes('&', e.getMessage());
            uwm.SetWorldName(name);
            e.getPlayer().sendMessage(ChatColor.GREEN + "성공적으로 월드 이름을 설정했습니다");
            Chooseing.put(e.getPlayer(), false);

        } else if (Objects.equals(msgType, "WorkshopSave")) {
            String presetName = e.getMessage().trim();
            if (presetName.isEmpty()) {
                e.getPlayer().sendMessage(ChatColor.RED + "프리셋 이름이 비어있습니다. 다시 입력해주세요.");
                // Keep Chooseing = true so the player can try again
                return;
            }
            PrivateWorld wm = PrivateWorld.getPlugin(PrivateWorld.class);
            wm.workshopManager.savePreset(e.getPlayer(), presetName, world.get(e.getPlayer()));
            e.getPlayer().sendMessage(ChatColor.GREEN + "프리셋 '"
                    + ChatColor.YELLOW + presetName + ChatColor.GREEN + "' 이 워크샵에 저장되었습니다!");
            Chooseing.put(e.getPlayer(), false);
        }
    }

    public void Chatset(Player p, String s, World w) {
        Chooseing.put(p, true);
        msg.put(p, s);
        world.put(p, w);
    }
}
