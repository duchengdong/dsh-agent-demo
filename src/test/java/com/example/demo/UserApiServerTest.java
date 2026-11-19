package com.example.demo;

import com.example.demo.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserApiServerTest {

    private UserApiServer server;
    private String baseUrl;

    @BeforeEach
    void startServer() throws IOException {
        server = new UserApiServer(new InetSocketAddress("127.0.0.1", 0));
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getPort();
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.close();
        }
    }

    @Test
    void getKnownPathReturnsFixedJson() throws IOException {
        HttpURLConnection connection = open("GET", baseUrl + UserApiServer.USER_PATH);

        assertEquals(200, connection.getResponseCode());
        assertTrue(connection.getContentType().startsWith("application/json"));
        assertEquals(
                "{\"id\":1,\"username\":\"demo\",\"displayName\":\"Demo User\",\"enabled\":true}",
                readBody(connection));
    }

    @Test
    void unknownPathReturns404Json() throws IOException {
        HttpURLConnection connection = open("GET", baseUrl + "/nope");

        assertEquals(404, connection.getResponseCode());
        assertTrue(connection.getContentType().startsWith("application/json"));
        assertEquals("{\"error\":\"not found\"}", readBody(connection));
    }

    @Test
    void nonGetMethodOnKnownPathReturns404() throws IOException {
        HttpURLConnection connection = open("POST", baseUrl + UserApiServer.USER_PATH);

        assertEquals(404, connection.getResponseCode());
        assertEquals("{\"error\":\"not found\"}", readBody(connection));
    }

    @Test
    void toJsonEscapesSpecialCharacters() {
        User user = new User(3L, "a\"b\\c", "line\nbreak", true);

        assertEquals(
                "{\"id\":3,\"username\":\"a\\\"b\\\\c\",\"displayName\":\"line\\nbreak\",\"enabled\":true}",
                UserApiServer.toJson(user));
    }

    @Test
    void toJsonRendersNullTextAsEmptyString() {
        assertEquals(
                "{\"id\":4,\"username\":\"\",\"displayName\":\"\",\"enabled\":false}",
                UserApiServer.toJson(new User(4L, null, null, false)));
    }

    private static HttpURLConnection open(String method, String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        return connection;
    }

    private static String readBody(HttpURLConnection connection) throws IOException {
        InputStream stream = connection.getResponseCode() >= 400
                ? connection.getErrorStream()
                : connection.getInputStream();
        try (InputStream input = stream;
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
