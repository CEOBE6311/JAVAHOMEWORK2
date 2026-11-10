package com.library.util;

import java.io.InputStream;
import java.util.Properties;

/** 读取 db.properties；环境变量优先级高于文件。 */
public final class DbConfig {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = DbConfig.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new IllegalStateException("classpath 下找不到 db.properties，请复制 db.properties.example 并填写数据库密码");
            }
            PROPS.load(in);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private DbConfig() {
    }

    public static String get(String key) {
        String env = System.getenv("LIB_" + key.toUpperCase().replace('.', '_'));
        if ((env == null || env.isBlank()) && "jdbc.url".equals(key)) {
            env = System.getenv("LIB_DB_URL");
        }
        if ((env == null || env.isBlank()) && "jdbc.username".equals(key)) {
            env = System.getenv("LIB_DB_USER");
        }
        if ((env == null || env.isBlank()) && "jdbc.password".equals(key)) {
            env = System.getenv("LIB_DB_PASSWORD");
        }
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        return PROPS.getProperty(key, "").trim();
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        return value.isEmpty() ? defaultValue : Boolean.parseBoolean(value);
    }

    public static String url() {
        return get("jdbc.url");
    }

    public static String username() {
        return get("jdbc.username");
    }

    public static String password() {
        return get("jdbc.password");
    }

    public static boolean allowStaffRegister() {
        return getBoolean("app.allowStaffRegister", true);
    }

    public static boolean captchaEnabled() {
        String sys = System.getProperty("app.captcha.enabled");
        if (sys != null) {
            return Boolean.parseBoolean(sys);
        }
        return getBoolean("app.captcha.enabled", true);
    }
}
