package com.example.login;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** End-to-end tests against a real HTTP server on an ephemeral port. */
class LoginServerTest {

    private LoginServer server;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        server = new LoginServer(new InetSocketAddress("127.0.0.1", 0),
                UserStore.withDemoUsers(), new SessionStore());
        server.start();
        baseUrl = "http://127.0.0.1:" + server.port();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void loginSucceedsAndTokenUnlocksProtectedEndpoint() throws IOException {
        Response login = login("demo", "demo123");
        assertEquals(200, login.status);
        assertEquals("application/json; charset=utf-8", login.contentType());

        String token = field(login.body, "token");
        assertFalse(token.isEmpty());
        assertEquals("Bearer", field(login.body, "tokenType"));
        assertTrue(login.body.contains("\"expiresIn\":1800"), login.body);
        assertEquals("demo", field(login.body, "username"));
        assertEquals("Demo User", field(login.body, "displayName"));

        Response me = request("GET", "/api/me", null, token);
        assertEquals(200, me.status);
        Map<String, String> meBody = Json.parseFlatStringObject(me.body);
        assertEquals("demo", meBody.get("username"));
        assertEquals("Demo User", meBody.get("displayName"));
    }

    @Test
    void loginRejectsWrongPassword() throws IOException {
        Response response = login("demo", "wrong-password");
        assertEquals(401, response.status);
        assertEquals("invalid_credentials", Json.parseFlatStringObject(response.body).get("error"));
    }

    @Test
    void loginRejectsUnknownUser() throws IOException {
        Response response = login("nobody", "demo123");
        assertEquals(401, response.status);
    }

    @Test
    void loginRejectsMissingFields() throws IOException {
        Response response = request("POST", "/api/login", "{}", null);
        assertEquals(400, response.status);
        assertEquals("invalid_request", Json.parseFlatStringObject(response.body).get("error"));
    }

    @Test
    void loginRejectsMalformedJson() throws IOException {
        Response response = request("POST", "/api/login", "{\"username\":", null);
        assertEquals(400, response.status);
    }

    @Test
    void loginRejectsNonStringValues() throws IOException {
        Response response = request("POST", "/api/login",
                "{\"username\":1,\"password\":\"demo123\"}", null);
        assertEquals(400, response.status);
    }

    @Test
    void loginRequiresJsonContentType() throws IOException {
        Response response = requestWithContentType("POST", "/api/login", "username=demo&password=demo123",
                null, "application/x-www-form-urlencoded");
        assertEquals(415, response.status);
    }

    @Test
    void protectedEndpointRejectsMissingToken() throws IOException {
        Response response = request("GET", "/api/me", null, null);
        assertEquals(401, response.status);
        assertEquals("unauthorized", Json.parseFlatStringObject(response.body).get("error"));
    }

    @Test
    void protectedEndpointRejectsUnknownToken() throws IOException {
        Response response = request("GET", "/api/me", null, "not-a-real-token");
        assertEquals(401, response.status);
    }

    @Test
    void logoutRevokesToken() throws IOException {
        String token = field(login("demo", "demo123").body, "token");

        Response logout = request("POST", "/api/logout", null, token);
        assertEquals(204, logout.status);

        Response me = request("GET", "/api/me", null, token);
        assertEquals(401, me.status);

        Response logoutAgain = request("POST", "/api/logout", null, token);
        assertEquals(401, logoutAgain.status);
    }

    @Test
    void wrongMethodIsRejected() throws IOException {
        Response response = request("GET", "/api/login", null, null);
        assertEquals(405, response.status);
        assertEquals("POST", response.header("Allow"));
    }

    @Test
    void unknownEndpointReturns404() throws IOException {
        Response response = request("GET", "/nope", null, null);
        assertEquals(404, response.status);
        assertEquals("not_found", Json.parseFlatStringObject(response.body).get("error"));
    }

    @Test
    void oversizedBodyIsRejected() throws IOException {
        StringBuilder huge = new StringBuilder("{\"username\":\"");
        for (int i = 0; i < 9 * 1024; i++) {
            huge.append('a');
        }
        huge.append("\",\"password\":\"x\"}");
        Response response = request("POST", "/api/login", huge.toString(), null);
        assertEquals(413, response.status);
    }

    /** Reads one string field from a response body; the success body also has a numeric field. */
    private static String field(String json, String name) {
        String needle = "\"" + name + "\":\"";
        int start = json.indexOf(needle);
        assertTrue(start >= 0, "missing field " + name + " in " + json);
        start += needle.length();
        int end = json.indexOf('"', start);
        assertTrue(end > start, "malformed field " + name + " in " + json);
        return json.substring(start, end);
    }

    private Response login(String username, String password) throws IOException {
        String body = "{\"username\":" + Json.quote(username)
                + ",\"password\":" + Json.quote(password) + "}";
        return request("POST", "/api/login", body, null);
    }

    private Response request(String method, String path, String body, String token) throws IOException {
        return requestWithContentType(method, path, body, token, "application/json");
    }

    private Response requestWithContentType(String method, String path, String body,
                                            String token, String contentType) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(baseUrl + path).openConnection();
        try {
            connection.setRequestMethod(method);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            if (contentType != null) {
                connection.setRequestProperty("Content-Type", contentType);
            }
            if (token != null) {
                connection.setRequestProperty("Authorization", "Bearer " + token);
            }
            if (body != null) {
                connection.setDoOutput(true);
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(body.getBytes(StandardCharsets.UTF_8));
                }
            }
            int status = connection.getResponseCode();
            InputStream in = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String responseBody = in == null ? "" : readAll(in);
            return new Response(status, responseBody, connection.getHeaderFields());
        } finally {
            connection.disconnect();
        }
    }

    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private static final class Response {
        final int status;
        final String body;
        final Map<String, List<String>> headers;

        Response(int status, String body, Map<String, List<String>> headers) {
            this.status = status;
            this.body = body;
            this.headers = headers;
        }

        String contentType() {
            return header("Content-Type");
        }

        String header(String name) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                    return entry.getValue().isEmpty() ? null : entry.getValue().get(0);
                }
            }
            return null;
        }
    }
}
