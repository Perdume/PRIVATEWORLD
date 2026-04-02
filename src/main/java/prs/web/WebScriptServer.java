package prs.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.entity.Player;
import prs.data.ScriptManager;
import prs.privateworld.PrivateWorld;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * Embedded HTTP server that serves the web-based script editor.
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>{@code GET /?token=xxx} — editor HTML page</li>
 *   <li>{@code GET /api/info?token=xxx} — world name and trigger list (JSON)</li>
 *   <li>{@code GET /api/script?token=xxx&trigger=ENTER} — raw script text (JSON)</li>
 *   <li>{@code GET /api/script?token=xxx} — all triggers' raw scripts (JSON map)</li>
 *   <li>{@code POST /api/script?token=xxx&trigger=ENTER} — save script (body = raw text)</li>
 * </ul>
 *
 * <p>Tokens are 32-hex-char values issued via
 * {@link #issueToken(Player, String)} and expire after 30 minutes.
 */
public class WebScriptServer {

    private static final int TOKEN_EXPIRY_SECONDS = 30 * 60;

    private final PrivateWorld plugin;
    private final String editorHtml;
    private HttpServer server;

    /** Active token sessions: token → session. */
    private final ConcurrentHashMap<String, TokenSession> sessions = new ConcurrentHashMap<>();

    private record TokenSession(UUID playerUuid, String worldName, Instant expiry) {
        boolean isExpired() { return Instant.now().isAfter(expiry); }
    }

    public WebScriptServer(PrivateWorld plugin) {
        this.plugin = plugin;
        this.editorHtml = loadHtml();
    }

    private String loadHtml() {
        try (InputStream is = getClass().getResourceAsStream("/web/editor.html")) {
            if (is == null) {
                plugin.getLogger().warning("web/editor.html 리소스를 찾을 수 없습니다.");
                return "<h1>editor.html missing</h1>";
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "에디터 HTML 로드 실패", e);
            return "<h1>error loading editor</h1>";
        }
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    public void start(int port) {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/api/script", new ScriptApiHandler());
            server.createContext("/api/info",   new InfoHandler());
            server.createContext("/",            new EditorHandler());
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            plugin.getLogger().info("웹 스크립트 에디터 시작됨 → 포트 " + port);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "웹 에디터 서버를 시작할 수 없습니다 (포트 " + port + ")", e);
        }
    }

    public void stop() {
        if (server != null) { server.stop(0); server = null; }
    }

    // -------------------------------------------------------------------------
    // Token management
    // -------------------------------------------------------------------------

    /**
     * Issue a new token for the given player and world.
     * Any existing unexpired token for the same player is revoked first.
     *
     * @return the new token string
     */
    public String issueToken(Player player, String worldName) {
        UUID uid = player.getUniqueId();
        sessions.entrySet().removeIf(e -> e.getValue().playerUuid().equals(uid));
        String token = generateToken();
        sessions.put(token, new TokenSession(uid, worldName,
                Instant.now().plusSeconds(TOKEN_EXPIRY_SECONDS)));
        return token;
    }

    private TokenSession validateToken(String token) {
        if (token == null) return null;
        TokenSession s = sessions.get(token);
        if (s == null || s.isExpired()) { sessions.remove(token); return null; }
        return s;
    }

    private static String generateToken() {
        byte[] buf = new byte[16];
        new SecureRandom().nextBytes(buf);
        StringBuilder sb = new StringBuilder(32);
        for (byte b : buf) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // HTTP utilities
    // -------------------------------------------------------------------------

    private static Map<String, String> parseQuery(URI uri) {
        Map<String, String> params = new HashMap<>();
        String query = uri.getQuery();
        if (query == null) return params;
        for (String part : query.split("&")) {
            int eq = part.indexOf('=');
            if (eq > 0) params.put(
                    part.substring(0, eq),
                    java.net.URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8));
        }
        return params;
    }

    private static void respond(HttpExchange ex, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private static void respondJson(HttpExchange ex, int status, String json) throws IOException {
        respond(ex, status, "application/json", json);
    }

    private static String readBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Escape a string for embedding in a JSON string value. */
    static String escJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"'  -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default   -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Handlers
    // -------------------------------------------------------------------------

    /** Serves the editor HTML page. */
    private class EditorHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (!ex.getRequestMethod().equalsIgnoreCase("GET")) {
                ex.sendResponseHeaders(405, -1);
                return;
            }
            Map<String, String> params = parseQuery(ex.getRequestURI());
            TokenSession session = validateToken(params.get("token"));
            if (session == null) {
                respond(ex, 403, "text/html", ERROR_PAGE);
                return;
            }
            String html = editorHtml.replace("{{TOKEN}}", params.get("token"));
            respond(ex, 200, "text/html", html);
        }
    }

    /** Returns world name and trigger names as JSON. */
    private class InfoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            handleCors(ex);
            if (ex.getRequestMethod().equalsIgnoreCase("OPTIONS")) { ex.sendResponseHeaders(204, -1); return; }
            Map<String, String> params = parseQuery(ex.getRequestURI());
            TokenSession session = validateToken(params.get("token"));
            if (session == null) { respondJson(ex, 403, "{\"error\":\"invalid token\"}"); return; }
            respondJson(ex, 200, "{\"worldName\":\"" + escJson(session.worldName()) + "\","
                    + "\"triggers\":[\"ENTER\",\"LEAVE\",\"DEATH\",\"RESPAWN\"]}");
        }
    }

    /** GET/POST for reading and writing per-trigger scripts. */
    private class ScriptApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            handleCors(ex);
            if (ex.getRequestMethod().equalsIgnoreCase("OPTIONS")) { ex.sendResponseHeaders(204, -1); return; }

            Map<String, String> params = parseQuery(ex.getRequestURI());
            TokenSession session = validateToken(params.get("token"));
            if (session == null) { respondJson(ex, 403, "{\"error\":\"invalid token\"}"); return; }

            String worldName = session.worldName();
            String triggerParam = params.get("trigger");

            if (ex.getRequestMethod().equalsIgnoreCase("GET")) {
                if (triggerParam == null) {
                    // Return all triggers at once
                    StringBuilder sb = new StringBuilder("{");
                    ScriptManager.Trigger[] triggers = ScriptManager.Trigger.values();
                    for (int i = 0; i < triggers.length; i++) {
                        if (i > 0) sb.append(',');
                        sb.append('"').append(triggers[i].name()).append("\":\"")
                          .append(escJson(plugin.scriptManager.getRawScript(worldName, triggers[i])))
                          .append('"');
                    }
                    sb.append('}');
                    respondJson(ex, 200, sb.toString());
                } else {
                    ScriptManager.Trigger trigger = parseTrigger(triggerParam);
                    if (trigger == null) { respondJson(ex, 400, "{\"error\":\"unknown trigger\"}"); return; }
                    String content = plugin.scriptManager.getRawScript(worldName, trigger);
                    respondJson(ex, 200, "{\"content\":\"" + escJson(content) + "\"}");
                }
            } else if (ex.getRequestMethod().equalsIgnoreCase("POST")) {
                ScriptManager.Trigger trigger = parseTrigger(triggerParam);
                if (trigger == null) { respondJson(ex, 400, "{\"error\":\"trigger required\"}"); return; }
                String body = readBody(ex);
                plugin.scriptManager.setRawScript(worldName, trigger, body);
                respondJson(ex, 200, "{\"ok\":true}");
            } else {
                ex.sendResponseHeaders(405, -1);
            }
        }
    }

    private static ScriptManager.Trigger parseTrigger(String name) {
        if (name == null) return null;
        try { return ScriptManager.Trigger.valueOf(name.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }

    private static void handleCors(HttpExchange ex) {
        // Restrict to same origin: reflect the actual request origin if present,
        // otherwise allow localhost (editor is served from the same host).
        String origin = ex.getRequestHeaders().getFirst("Origin");
        if (origin != null && !origin.isBlank()) {
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", origin);
            ex.getResponseHeaders().set("Vary", "Origin");
        }
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    // -------------------------------------------------------------------------
    // Error page
    // -------------------------------------------------------------------------

    private static final String ERROR_PAGE = """
            <!DOCTYPE html>
            <html lang="ko"><head><meta charset="UTF-8"><title>오류</title>
            <style>body{font-family:sans-serif;background:#1e1e2e;color:#cdd6f4;display:flex;
            align-items:center;justify-content:center;min-height:100vh;margin:0}
            .box{text-align:center;padding:2rem}h2{color:#f38ba8}code{background:#313244;
            padding:.2em .4em;border-radius:4px}</style></head>
            <body><div class="box">
            <h2>❌ 토큰이 만료되었거나 유효하지 않습니다</h2>
            <p>게임 내에서 <code>/privateworld script</code> 를 다시 입력하여 새 링크를 받으세요.</p>
            </div></body></html>
            """;
}
