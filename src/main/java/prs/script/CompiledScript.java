package prs.script;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The parsed, ready-to-run form of a Workshop script.
 *
 * <p>A script is a list of {@link Trigger}s ({@code on <event> ...} blocks).
 * Each trigger owns a body of {@link Statement}s that is executed by
 * {@link ScriptRunner} whenever the matching event happens in a world the
 * script is installed in.</p>
 */
public final class CompiledScript {

    // -------------------------------------------------------------------------
    // Triggers (events)
    // -------------------------------------------------------------------------

    public enum TriggerType {
        JOIN   ("join",   "플레이어가 월드에 들어올 때"),
        LEAVE  ("leave",  "플레이어가 월드를 나갈 때"),
        STEP   ("step",   "블록을 밟을 때"),
        CLICK  ("click",  "블록을 우클릭할 때"),
        FALL   ("fall",   "지정한 Y 아래로 떨어질 때"),
        DEATH  ("death",  "플레이어가 죽을 때"),
        KILL   ("kill",   "다른 플레이어를 처치할 때"),
        CHAT   ("chat",   "특정 채팅을 입력할 때"),
        TIMER  ("timer",  "N초마다 (월드 안 플레이어마다 실행)");

        public final String keyword;
        public final String description;

        TriggerType(String keyword, String description) {
            this.keyword = keyword;
            this.description = description;
        }

        public static TriggerType byKeyword(String s) {
            for (TriggerType t : values()) {
                if (t.keyword.equalsIgnoreCase(s)) return t;
            }
            return null;
        }
    }

    /**
     * One {@code on <event>} block.
     *
     * <p>Only the fields relevant to the trigger type are set:</p>
     * <ul>
     *   <li>{@code material} – STEP / CLICK by block type</li>
     *   <li>{@code hasPosition, x, y, z} – STEP / CLICK at a fixed block</li>
     *   <li>{@code number} – FALL (y level) / TIMER (seconds)</li>
     *   <li>{@code text} – CHAT (message to match, case-insensitive)</li>
     * </ul>
     */
    public static final class Trigger {
        public final TriggerType type;
        public final int line;
        public final Material material;
        public final boolean hasPosition;
        public final int x, y, z;
        public final int number;
        public final String text;
        public final List<Statement> body;

        Trigger(TriggerType type, int line, Material material, boolean hasPosition,
                int x, int y, int z, int number, String text, List<Statement> body) {
            this.type = type;
            this.line = line;
            this.material = material;
            this.hasPosition = hasPosition;
            this.x = x;
            this.y = y;
            this.z = z;
            this.number = number;
            this.text = text;
            this.body = body;
        }

        /** Returns {@code true} when this STEP/CLICK trigger matches the given block. */
        public boolean matchesBlock(Material type, int bx, int by, int bz) {
            if (hasPosition) return x == bx && y == by && z == bz;
            return material == type;
        }
    }

    // -------------------------------------------------------------------------
    // Statements
    // -------------------------------------------------------------------------

    /** A single executable line (action) or an {@code if} block. */
    public interface Statement {
        int line();
    }

    public enum ActionType {
        MESSAGE   ("message",    "\"텍스트\"",                 "나에게 메시지"),
        BROADCAST ("broadcast",  "\"텍스트\"",                 "월드 전체에 메시지"),
        TITLE     ("title",      "\"제목\" [\"부제목\"]",       "화면 중앙 타이틀"),
        ACTIONBAR ("actionbar",  "\"텍스트\"",                 "액션바 메시지"),
        SOUND     ("sound",      "<사운드> [볼륨] [피치]",      "소리 재생"),
        PARTICLE  ("particle",   "<파티클> [개수]",             "파티클 표시"),
        TELEPORT  ("teleport",   "<x> <y> <z> | spawn | checkpoint", "순간이동 (~ 상대좌표 가능)"),
        CHECKPOINT("checkpoint", "",                           "현재 위치를 체크포인트로 저장"),
        RESPAWN   ("respawn",    "",                           "체크포인트(없으면 스폰)로 이동"),
        LOBBY     ("lobby",      "",                           "로비로 보내기"),
        GIVE      ("give",       "<아이템> [개수]",             "아이템 지급"),
        CLEAR     ("clear",      "",                           "인벤토리 비우기"),
        GAMEMODE  ("gamemode",   "survival|adventure|spectator", "게임모드 변경"),
        HEAL      ("heal",       "",                           "체력/배고픔 회복"),
        DAMAGE    ("damage",     "<양>",                       "피해 주기"),
        EFFECT    ("effect",     "<효과> [초] [레벨] | clear",  "포션 효과"),
        SET       ("set",        "<변수> <값>",                 "변수에 값 넣기"),
        ADD       ("add",        "<변수> <값>",                 "변수에 값 더하기"),
        TIMER     ("timer",      "start|stop",                 "스톱워치 ({time})"),
        WAIT      ("wait",       "<초>",                       "잠시 기다리기"),
        STOP      ("stop",       "",                           "이 블록 실행 중단");

        public final String keyword;
        public final String usage;
        public final String description;

        ActionType(String keyword, String usage, String description) {
            this.keyword = keyword;
            this.usage = usage;
            this.description = description;
        }

        public static ActionType byKeyword(String s) {
            for (ActionType t : values()) {
                if (t.keyword.equalsIgnoreCase(s)) return t;
            }
            return null;
        }
    }

    /** An action line, e.g. {@code give diamond 3}. Arguments are validated by the parser. */
    public record Action(int line, ActionType type, List<String> args) implements Statement {
        public String arg(int i) {
            return i < args.size() ? args.get(i) : null;
        }
    }

    /** {@code if <left> <op> <right> ... [else ...] end}. */
    public record If(int line, Operand left, String op, Operand right,
                     List<Statement> then, List<Statement> otherwise) implements Statement {
        public boolean test(double l, double r) {
            return switch (op) {
                case "==", "=" -> Math.abs(l - r) < 1e-9;
                case "!="      -> Math.abs(l - r) >= 1e-9;
                case ">"       -> l > r;
                case "<"       -> l < r;
                case ">="      -> l >= r;
                case "<="      -> l <= r;
                default        -> false;
            };
        }
    }

    /**
     * A value used in conditions and {@code set}/{@code add}: a number literal,
     * a built-in ({@code time}, {@code y}, {@code players}, ...), a per-player
     * variable ({@code coins}) or a world-shared variable ({@code $round}).
     */
    public record Operand(Double literal, String name) {
        public static final List<String> BUILTINS = List.of(
                "time", "x", "y", "z", "health", "food", "players", "random");

        public boolean isLiteral()  { return literal != null; }
        public boolean isBuiltin()  { return name != null && BUILTINS.contains(name); }
        public boolean isGlobal()   { return name != null && name.startsWith("$"); }
    }

    // -------------------------------------------------------------------------
    // Script
    // -------------------------------------------------------------------------

    private final List<Trigger> triggers;

    CompiledScript(List<Trigger> triggers) {
        this.triggers = Collections.unmodifiableList(new ArrayList<>(triggers));
    }

    public List<Trigger> getTriggers() {
        return triggers;
    }

    public List<Trigger> getTriggers(TriggerType type) {
        List<Trigger> result = new ArrayList<>();
        for (Trigger t : triggers) {
            if (t.type == type) result.add(t);
        }
        return result;
    }

    public boolean hasTrigger(TriggerType type) {
        for (Trigger t : triggers) {
            if (t.type == type) return true;
        }
        return false;
    }
}
