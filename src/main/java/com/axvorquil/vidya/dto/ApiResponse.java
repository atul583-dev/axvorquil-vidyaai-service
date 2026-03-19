package com.axvorquil.vidya.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String  message;
    private T       data;

    public static <T> ApiResponse<T> ok(T data)                   { return ApiResponse.<T>builder().success(true).data(data).build(); }
    public static <T> ApiResponse<T> ok(String msg, T data)       { return ApiResponse.<T>builder().success(true).message(msg).data(data).build(); }
    public static <T> ApiResponse<T> error(String msg)            { return ApiResponse.<T>builder().success(false).message(msg).build(); }
}
