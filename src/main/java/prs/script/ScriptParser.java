package prs.script;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffectType;
import prs.script.CompiledScript.Action;
import prs.script.CompiledScript.ActionType;
import prs.script.CompiledScript.If;
import prs.script.CompiledScript.Operand;
import prs.script.CompiledScript.Statement;
import prs.script.CompiledScript.Trigger;
import prs.script.CompiledScript.TriggerType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Workshop script source code into a {@link CompiledScript}.
 *
 * <h3>Syntax overview</h3>
 * <pre>
 * # comment
 * on join
 *   message "&amp;a{player}님 환영합니다!"
 *   timer start
 *
 * on step gold_block
 *   checkpoint
 *   if time &gt; 60
 *     message "느려요!"
 *   else
 *     message "빨라요!"
 *   end
 *
 * on fall 0
 *   respawn
 * </pre>
 *
 * <ul>
 *   <li>One statement per line. Indentation is optional and ignored.</li>
 *   <li>An {@code on} block ends at the next {@code on} line (or {@code end}).</li>
 *   <li>{@code if} blocks must be closed with {@code end}; {@code else} and
 *       {@code else if} are supported.</li>
 *   <li>Trailing colons ({@code on join:}) are allowed.</li>
 * </ul>
 *
 * Parsing never throws; every problem is reported as a {@link ScriptError}
 * with a 1-based line number so players can fix their code.
 */
public final class ScriptParser {

    public static final int MAX_TEXT_LENGTH  = 256;
    public static final double MAX_WAIT_SECONDS = 60;
    private static final int MAX_ERRORS = 20;

    /** Items a script may never hand out. */
    private static final Set<String> DENIED_ITEMS = new HashSet<>(Arrays.asList(
            "COMMAND_BLOCK", "CHAIN_COMMAND_BLOCK", "REPEATING_COMMAND_BLOCK", "COMMAND_BLOCK_MINECART",
            "STRUCTURE_BLOCK", "STRUCTURE_VOID", "JIGSAW", "BARRIER", "LIGHT", "DEBUG_STICK",
            "KNOWLEDGE_BOOK", "SPAWNER", "TRIAL_SPAWNER", "VAULT", "WRITTEN_BOOK", "WRITABLE_BOOK",
            "TNT", "TNT_MINECART", "END_CRYSTAL", "BUNDLE"));

    private static final Pattern CONDITION = Pattern.compile("^(.+?)\\s*(==|!=|>=|<=|=|>|<)\\s*(.+)$");
    private static final Pattern IDENTIFIER = Pattern.compile("^\\$?[\\p{L}_][\\p{L}\\p{N}_]{0,31}$");
    private static final Pattern SOUND_KEY = Pattern.compile("^[a-z0-9_.\\-/]+(:[a-z0-9_.\\-/]+)?$");

    private ScriptParser() {}

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public record ScriptError(int line, String message) {
        @Override
        public String toString() {
            return line > 0 ? line + "번째 줄: " + message : message;
        }
    }

    public record Result(CompiledScript script, List<ScriptError> errors) {
        public boolean ok() {
            return script != null;
        }
    }

    public static Result parse(String source) {
        List<ScriptError> errors = new ArrayList<>();
        List<Line> lines = new ArrayList<>();
        String[] raw = (source == null ? "" : source).replace("\r", "").split("\n", -1);
        for (int i = 0; i < raw.length; i++) {
            List<Token> tokens = tokenize(raw[i], i + 1, errors);
            if (!tokens.isEmpty()) lines.add(new Line(i + 1, normalize(tokens)));
        }

        Parser parser = new Parser(lines, errors);
        List<Trigger> triggers = parser.parseScript();
        if (triggers.isEmpty() && errors.isEmpty()) {
            errors.add(new ScriptError(0, "'on <이벤트>' 블록이 하나도 없습니다"));
        }
        errors.sort(Comparator.comparingInt(ScriptError::line));
        List<ScriptError> shown = errors.size() > MAX_ERRORS ? errors.subList(0, MAX_ERRORS) : errors;
        return new Result(errors.isEmpty() ? new CompiledScript(triggers) : null, new ArrayList<>(shown));
    }

