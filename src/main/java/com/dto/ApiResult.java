package com.dto;

/**
 * Uniform response envelope: every API response has status, message, and data.
 * Controllers return ApiResult<T> for success; GlobalExceptionHandler returns
 * ApiResult<?> for errors — so the client always sees the same JSON shape.
 */
public class ApiResult<T> {

    private final int status;
    private final String message;
    private final T data;

    private ApiResult(int status, String message, T data) {
        this.status  = status;
        this.message = message;
        this.data    = data;
    }

    public static <T> ApiResult<T> success(String message, T data) {
        return new ApiResult<>(200, message, data);
    }

    public static <T> ApiResult<T> created(String message, T data) {
        return new ApiResult<>(201, message, data);
    }

    public static <T> ApiResult<T> error(int status, String message) {
        return new ApiResult<>(status, message, null);
    }

    public int getStatus()    { return status; }
    public String getMessage(){ return message; }
    public T getData()        { return data; }
}
