package com.sandy.ruankao.common;

/**
 * 前后端接口的统一响应结构。
 *
 * @param <T> 响应数据类型
 * @author zoujunke
 */
public class ApiResponse<T> {
  private final boolean success;
  private final String message;
  private final T data;

  private ApiResponse(boolean success, String message, T data) {
    this.success = success;
    this.message = message;
    this.data = data;
  }

  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<T>(true, "操作成功", data);
  }

  public static <T> ApiResponse<T> ok(String message, T data) {
    return new ApiResponse<T>(true, message, data);
  }

  public static <T> ApiResponse<T> error(String message) {
    return new ApiResponse<T>(false, message, null);
  }

  public boolean isSuccess() {
    return success;
  }

  public String getMessage() {
    return message;
  }

  public T getData() {
    return data;
  }
}
