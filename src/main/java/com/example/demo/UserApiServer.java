package com.example.demo;

import com.example.demo.entity.User;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 基于 JDK 自带 {@link HttpServer} 的演示服务。
 *
 * <p>只有一个接口：{@code GET /api/users/1}，返回由 {@link User} 实体序列化出的固定 JSON 内容。
 * 其余路径返回 404 JSON。</p>
 */
public final class UserApiServer implements AutoCloseable {

    /** 固定返回的用户实体。 */
    static final User SAMPLE_USER = new User(1L, "demo", "Demo User", true);

    /** 已知接口路径。 */
    static final String USER_PATH = "/api/users/1";

    private final HttpServer server;

    /**
     * 在给定地址上创建（但尚未启动）HTTP 服务。
     *
     * @param address 监听地址，端口传 0 时由系统分配空闲端口
     * @throws IOException 绑定地址失败
     */
    public UserApiServer(InetSocketAddress address) throws IOException {
        Objects.requireNonNull(address, "address");
        this.server = HttpServer.create(address, 0);
        this.server.createContext("/", this::handle);
    }

    /** 启动服务。 */
    public void start() {
        server.start();
    }

    /** 返回实际监听端口（适用于构造时传入端口 0 的场景）。 */
    public int getPort() {
        return server.getAddress().getPort();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("GET".equalsIgnoreCase(method) && USER_PATH.equals(path)) {
                sendJson(exchange, 200, toJson(SAMPLE_USER));
                return;
            }

            sendJson(exchange, 404, "{\"error\":\"not found\"}");
        } finally {
            exchange.close();
        }
    }

    private static void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    /** 把实体序列化为 JSON；字段值经过转义，避免生成非法 JSON。 */
    static String toJson(User user) {
        Objects.requireNonNull(user, "user");
        return "{"
                + "\"id\":" + user.getId()
                + ",\"username\":\"" + escape(user.getUsername()) + "\""
                + ",\"displayName\":\"" + escape(user.getDisplayName()) + "\""
                + ",\"enabled\":" + user.isEnabled()
                + "}";
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
            }
        }
        return escaped.toString();
    }
}
