package com.library.util;

import java.io.Serializable;
import java.util.Arrays;

/** 登录会话中的轻量用户，绝不包含密码。 */
public class SessionUser implements Serializable {

    private static final long serialVersionUID = 1L;

    public final String id;
    public final String name;
    public final String role;
    public final String[] permissions;

    public SessionUser(String id, String name, String role, String[] permissions) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.permissions = permissions == null ? new String[0] : permissions;
    }

    public boolean has(String permission) {
        return permission != null && Arrays.asList(permissions).contains(permission);
    }
}