    // -------------------------------------------------------------------------
    // Lexing
    // -------------------------------------------------------------------------

    private record Token(String text, boolean quoted) {
        String lower() {
            return text.toLowerCase(Locale.ROOT);
        }
    }

    private record Line(int no, List<Token> tokens) {
        String keyword() {
            Token first = tokens.get(0);
            return first.quoted() ? "" : first.lower();
        }

        List<Token> args() {
            return tokens.subList(1, tokens.size());
        }
    }

    /** Splits a line into whitespace-separated tokens, honouring "quoted strings" and # comments. */
    private static List<Token> tokenize(String line, int no, List<ScriptError> errors) {
        List<Token> tokens = new ArrayList<>();
        StringBuilder cur = null;
        boolean inQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuote) {
                if (c == '\\' && i + 1 < line.length()) {
                    cur.append(line.charAt(++i));
                } else if (c == '"') {
                    tokens.add(new Token(cur.toString(), true));
                    cur = null;
                    inQuote = false;
                } else {
                    cur.append(c);
                }
                continue;
            }
            if (c == '#') break;
            if (Character.isWhitespace(c)) {
                if (cur != null) tokens.add(new Token(cur.toString(), false));
                cur = null;
            } else if (c == '"') {
                if (cur != null) tokens.add(new Token(cur.toString(), false));
                cur = new StringBuilder();
                inQuote = true;
            } else {
                if (cur == null) cur = new StringBuilder();
                cur.append(c);
            }
        }
        if (inQuote) {
            errors.add(new ScriptError(no, "따옴표(\")가 닫히지 않았습니다"));
            tokens.add(new Token(cur.toString(), true));
        } else if (cur != null) {
            tokens.add(new Token(cur.toString(), false));
        }
        return tokens;
    }

    /** Removes the optional trailing ':' of {@code on}/{@code if}/{@code else} lines. */
    private static List<Token> normalize(List<Token> tokens) {
        List<Token> result = new ArrayList<>(tokens);
        Token first = result.get(0);
        if (!first.quoted() && first.text().endsWith(":") && first.text().length() > 1) {
            result.set(0, new Token(first.text().substring(0, first.text().length() - 1), false));
        }
        String kw = result.get(0).quoted() ? "" : result.get(0).lower();
        if (!kw.equals("on") && !kw.equals("if") && !kw.equals("else")) return result;

        int last = result.size() - 1;
        Token tail = result.get(last);
        if (last > 0 && !tail.quoted() && tail.text().endsWith(":")) {
            String stripped = tail.text().substring(0, tail.text().length() - 1);
            if (stripped.isEmpty()) result.remove(last);
            else result.set(last, new Token(stripped, false));
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Parsing
    // -------------------------------------------------------------------------

    private static final class Parser {
        private final List<Line> lines;
        private final List<ScriptError> errors;
        private int pos = 0;

        Parser(List<Line> lines, List<ScriptError> errors) {
            this.lines = lines;
            this.errors = errors;
        }

        private void error(int line, String msg) {
            errors.add(new ScriptError(line, msg));
        }

        private String peekKeyword() {
            return pos < lines.size() ? lines.get(pos).keyword() : null;
        }

        List<Trigger> parseScript() {
            List<Trigger> triggers = new ArrayList<>();
            while (pos < lines.size()) {
                Line line = lines.get(pos);
                if (line.keyword().equals("on")) {
                    Trigger t = parseTrigger(line);
                    if (t != null) triggers.add(t);
                } else {
                    if (TriggerType.byKeyword(line.keyword()) != null) {
                        error(line.no(), "이벤트는 'on " + line.keyword() + "' 처럼 on 을 붙여서 시작해야 합니다");
                    } else {
                        error(line.no(), "모든 코드는 'on <이벤트>' 블록 안에 있어야 합니다");
                    }
                    pos++;
                }
            }
            return triggers;
        }

        // ---- on <event> ------------------------------------------------------

        private Trigger parseTrigger(Line line) {
            pos++;
            int errorsBefore = errors.size();
            List<Token> args = line.args();

            TriggerType type = null;
            Material material = null;
            boolean hasPos = false;
            int x = 0, y = 0, z = 0, number = 0;
            String text = null;

            if (args.isEmpty()) {
                error(line.no(), "on 뒤에 이벤트 이름이 필요합니다 (" + triggerKeywords() + ")");
            } else {
                type = TriggerType.byKeyword(args.get(0).text());
                List<Token> rest = args.subList(1, args.size());
                if (type == null) {
                    error(line.no(), "알 수 없는 이벤트: " + args.get(0).text()
                            + suggest(args.get(0).lower(), triggerKeywordList())
                            + " (사용 가능: " + triggerKeywords() + ")");
                } else switch (type) {
                    case JOIN, LEAVE, DEATH, KILL -> {
                        if (!rest.isEmpty()) error(line.no(), "'on " + type.keyword + "' 뒤에는 아무것도 붙이지 않습니다");
                    }
                    case STEP, CLICK -> {
                        List<Token> coords = rest;
                        if (!rest.isEmpty() && rest.get(0).lower().equals("at")) coords = rest.subList(1, rest.size());
                        if (coords.size() == 3) {
                            Integer ix = parseInt(coords.get(0).text());
                            Integer iy = parseInt(coords.get(1).text());
                            Integer iz = parseInt(coords.get(2).text());
                            if (ix == null || iy == null || iz == null) {
                                error(line.no(), "좌표는 정수여야 합니다: on " + type.keyword + " at <x> <y> <z>");
                            } else {
                                hasPos = true;
                                x = ix; y = iy; z = iz;
                            }
                        } else if (rest.size() == 1) {
                            material = Material.matchMaterial(rest.get(0).text());
                            if (material == null || !material.isBlock() || material.isAir()) {
                                error(line.no(), "알 수 없는 블록: " + rest.get(0).text());
                            }
                        } else {
                            error(line.no(), "사용법: on " + type.keyword + " <블록>  또는  on " + type.keyword + " at <x> <y> <z>");
                        }
                    }
                    case FALL -> {
                        if (rest.size() > 1) {
                            error(line.no(), "사용법: on fall <y>");
                        } else if (rest.size() == 1) {
                            Integer iy = parseInt(rest.get(0).text());
                            if (iy == null || iy < -2048 || iy > 2048) error(line.no(), "Y 좌표는 -2048 ~ 2048 사이 정수여야 합니다");
                            else number = iy;
                        }
                    }
                    case CHAT -> {
                        text = joinText(rest);
                        if (text.isEmpty()) error(line.no(), "사용법: on chat \"<채팅 내용>\"");
                    }
                    case TIMER -> {
                        Integer sec = rest.size() == 1 ? parseInt(rest.get(0).text()) : null;
                        if (sec == null || sec < 1 || sec > 3600) error(line.no(), "사용법: on timer <초> (1 ~ 3600)");
                        else number = sec;
                    }
                }
            }

            List<Statement> body = parseBlock(false, line.no());
            if ("end".equals(peekKeyword())) pos++;
            if (type == null || errors.size() > errorsBefore) return null;
            return new Trigger(type, line.no(), material, hasPos, x, y, z, number, text, body);
        }

        // ---- blocks ----------------------------------------------------------

        private List<Statement> parseBlock(boolean inIf, int openLine) {
            List<Statement> body = new ArrayList<>();
            while (pos < lines.size()) {
                Line line = lines.get(pos);
                switch (line.keyword()) {
                    case "on", "end" -> {
                        return body;
                    }
                    case "else" -> {
                        if (inIf) return body;
                        error(line.no(), "else 앞에 짝이 되는 if 가 없습니다");
                        pos++;
                    }
                    case "if" -> {
                        If stmt = parseIf(line, line.args());
                        if (stmt != null) body.add(stmt);
                    }
                    default -> {
                        pos++;
                        Statement stmt = parseAction(line);
                        if (stmt != null) body.add(stmt);
                    }
                }
            }
            return body;
        }

        /**
         * Parses an {@code if} whose condition tokens are {@code condTokens}.
         * Handles {@code else}, {@code else if ...} chains and the closing {@code end}.
         */
        private If parseIf(Line line, List<Token> condTokens) {
            pos++;
            int errorsBefore = errors.size();
            Operand left = null, right = null;
            String op = null;

            Matcher m = CONDITION.matcher(joinRaw(condTokens));
            if (!m.matches()) {
                error(line.no(), "조건식이 올바르지 않습니다. 예: if coins >= 10");
            } else {
                left  = parseOperand(m.group(1).trim(), line.no());
                op    = m.group(2);
                right = parseOperand(m.group(3).trim(), line.no());
            }

            List<Statement> then = parseBlock(true, line.no());
            List<Statement> otherwise = new ArrayList<>();
            boolean chained = false;

            if ("else".equals(peekKeyword())) {
                Line elseLine = lines.get(pos);
                List<Token> elseArgs = elseLine.args();
                if (!elseArgs.isEmpty() && elseArgs.get(0).lower().equals("if")) {
                    // "else if ..." – the nested if consumes the shared 'end'
                    If nested = parseIf(elseLine, elseArgs.subList(1, elseArgs.size()));
                    if (nested != null) otherwise.add(nested);
                    chained = true;
                } else {
                    if (!elseArgs.isEmpty()) error(elseLine.no(), "else 뒤에는 if 만 올 수 있습니다");
                    pos++;
                    otherwise = parseBlock(true, line.no());
                    while ("else".equals(peekKeyword())) {
                        error(lines.get(pos).no(), "하나의 if 에 else 는 한 번만 쓸 수 있습니다");
                        pos++;
                        parseBlock(true, line.no());
                    }
                }
            }

            if (!chained) {
                if ("end".equals(peekKeyword())) pos++;
                else error(line.no(), "if 블록이 end 로 닫히지 않았습니다");
            }
            if (errors.size() > errorsBefore || left == null || right == null) return null;
            return new If(line.no(), left, op, right, then, otherwise);
        }

        // ---- actions ---------------------------------------------------------

        private Statement parseAction(Line line) {
            String kw = line.keyword();
            ActionType type = ActionType.byKeyword(kw);
            int no = line.no();
            if (type == null) {
                if (TriggerType.byKeyword(kw) != null && !kw.equals("timer")) {
                    error(no, "'" + kw + "' 는 이벤트입니다. 'on " + kw + "' 로 새 블록을 시작하세요");
                } else {
                    error(no, "알 수 없는 명령어: " + line.tokens().get(0).text() + suggest(kw, actionKeywordList()));
                }
                return null;
            }

            List<Token> args = line.args();
            List<String> out = new ArrayList<>();
            int errorsBefore = errors.size();

            switch (type) {
                case MESSAGE, BROADCAST, ACTIONBAR -> {
                    String text = joinText(args);
                    if (text.isEmpty()) error(no, "사용법: " + type.keyword + " \"텍스트\"");
                    else out.add(checkText(text, no));
                }
                case TITLE -> {
                    if (!args.isEmpty() && args.get(0).quoted()) {
                        if (args.size() > 2) error(no, "사용법: title \"제목\" [\"부제목\"]");
                        for (Token t : args) out.add(checkText(t.text(), no));
                    } else {
                        String text = joinText(args);
                        if (text.isEmpty()) error(no, "사용법: title \"제목\" [\"부제목\"]");
                        else out.add(checkText(text, no));
                    }
                }
                case SOUND -> {
                    if (args.isEmpty() || args.size() > 3) {
                        error(no, "사용법: sound <사운드> [볼륨] [피치]  예: sound entity.player.levelup");
                    } else {
                        String key = args.get(0).lower();
                        if (!SOUND_KEY.matcher(key).matches()) error(no, "사운드 이름이 올바르지 않습니다: " + key);
                        out.add(key);
                        if (args.size() > 1) out.add(checkNumber(args.get(1).text(), 0, 10, "볼륨", no));
                        if (args.size() > 2) out.add(checkNumber(args.get(2).text(), 0.5, 2, "피치", no));
                    }
                }
                case PARTICLE -> {
                    if (args.isEmpty() || args.size() > 2) {
                        error(no, "사용법: particle <파티클> [개수]  예: particle heart 10");
                    } else {
                        String name = args.get(0).text().toUpperCase(Locale.ROOT);
                        Particle particle = null;
                        try {
                            particle = Particle.valueOf(name);
                        } catch (IllegalArgumentException ignored) {
                        }
                        if (particle == null) error(no, "알 수 없는 파티클: " + args.get(0).text());
                        else if (particle.getDataType() != Void.class) error(no, "추가 데이터가 필요한 파티클은 사용할 수 없습니다: " + args.get(0).text());
                        out.add(name);
                        if (args.size() > 1) out.add(checkNumber(args.get(1).text(), 1, 200, "개수", no));
                    }
                }
                case TELEPORT -> {
                    if (args.size() == 1 && (args.get(0).lower().equals("spawn") || args.get(0).lower().equals("checkpoint"))) {
                        out.add(args.get(0).lower());
                    } else if (args.size() == 3 || args.size() == 5) {
                        for (int i = 0; i < args.size(); i++) {
                            String c = args.get(i).text();
                            boolean rel = i < 3 && c.startsWith("~");
                            String num = rel ? c.substring(1) : c;
                            if (!num.isEmpty() && parseDouble(num) == null) {
                                error(no, "좌표가 올바르지 않습니다: " + c);
                            } else if (num.isEmpty() && !rel) {
                                error(no, "좌표가 올바르지 않습니다: " + c);
                            }
                            out.add(c);
                        }
                    } else {
                        error(no, "사용법: teleport <x> <y> <z> [yaw] [pitch]  |  teleport spawn  |  teleport checkpoint");
                    }
                }
                case CHECKPOINT, RESPAWN, LOBBY, CLEAR, HEAL, STOP -> {
                    if (!args.isEmpty()) error(no, "'" + type.keyword + "' 뒤에는 아무것도 붙이지 않습니다");
                }
                case GIVE -> {
                    if (args.isEmpty() || args.size() > 2) {
                        error(no, "사용법: give <아이템> [개수]  예: give diamond_sword");
                    } else {
                        Material mat = Material.matchMaterial(args.get(0).text());
                        if (mat == null || !mat.isItem() || mat.isAir()) {
                            error(no, "알 수 없는 아이템: " + args.get(0).text());
                        } else if (DENIED_ITEMS.contains(mat.name()) || mat.name().endsWith("_SPAWN_EGG")) {
                            error(no, "스크립트로 지급할 수 없는 아이템입니다: " + mat.name().toLowerCase(Locale.ROOT));
                        } else {
                            out.add(mat.name());
                        }
                        if (args.size() > 1) out.add(checkNumber(args.get(1).text(), 1, 64, "개수", no));
                    }
                }
                case GAMEMODE -> {
                    String mode = args.size() == 1 ? args.get(0).lower() : "";
                    switch (mode) {
                        case "survival", "0", "s"  -> out.add("SURVIVAL");
                        case "adventure", "2", "a" -> out.add("ADVENTURE");
                        case "spectator", "3", "sp" -> out.add("SPECTATOR");
                        default -> error(no, "사용법: gamemode survival|adventure|spectator");
                    }
                }
                case DAMAGE -> {
                    if (args.size() != 1) error(no, "사용법: damage <양>  (하트 1칸 = 2)");
                    else out.add(checkNumber(args.get(0).text(), 0, 100, "피해량", no));
                }
                case EFFECT -> {
                    if (args.size() == 1 && args.get(0).lower().equals("clear")) {
                        out.add("clear");
                    } else if (args.isEmpty() || args.size() > 3) {
                        error(no, "사용법: effect <효과> [초] [레벨]  |  effect clear  예: effect speed 10 2");
                    } else {
                        String key = args.get(0).lower();
                        if (findEffect(key) == null) error(no, "알 수 없는 효과: " + args.get(0).text());
                        out.add(key);
                        out.add(args.size() > 1 ? checkNumber(args.get(1).text(), 1, 600, "지속 시간", no) : "10");
                        out.add(args.size() > 2 ? checkNumber(args.get(2).text(), 1, 10, "레벨", no) : "1");
                    }
                }
                case SET, ADD -> {
                    if (args.size() != 2) {
                        error(no, "사용법: " + type.keyword + " <변수> <값>  예: " + type.keyword + " coins 1");
                    } else {
                        String var = args.get(0).lower();
                        if (!IDENTIFIER.matcher(var).matches()) {
                            error(no, "변수 이름이 올바르지 않습니다: " + var);
                        } else if (Operand.BUILTINS.contains(var)) {
                            error(no, "'" + var + "' 는 내장 값이라 바꿀 수 없습니다");
                        }
                        out.add(var);
                        parseOperand(args.get(1).text(), no);
                        out.add(args.get(1).lower());
                    }
                }
                case TIMER -> {
                    String sub = args.size() == 1 ? args.get(0).lower() : "";
                    if (!sub.equals("start") && !sub.equals("stop")) error(no, "사용법: timer start  |  timer stop");
                    else out.add(sub);
                }
                case WAIT -> {
                    if (args.size() != 1) error(no, "사용법: wait <초>  예: wait 1.5");
                    else out.add(checkNumber(args.get(0).text(), 0.05, MAX_WAIT_SECONDS, "대기 시간", no));
                }
            }

            if (errors.size() > errorsBefore) return null;
            return new Action(no, type, out);
        }

        // ---- helpers ---------------------------------------------------------

        private Operand parseOperand(String raw, int no) {
            Double d = parseDouble(raw);
            if (d != null) return new Operand(d, null);
            String name = raw.toLowerCase(Locale.ROOT);
            if (!IDENTIFIER.matcher(name).matches()) {
                error(no, "값이 올바르지 않습니다: " + raw + " (숫자, 변수 이름, time/y/players 같은 내장 값만 가능)");
                return null;
            }
            return new Operand(null, name);
        }

        private String checkText(String text, int no) {
            if (text.length() > MAX_TEXT_LENGTH) error(no, "텍스트가 너무 깁니다 (최대 " + MAX_TEXT_LENGTH + "자)");
            return text;
        }

        private String checkNumber(String raw, double min, double max, String what, int no) {
            Double d = parseDouble(raw);
            if (d == null || d < min || d > max) {
                error(no, what + " 값은 " + fmt(min) + " ~ " + fmt(max) + " 사이 숫자여야 합니다: " + raw);
            }
            return raw;
        }
    }

    // -------------------------------------------------------------------------
    // Static helpers
    // -------------------------------------------------------------------------

    /** Quoted tokens are used verbatim; bare words are joined with spaces (lenient text). */
    private static String joinText(List<Token> tokens) {
        StringBuilder sb = new StringBuilder();
        for (Token t : tokens) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(t.text());
        }
        return sb.toString();
    }

    private static String joinRaw(List<Token> tokens) {
        return joinText(tokens).trim();
    }

    /** Looks up a potion effect by key ({@code speed}, {@code minecraft:jump_boost}); {@code null} if unknown. */
    static PotionEffectType findEffect(String key) {
        try {
            NamespacedKey nk = NamespacedKey.fromString(key);
            return nk == null ? null : Registry.EFFECT.get(nk);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static Integer parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static Double parseDouble(String s) {
        try {
            double d = Double.parseDouble(s);
            if (Double.isNaN(d) || Double.isInfinite(d)) return null;
            return d;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static String fmt(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
    }

    private static List<String> triggerKeywordList() {
        List<String> list = new ArrayList<>();
        for (TriggerType t : TriggerType.values()) list.add(t.keyword);
        return list;
    }

    private static String triggerKeywords() {
        return String.join(", ", triggerKeywordList());
    }

    private static List<String> actionKeywordList() {
        List<String> list = new ArrayList<>();
        for (ActionType t : ActionType.values()) list.add(t.keyword);
        list.add("if");
        return list;
    }

    /** Returns " (혹시 'xxx' 인가요?)" when a keyword is within edit distance 2. */
    private static String suggest(String word, List<String> candidates) {
        String best = null;
        int bestDist = 3;
        for (String c : candidates) {
            int d = editDistance(word, c);
            if (d < bestDist) {
                bestDist = d;
                best = c;
            }
        }
        return best == null ? "" : " (혹시 '" + best + "' 인가요?)";
    }

    private static int editDistance(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur  = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = cur;
            cur = tmp;
        }
        return prev[b.length()];
    }
}
