package com.example.demo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

/**
 * 演示服务入口：读取 {@code HOST}/{@code PORT} 环境变量并启动服务。
 */
public final class ServerMain {

    private ServerMain() {
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        String host = environmentOrDefault("HOST", "127.0.0.1");
        int port = Integer.parseInt(environmentOrDefault("PORT", "8080"));

        UserApiServer server = new UserApiServer(new InetSocketAddress(host, port));
        Runtime.getRuntime().addShutdownHook(new Thread(server::close, "shutdown"));
        server.start();

        System.out.println("Server listening on http://" + host + ":" + port);
        System.out.println("GET http://" + host + ":" + port + UserApiServer.USER_PATH);

        // 主线程保持存活，由 SIGINT/SIGTERM 触发 shutdown hook 关闭服务。
        new CountDownLatch(1).await();
    }

    private static String environmentOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }
}
