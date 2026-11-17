package com.example.login;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * HTTP front end for the demo login service. Built on the JDK's own
 * {@link HttpServer} so the demo has no runtime dependencies.
 *
 * <pre>
 * POST /api/login   {"username":"demo","password":"demo123"} -> {"token":"...", ...}
 * GET  /api/me      Authorization: Bearer &lt;token&gt;            -> {"username":..., "displayName":...}
 * POST /api/logout  Authorization: Bearer &lt;token&gt;            -> 204
 * </pre>
 */
public final class LoginServer {

    private static final int MAX_BODY_BYTES = 8 * 1024;

    private final HttpServer http;
    private final UserStore users;
    private final SessionStore sessions;

    public LoginServer(InetSocketAddress address, UserStore users, SessionStore sessions) throws IOException {
        if (address == null || users == null || sessions == null) {
            throw new IllegalArgumentException("address, users and sessions are required");
        }
        this.users = users;
        this.sessions = sessions;
        this.http = HttpServer.create(address, 0);
        this.http.createContext("/", new Dispatcher());
    }

    public static LoginServer createDefault(InetSocketAddress address) throws IOException {
        return new LoginServer(address, UserStore.withDemoUsers(), new SessionStore());
    }

    public void start() {
        http.start();
    }

    public void stop() {
        http.stop(0);
    }

    public int port() {
        return http.getAddress().getPort();
    }

    private final class Dispatcher implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String path = exchange.getRequestURI().getPath();
                String method = exchange.getRequestMethod();
                if ("/api/login".equals(path)) {
                    if (!"POST".equalsIgnoreCase(method)) {
                        methodNotAllowed(exchange, "POST");
                        return;
                    }
                    handleLogin(exchange);
                } else if ("/api/me".equals(path)) {
                    if (!"GET".equalsIgnoreCase(method)) {
                        methodNotAllowed(exchange, "GET");
                        return;
                    }
                    handleMe(exchange);
                } else if ("/api/logout".equals(path)) {
                    if (!"POST".equalsIgnoreCase(method)) {
                        methodNotAllowed(exchange, "POST");
                        return;
                    }
                    handleLogout(exchange);
                } else {
                    sendJson(exchange, 404, error("not_found", "no such endpoint"));
                }
            } catch (BodyTooLargeException e) {
                sendJson(exchange, 413, error("payload_too_large", "request body is too large"));
            } catch (Json.JsonParseException e) {
                sendJson(exchange, 400, error("invalid_request", e.getMessage()));
            } catch (IOException e) {
                throw e;
            } catch (RuntimeException e) {
                sendJson(exchange, 500, error("internal_error", "unexpected server error"));
            } finally {
                exchange.close();
            }
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null
                || !contentType.toLowerCase(Locale.ROOT).startsWith("application/json")) {
            sendJson(exchange, 415,
                    error("unsupported_media_type", "Content-Type must be application/json"));
            return;
        }

        Map<String, String> fields = Json.parseFlatStringObject(readBody(exchange));
        String username = fields.get("username");
        String password = fields.get("password");
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            sendJson(exchange, 400,
                    error("invalid_request", "username and password are required"));
            return;
        }

        username = username.trim();
        if (!users.authenticate(username, password)) {
            sendJson(exchange, 401,
                    error("invalid_credentials", "invalid username or password"));
            return;
        }

        Optional<UserStore.User> user = users.find(username);
        if (!user.isPresent()) {
            sendJson(exchange, 401,
                    error("invalid_credentials", "invalid username or password"));
            return;
        }

        String token = sessions.create(username);
        String body = "{"
                + "\"token\":" + Json.quote(token) + ","
                + "\"tokenType\":\"Bearer\","
                + "\"expiresIn\":" + sessions.ttl().getSeconds() + ","
                + "\"username\":" + Json.quote(user.get().username()) + ","
                + "\"displayName\":" + Json.quote(user.get().displayName())
                + "}";
        sendJson(exchange, 200, body);
    }

    private void handleMe(HttpExchange exchange) throws IOException {
        Optional<String> username = sessions.resolve(bearerToken(exchange));
        if (!username.isPresent()) {
            sendJson(exchange, 401, error("unauthorized", "missing or invalid token"));
            return;
        }
        Optional<UserStore.User> user = users.find(username.get());
        if (!user.isPresent()) {
            sendJson(exchange, 401, error("unauthorized", "missing or invalid token"));
            return;
        }
        String body = "{"
                + "\"username\":" + Json.quote(user.get().username()) + ","
                + "\"displayName\":" + Json.quote(user.get().displayName())
                + "}";
        sendJson(exchange, 200, body);
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        String token = bearerToken(exchange);
        if (!sessions.resolve(token).isPresent()) {
            sendJson(exchange, 401, error("unauthorized", "missing or invalid token"));
            return;
        }
        sessions.revoke(token);
        exchange.sendResponseHeaders(204, -1);
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        InputStream in = exchange.getRequestBody();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int total = 0;
        int read;
        while ((read = in.read(buffer)) != -1) {
            total += read;
            if (total > MAX_BODY_BYTES) {
                throw new BodyTooLargeException();
            }
            out.write(buffer, 0, read);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private static String bearerToken(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null) {
            return null;
        }
        String value = header.trim();
        if (!value.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            return null;
        }
        String token = value.substring("Bearer ".length()).trim();
        return token.isEmpty() ? null : token;
    }

    private static void methodNotAllowed(HttpExchange exchange, String allowed) throws IOException {
        exchange.getResponseHeaders().set("Allow", allowed);
        sendJson(exchange, 405, error("method_not_allowed", "use " + allowed));
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static String error(String code, String message) {
        return "{\"error\":" + Json.quote(code) + ",\"message\":" + Json.quote(message) + "}";
    }

    private static final class BodyTooLargeException extends RuntimeException {
    }
}
