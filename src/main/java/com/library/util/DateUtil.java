package com.library.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** 日期格式与范围工具。 */
public final class DateUtil {

    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateUtil() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now().withNano(0);
    }

    public static LocalDate parseDate(String text, String fieldName) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim(), DATE);
        } catch (DateTimeParseException e) {
            throw new ApiException(400, 1001, fieldName + "格式应为 yyyy-MM-dd");
        }
    }

    public static LocalDateTime startOfDay(String text, String fieldName) {
        LocalDate date = parseDate(text, fieldName);
        return date == null ? null : date.atStartOfDay();
    }

    public static LocalDateTime endOfDay(String text, String fieldName) {
        LocalDate date = parseDate(text, fieldName);
        return date == null ? null : date.atTime(LocalTime.MAX).withNano(0);
    }

    public static String format(LocalDateTime value) {
        return value == null ? null : value.format(DATE_TIME);
    }

    public static String format(LocalDate value) {
        return value == null ? null : value.format(DATE);
    }
}
