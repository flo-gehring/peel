package de.flogehring.peel.convenience.output;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

final class TraceJsonOutput {

    private static final String OBJECT_MAPPER_CLASS = "com.fasterxml.jackson.databind.ObjectMapper";
    private static final Method JACKSON_WRITE_VALUE_AS_STRING;

    static {
        Method method = null;
        try {
            Class<?> objectMapperClass = Class.forName(OBJECT_MAPPER_CLASS);
            method = objectMapperClass.getMethod("writeValueAsString", Object.class);
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            method = null;
        }
        JACKSON_WRITE_VALUE_AS_STRING = method;
    }

    private TraceJsonOutput() {
    }

    static String toJson(Map<String, Object> source) {
        if (JACKSON_WRITE_VALUE_AS_STRING != null) {
            String jacksonOutput = tryJackson(source);
            if (jacksonOutput != null) {
                return jacksonOutput;
            }
        }
        return JsonStringBuilder.toJson(source);
    }

    private static String tryJackson(Map<String, Object> source) {
        try {
            Object objectMapper = JACKSON_WRITE_VALUE_AS_STRING.getDeclaringClass().getDeclaredConstructor().newInstance();
            Object output = JACKSON_WRITE_VALUE_AS_STRING.invoke(objectMapper, source);
            return (String) output;
        } catch (InstantiationException
                 | IllegalAccessException
                 | InvocationTargetException
                 | NoSuchMethodException e) {
            return null;
        }
    }

    private static final class JsonStringBuilder {

        private JsonStringBuilder() {
        }

        static String toJson(Object value) {
            StringBuilder sb = new StringBuilder();
            appendValue(value, sb);
            return sb.toString();
        }

        private static void appendValue(Object value, StringBuilder sb) {
            if (value == null) {
                sb.append("null");
                return;
            }
            if (value instanceof String string) {
                appendString(string, sb);
                return;
            }
            if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
                return;
            }
            if (value instanceof Map<?, ?> map) {
                appendMap(map, sb);
                return;
            }
            if (value instanceof List<?> list) {
                appendList(list, sb);
                return;
            }
            throw new IllegalArgumentException("Unsupported json node type: " + value.getClass().getName());
        }

        private static void appendMap(Map<?, ?> map, StringBuilder sb) {
            sb.append('{');
            Iterator<? extends Map.Entry<?, ?>> iterator = map.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<?, ?> entry = iterator.next();
                if (!(entry.getKey() instanceof String key)) {
                    throw new IllegalArgumentException("JSON map keys must be String");
                }
                appendString(key, sb);
                sb.append(':');
                appendValue(entry.getValue(), sb);
                if (iterator.hasNext()) {
                    sb.append(',');
                }
            }
            sb.append('}');
        }

        private static void appendList(List<?> list, StringBuilder sb) {
            sb.append('[');
            for (int i = 0; i < list.size(); i++) {
                appendValue(list.get(i), sb);
                if (i < list.size() - 1) {
                    sb.append(',');
                }
            }
            sb.append(']');
        }

        private static void appendString(String value, StringBuilder sb) {
            sb.append('"');
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                switch (c) {
                    case '"' -> sb.append("\\\"");
                    case '\\' -> sb.append("\\\\");
                    case '\b' -> sb.append("\\b");
                    case '\f' -> sb.append("\\f");
                    case '\n' -> sb.append("\\n");
                    case '\r' -> sb.append("\\r");
                    case '\t' -> sb.append("\\t");
                    default -> {
                        if (c < 0x20) {
                            sb.append(String.format("\\u%04x", (int) c));
                        } else {
                            sb.append(c);
                        }
                    }
                }
            }
            sb.append('"');
        }
    }
}
