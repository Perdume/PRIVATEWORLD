package prs.script;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.WritableBookMeta;
import org.bukkit.persistence.PersistentDataType;
import prs.privateworld.PrivateWorld;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Book helpers for Workshop scripts.
 *
 * <ul>
 *   <li><b>Edit book</b> – a Book &amp; Quill tagged with the script ID. Pressing
 *       "Done" saves the code (see {@code ScriptListener#onEditBook}).</li>
 *   <li><b>View book</b> – a signed, read-only copy of a script's code.</li>
 *   <li><b>Guide book</b> – the syntax reference.</li>
 * </ul>
 *
 * Every page boundary counts as a line break.
 */
public final class ScriptBooks {

    /** Visible book width in pixels and a conservative number of visible lines per page. */
    private static final int PAGE_WIDTH_PX = 114;
    private static final int PAGE_LINES    = 13;
    private static final int PAGE_CHARS    = 900;

    private ScriptBooks() {}

    private static NamespacedKey idKey() {
        return new NamespacedKey(PrivateWorld.getPlugin(PrivateWorld.class), "script_id");
    }

    // -------------------------------------------------------------------------
    // Edit book
    // -------------------------------------------------------------------------

    public static ItemStack editBook(String id, String name, String source) {
        ItemStack book = new ItemStack(Material.WRITABLE_BOOK);
        WritableBookMeta meta = (WritableBookMeta) book.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "[스크립트] " + ChatColor.WHITE + name);
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "코드를 쓰고 '완료'를 누르면 저장됩니다",
                ChatColor.GRAY + "페이지가 바뀌면 줄도 바뀝니다",
                ChatColor.DARK_GRAY + "ID: " + id));
        meta.setPages(toPages(source));
        tag(meta, id);
        book.setItemMeta(meta);
        return book;
    }

    public static void tag(ItemMeta meta, String id) {
        meta.getPersistentDataContainer().set(idKey(), PersistentDataType.STRING, id);
    }

    /** Returns the script ID stored on a book, or {@code null} for ordinary items. */
    public static String getScriptId(ItemMeta meta) {
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(idKey(), PersistentDataType.STRING);
    }

    /** Joins the pages of a book back into source code. */
    public static String readSource(WritableBookMeta meta) {
        List<String> lines = new ArrayList<>();
        for (String page : meta.getPages()) {
            lines.addAll(Arrays.asList(ChatColor.stripColor(page).replace("\r", "").split("\n", -1)));
        }
        while (!lines.isEmpty() && lines.get(lines.size() - 1).trim().isEmpty()) {
            lines.remove(lines.size() - 1);
        }
        return String.join("\n", lines);
    }

    /** Removes every edit book for {@code id} from the player's inventory (prevents stale copies). */
    public static void removeEditBooks(Player p, String id) {
        PlayerInventory inv = p.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() != Material.WRITABLE_BOOK) continue;
            if (id.equals(getScriptId(item.getItemMeta()))) inv.setItem(i, null);
        }
    }

    // -------------------------------------------------------------------------
    // Read-only books
    // -------------------------------------------------------------------------

    public static ItemStack viewBook(String name, String author, String source) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        String title = ChatColor.stripColor(name);
        meta.setTitle(title.length() > 32 ? title.substring(0, 32) : title);
        meta.setAuthor(author);
        meta.setPages(toPages(source));
        book.setItemMeta(meta);
        return book;
    }

    public static ItemStack guideBook() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setTitle("스크립트 문법 가이드");
        meta.setAuthor("PrivateWorld");
        meta.setPages(GUIDE);
        book.setItemMeta(meta);
        return book;
    }

    // -------------------------------------------------------------------------
    // Page layout
    // -------------------------------------------------------------------------

    /** Splits source into pages without breaking lines, keeping each page within the visible area. */
    public static List<String> toPages(String source) {
        List<String> pages = new ArrayList<>();
        StringBuilder page = new StringBuilder();
        int used = 0;
        for (String line : source.split("\n", -1)) {
            int rows = visualRows(line);
            boolean full = used + rows > PAGE_LINES || page.length() + line.length() + 1 > PAGE_CHARS;
            if (used > 0 && full) {
                pages.add(page.toString());
                page.setLength(0);
                used = 0;
            }
            if (used > 0) page.append('\n');
            page.append(line.length() > PAGE_CHARS ? line.substring(0, PAGE_CHARS) : line);
            used += rows;
        }
        if (page.length() > 0 || pages.isEmpty()) pages.add(page.toString());
        return pages;
    }

    /** Rough number of wrapped rows a line takes in a book (Minecraft font ≈ 6px ASCII, ≈ 9px Hangul). */
    private static int visualRows(String line) {
        int px = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            px += c == ' ' ? 4 : (c < 128 ? 6 : 9);
        }
        return Math.max(1, (px + PAGE_WIDTH_PX - 1) / PAGE_WIDTH_PX);
    }

    private static final List<String> GUIDE = Arrays.asList(
            "§l§1워크샵 스크립트§r\n\n"
                    + "책에 코드를 쓰고 §2'완료'§0를 누르면 저장돼요.\n\n"
                    + "§l구조§r\n"
                    + "on <이벤트>\n"
                    + "  <명령어>\n"
                    + "  <명령어>\n\n"
                    + "§8# 뒤는 주석\n들여쓰기는 자유",
            "§l이벤트§r\n"
                    + "on join §8입장§0\n"
                    + "on leave §8퇴장§0\n"
                    + "on step <블록>\n"
                    + "on step at x y z\n"
                    + "on click <블록>\n"
                    + "on fall <y> §8추락§0\n"
                    + "on death §8사망§0\n"
                    + "on kill §8처치§0\n"
                    + "on chat \"말\"\n"
                    + "on timer <초>",
            "§l메시지§r\n"
                    + "message \"글\"\n"
                    + "broadcast \"글\"\n"
                    + "title \"제목\" \"부제\"\n"
                    + "actionbar \"글\"\n"
                    + "sound <이름>\n"
                    + "particle <이름> [개수]\n\n"
                    + "§8&a 같은 색 코드 사용 가능",
            "§l이동§r\n"
                    + "teleport x y z\n"
                    + "teleport ~ ~5 ~\n"
                    + "teleport spawn\n"
                    + "checkpoint\n"
                    + "respawn\n"
                    + "lobby\n\n"
                    + "§l플레이어§r\n"
                    + "give <아이템> [개수]\n"
                    + "clear / heal\n"
                    + "damage <양>",
            "gamemode adventure\n"
                    + "effect speed 10 2\n"
                    + "effect clear\n\n"
                    + "§l흐름§r\n"
                    + "wait <초>\n"
                    + "stop §8남은 줄 생략§0\n"
                    + "timer start\n"
                    + "timer stop",
            "§l변수§r\n"
                    + "set coins 0\n"
                    + "add coins 1\n"
                    + "§8$로 시작하면 월드 공용§0\n"
                    + "set $round 1\n\n"
                    + "§l조건§r\n"
                    + "if coins >= 10\n"
                    + "  ...\n"
                    + "else\n"
                    + "  ...\n"
                    + "end",
            "§l내장 값§r\n"
                    + "time §8스톱워치(초)§0\n"
                    + "x y z health food\n"
                    + "players §8인원 수§0\n"
                    + "random §80~100§0\n\n"
                    + "§l글자 치환§r\n"
                    + "{player} {world}\n"
                    + "{time} {coins}\n"
                    + "{$round}",
            "§l예제: 파쿠르§r\n"
                    + "on join\n"
                    + "  timer start\n"
                    + "on fall 0\n"
                    + "  respawn\n"
                    + "on step gold_block\n"
                    + "  checkpoint\n"
                    + "on step diamond_block\n"
                    + "  timer stop\n"
                    + "  broadcast \"{time}초!\"",
            "§l팁§r\n"
                    + "- 한 줄이 다음 페이지로 넘어가지 않게 쓰세요\n"
                    + "- 내 월드에 설치하면 저장 즉시 반영\n"
                    + "- 다른 사람 월드에는 '공개' 버전이 실행\n"
                    + "- 오류는 저장할 때 줄 번호로 알려줘요");
}
