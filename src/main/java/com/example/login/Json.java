package com.example.login;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal JSON helpers: escaping for responses and a strict parser for the flat
 * string object used by the login endpoint.
 */
public final class Json {

    private Json() {
    }

    public static String quote(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder(value.length() + 2);
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    /**
     * Parses a JSON object whose values are all strings, e.g.
     * {@code {"username":"demo","password":"secret"}}. Any other JSON shape is
     * rejected with {@link JsonParseException}.
     */
    public static Map<String, String> parseFlatStringObject(String text) {
        if (text == null) {
            throw new JsonParseException("body is empty");
        }
        Parser parser = new Parser(text);
        Map<String, String> values = parser.parseObject();
        parser.skipWhitespace();
        if (!parser.atEnd()) {
            throw new JsonParseException("unexpected trailing content");
        }
        return values;
    }

    public static final class JsonParseException extends RuntimeException {
        public JsonParseException(String message) {
            super(message);
        }
    }

    private static final class Parser {
        private final String text;
        private int pos;

        Parser(String text) {
            this.text = text;
        }

        Map<String, String> parseObject() {
            Map<String, String> values = new LinkedHashMap<String, String>();
            skipWhitespace();
            expect('{');
            skipWhitespace();
            if (consume('}')) {
                return values;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                String value = parseString();
                values.put(key, value);
                skipWhitespace();
                if (consume(',')) {
                    continue;
                }
                expect('}');
                return values;
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (!atEnd()) {
                char c = text.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    if (atEnd()) {
                        throw new JsonParseException("unterminated escape");
                    }
                    char escaped = text.charAt(pos++);
                    switch (escaped) {
                        case '"':
                            sb.append('"');
                            break;
                        case '\\':
                            sb.append('\\');
                            break;
                        case '/':
                            sb.append('/');
                            break;
                        case 'b':
                            sb.append('\b');
                            break;
                        case 'f':
                            sb.append('\f');
                            break;
                        case 'n':
                            sb.append('\n');
                            break;
                        case 'r':
                            sb.append('\r');
                            break;
                        case 't':
                            sb.append('\t');
                            break;
                        case 'u':
                            sb.append(parseUnicodeEscape());
                            break;
                        default:
                            throw new JsonParseException("invalid escape");
                    }
                } else if (c < 0x20) {
                    throw new JsonParseException("unescaped control character");
                } else {
                    sb.append(c);
                }
            }
            throw new JsonParseException("unterminated string");
        }

        private char parseUnicodeEscape() {
            if (pos + 4 > text.length()) {
                throw new JsonParseException("invalid unicode escape");
            }
            int value = 0;
            for (int i = 0; i < 4; i++) {
                int digit = Character.digit(text.charAt(pos++), 16);
                if (digit < 0) {
                    throw new JsonParseException("invalid unicode escape");
                }
                value = (value << 4) | digit;
            }
            return (char) value;
        }

        void skipWhitespace() {
            while (!atEnd()) {
                char c = text.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    pos++;
                } else {
                    break;
                }
            }
        }

        boolean consume(char expected) {
            if (!atEnd() && text.charAt(pos) == expected) {
                pos++;
                return true;
            }
            return false;
        }

        void expect(char expected) {
            if (!consume(expected)) {
                throw new JsonParseException("expected '" + expected + "'");
            }
        }

        boolean atEnd() {
            return pos >= text.length();
        }
    }
}
