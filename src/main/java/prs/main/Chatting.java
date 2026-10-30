package prs.main;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import prs.data.UserWorldManager;
import prs.data.WorkshopManager;
import prs.privateworld.PrivateWorld;
import prs.script.ScriptBooks;

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

        } else if (Objects.equals(msgType, "ScriptCreate")) {
            String name = ChatColor.stripColor(e.getMessage().trim());
            if (name.isEmpty() || name.length() > 32) {
                e.getPlayer().sendMessage(ChatColor.RED + "이름은 1~32자로 입력해주세요.");
                return;
            }
            Chooseing.put(e.getPlayer(), false);
            Player p = e.getPlayer();
            PrivateWorld plugin = PrivateWorld.getPlugin(PrivateWorld.class);
            // Chat is async: touch data and inventories on the main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                int max = plugin.configManager.getMaxScriptsPerPlayer();
                if (plugin.scriptManager.getScriptsByAuthor(p.getUniqueId()).size() >= max) {
                    p.sendMessage(ChatColor.RED + "스크립트는 최대 " + max + "개까지 만들 수 있습니다");
                    return;
                }
                String id = plugin.scriptManager.createScript(p, name);
                p.sendMessage(ChatColor.GREEN + "스크립트 '" + ChatColor.YELLOW + name + ChatColor.GREEN + "' 를 만들었습니다!");
                if (p.getInventory().addItem(ScriptBooks.editBook(id, name, plugin.scriptManager.getSource(id))).isEmpty()) {
                    p.sendMessage(ChatColor.GREEN + "받은 책과 깃펜에 코드를 쓰고 '완료'를 누르면 저장됩니다.");
                } else {
                    p.sendMessage(ChatColor.RED + "인벤토리가 가득 차서 책을 받지 못했습니다. /pv script 에서 '코드 편집'을 눌러주세요.");
                }
                p.sendMessage(ChatColor.GRAY + "예제 코드가 들어 있습니다. 문법은 /pv script guide 에서 볼 수 있습니다");
            });

        } else if (msgType.startsWith("ScriptRename:")) {
            String id = msgType.substring("ScriptRename:".length());
            String name = ChatColor.stripColor(e.getMessage().trim());
            if (name.isEmpty() || name.length() > 32) {
                e.getPlayer().sendMessage(ChatColor.RED + "이름은 1~32자로 입력해주세요.");
                return;
            }
            Chooseing.put(e.getPlayer(), false);
            Player p = e.getPlayer();
            PrivateWorld plugin = PrivateWorld.getPlugin(PrivateWorld.class);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!plugin.scriptManager.exists(id) || !plugin.scriptManager.isAuthor(id, p)) {
                    p.sendMessage(ChatColor.RED + "스크립트를 찾을 수 없습니다");
                    return;
                }
                plugin.scriptManager.rename(id, name);
                p.sendMessage(ChatColor.GREEN + "스크립트 이름을 '" + ChatColor.YELLOW + name + ChatColor.GREEN + "' 로 바꿨습니다");
            });
        }
    }

    public void startChatInput(Player p, String s, World w) {
        Chooseing.put(p, true);
        msg.put(p, s);
        world.put(p, w);
    }
}
