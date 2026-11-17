package com.example.login;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * Starts the demo login service. Configuration comes from the HOST and PORT
 * environment variables: defaults are 127.0.0.1 and 8080.
 */
public final class ServerMain {

    private ServerMain() {
    }

    public static void main(String[] args) throws IOException {
        String host = env("HOST", "127.0.0.1");
        int port = parsePort(env("PORT", "8080"));
        InetSocketAddress address = new InetSocketAddress(InetAddress.getByName(host), port);

        final LoginServer server = LoginServer.createDefault(address);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                server.stop();
            }
        }));

        server.start();
        System.out.println("Login demo listening on http://" + host + ":" + server.port());
        System.out.println("Demo accounts: demo/demo123, admin/admin123");
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private static int parsePort(String value) {
        try {
            int port = Integer.parseInt(value);
            if (port < 0 || port > 65535) {
                throw new NumberFormatException("out of range");
            }
            return port;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("PORT must be an integer between 0 and 65535: " + value);
        }
    }
}
