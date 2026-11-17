package com.example.login;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonTest {

    @Test
    void parsesFlatStringObject() {
        Map<String, String> values = Json.parseFlatStringObject("  {\"username\":\"demo\", \"password\":\"p\\\"w\"}  ");
        assertEquals("demo", values.get("username"));
        assertEquals("p\"w", values.get("password"));
    }

    @Test
    void parsesUnicodeEscapes() {
        Map<String, String> values = Json.parseFlatStringObject("{\"name\":\"\\u4f60\\u597d\"}");
        assertEquals("\u4f60\u597d", values.get("name"));
    }

    @Test
    void parsesEmptyObject() {
        assertTrue(Json.parseFlatStringObject("{}").isEmpty());
    }

    @Test
    void rejectsTrailingContent() {
        assertThrows(Json.JsonParseException.class,
                () -> Json.parseFlatStringObject("{\"username\":\"demo\"} trailing"));
    }

    @Test
    void rejectsNonStringValues() {
        assertThrows(Json.JsonParseException.class,
                () -> Json.parseFlatStringObject("{\"username\":123}"));
        assertThrows(Json.JsonParseException.class,
                () -> Json.parseFlatStringObject("{\"username\":null}"));
    }

    @Test
    void rejectsMalformedDocuments() {
        assertThrows(Json.JsonParseException.class, () -> Json.parseFlatStringObject(""));
        assertThrows(Json.JsonParseException.class, () -> Json.parseFlatStringObject("{"));
        assertThrows(Json.JsonParseException.class, () -> Json.parseFlatStringObject("{\"username\"}"));
        assertThrows(Json.JsonParseException.class, () -> Json.parseFlatStringObject("{\"username\":\"x\""));
        assertThrows(Json.JsonParseException.class, () -> Json.parseFlatStringObject(null));
    }

    @Test
    void quotesSpecialCharacters() {
        assertEquals("\"a\\\"b\\\\c\\n\"", Json.quote("a\"b\\c\n"));
        assertEquals("null", Json.quote(null));
    }
}
