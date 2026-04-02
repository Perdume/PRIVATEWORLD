package prs.main;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import prs.data.ScriptManager;
import prs.data.UserWorldManager;
import prs.data.WorkshopManager;
import prs.privateworld.PrivateWorld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class Chatting implements Listener {
    static HashMap<Player, Boolean> Chooseing = new HashMap<>();
    static HashMap<Player, String> msg = new HashMap<>();
    static HashMap<Player, World> world = new HashMap<>();
    /** Accumulated script lines for the ScriptEdit input mode. */
    static HashMap<Player, List<String>> scriptLines = new HashMap<>();

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
            UserWorldManager worldSettings = new UserWorldManager(world.get(e.getPlayer()));
            String name = ChatColor.translateAlternateColorCodes('&', e.getMessage());
            worldSettings.setWorldName(name);
            e.getPlayer().sendMessage(ChatColor.GREEN + "성공적으로 월드 이름을 설정했습니다");
            Chooseing.put(e.getPlayer(), false);

        } else if (Objects.equals(msgType, "WorkshopSave")) {
            String presetName = e.getMessage().trim();
            if (presetName.isEmpty()) {
                e.getPlayer().sendMessage(ChatColor.RED + "프리셋 이름이 비어있습니다. 다시 입력해주세요.");
                return;
            }
            PrivateWorld plugin = PrivateWorld.getPlugin(PrivateWorld.class);
            plugin.workshopManager.savePreset(e.getPlayer(), presetName, world.get(e.getPlayer()));
            e.getPlayer().sendMessage(ChatColor.GREEN + "프리셋 '"
                    + ChatColor.YELLOW + presetName + ChatColor.GREEN + "' 이 저장되었습니다!");
            Chooseing.put(e.getPlayer(), false);

        } else if (msgType.startsWith("WorkshopPublish:")) {
            // msgType format: "WorkshopPublish:<ContentType name>"
            String typeName = msgType.substring("WorkshopPublish:".length());
            WorkshopManager.ContentType type;
            try {
                type = WorkshopManager.ContentType.valueOf(typeName);
            } catch (IllegalArgumentException ex) {
                type = WorkshopManager.ContentType.OTHER;
            }
            String title = e.getMessage().trim();
            if (title.isEmpty()) {
                e.getPlayer().sendMessage(ChatColor.RED + "제목이 비어있습니다. 다시 입력해주세요.");
                return;
            }
            PrivateWorld plugin = PrivateWorld.getPlugin(PrivateWorld.class);
            World w = world.get(e.getPlayer());
            plugin.workshopManager.publishWorld(e.getPlayer(), w.getName(), type, title);
            e.getPlayer().sendMessage(ChatColor.GREEN + "'"
                    + type.color + title
                    + ChatColor.GREEN + "' 이(가) 워크샵에 등록되었습니다! ["
                    + type.color + type.displayName + ChatColor.GREEN + "]");
            Chooseing.put(e.getPlayer(), false);

        } else if (msgType.startsWith("ScriptEdit:")) {
            // msgType format: "ScriptEdit:<worldName>:<triggerName>"
            String rest = msgType.substring("ScriptEdit:".length());
            int colon = rest.lastIndexOf(':');
            if (colon < 0) {
                Chooseing.put(e.getPlayer(), false);
                return;
            }
            String worldName  = rest.substring(0, colon);
            String triggerName = rest.substring(colon + 1);
            String input = e.getMessage().trim();

            if (input.equalsIgnoreCase("clear")) {
                scriptLines.put(e.getPlayer(), new ArrayList<>());
                e.getPlayer().sendMessage(ChatColor.BLUE + "입력 목록이 초기화되었습니다. 처음부터 다시 입력해주세요.");
                return;
            }

            if (input.equalsIgnoreCase("done")) {
                List<String> lines = scriptLines.getOrDefault(e.getPlayer(), new ArrayList<>());
                PrivateWorld plugin = PrivateWorld.getPlugin(PrivateWorld.class);
                try {
                    ScriptManager.Trigger trigger = ScriptManager.Trigger.valueOf(triggerName);
                    plugin.scriptManager.setActions(worldName, trigger, lines);
                    e.getPlayer().sendMessage(ChatColor.GREEN + "스크립트가 저장되었습니다 ("
                            + lines.size() + "줄)!");
                } catch (IllegalArgumentException ex) {
                    e.getPlayer().sendMessage(ChatColor.RED + "잘못된 트리거 이름입니다: " + triggerName);
                }
                scriptLines.remove(e.getPlayer());
                Chooseing.put(e.getPlayer(), false);
                return;
            }

            // Accumulate the line
            List<String> lines = scriptLines.computeIfAbsent(e.getPlayer(), k -> new ArrayList<>());
            lines.add(input);
            e.getPlayer().sendMessage(ChatColor.DARK_GRAY + "[" + lines.size() + "] "
                    + ChatColor.WHITE + input);
            e.getPlayer().sendMessage(ChatColor.GRAY
                    + "계속 입력하거나 done(저장)/Quit(취소)/clear(초기화)를 입력하세요.");
        }
    }

    public void startChatInput(Player p, String s, World w) {
        Chooseing.put(p, true);
        msg.put(p, s);
        world.put(p, w);
    }
}
