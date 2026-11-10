package com.library.util;

/** 业务异常：带 HTTP 状态码和业务码。 */
public class ApiException extends RuntimeException {

    private final int status;
    private final int code;

    public ApiException(int status, int code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public ApiException(int status, String message) {
        this(status, 1001, message);
    }

    public int getStatus() {
        return status;
    }

    public int getCode() {
        return code;
    }
}
