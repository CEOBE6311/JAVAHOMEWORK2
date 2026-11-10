package com.library.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Gson 统一配置，处理 java.time 与 null。 */
public final class JsonUtil {

    private static final TypeAdapter<LocalDateTime> DATE_TIME_ADAPTER = new TypeAdapter<>() {
        @Override
        public void write(JsonWriter out, LocalDateTime value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(DateUtil.format(value));
            }
        }

        @Override
        public LocalDateTime read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            String text = in.nextString().trim();
            if (text.isEmpty()) {
                return null;
            }
            if (text.length() > 10) {
                return LocalDateTime.parse(text.replace('T', ' ').substring(0, 19), DateUtil.DATE_TIME);
            }
            return LocalDate.parse(text, DateUtil.DATE).atStartOfDay();
        }
    };

    private static final TypeAdapter<LocalDate> DATE_ADAPTER = new TypeAdapter<>() {
        @Override
        public void write(JsonWriter out, LocalDate value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(DateUtil.format(value));
            }
        }

        @Override
        public LocalDate read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            String text = in.nextString().trim();
            return text.isEmpty() ? null : LocalDate.parse(text, DateUtil.DATE);
        }
    };

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, DATE_TIME_ADAPTER)
            .registerTypeAdapter(LocalDate.class, DATE_ADAPTER)
            .serializeNulls()
            .create();

    private JsonUtil() {
    }

    public static Gson gson() {
        return GSON;
    }

    public static String toJson(Object value) {
        return GSON.toJson(value);
    }

    public static <T> T fromJson(String json, Class<T> type) {
        return GSON.fromJson(json, type);
    }
}
