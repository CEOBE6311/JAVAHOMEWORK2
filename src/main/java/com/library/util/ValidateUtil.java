package com.library.util;

import java.util.regex.Pattern;

/** 服务端参数校验。 */
public final class ValidateUtil {

    private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");

    private ValidateUtil() {
    }

    public static String text(String value, String field, int min, int max) {
        String v = value == null ? "" : value.trim();
        if (v.length() < min) {
            throw new ApiException(400, 1001, field + "不能为空");
        }
        if (v.length() > max) {
            throw new ApiException(400, 1001, field + "不能超过 " + max + " 个字符");
        }
        return v;
    }

    public static String optionalText(String value, String field, int max) {
        String v = value == null ? "" : value.trim();
        if (v.length() > max) {
            throw new ApiException(400, 1001, field + "不能超过 " + max + " 个字符");
        }
        return v;
    }

    public static String phone(String value) {
        String v = value == null ? "" : value.trim();
        if (!PHONE.matcher(v).matches()) {
            throw new ApiException(400, 1001, "手机号格式不正确");
        }
        return v;
    }

    public static int intRange(Integer value, String field, int min, int max) {
        if (value == null) {
            throw new ApiException(400, 1001, field + "不能为空");
        }
        if (value < min || value > max) {
            throw new ApiException(400, 1001, field + "必须在 " + min + "–" + max + " 之间");
        }
        return value;
    }

    public static double price(Double value) {
        if (value == null) {
            throw new ApiException(400, 1001, "价格不能为空");
        }
        if (value < 0 || value > 99999) {
            throw new ApiException(400, 1001, "价格必须在 0–99999 之间");
        }
        return value;
    }

    public static String role(String value) {
        String v = value == null ? "" : value.trim();
        if (!v.equals("管理员") && !v.equals("操作员") && !v.equals("学生") && !v.equals("教师")) {
            throw new ApiException(400, 1001, "未知角色");
        }
        return v;
    }
}
