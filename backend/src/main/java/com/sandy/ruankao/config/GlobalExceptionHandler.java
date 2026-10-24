package com.sandy.ruankao.config;

import com.sandy.ruankao.common.ApiResponse;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

/**
 * 全局异常处理器，将常见业务异常转换为统一接口响应。
 *
 * @author zoujunke
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(
      IllegalArgumentException exception) {
    return ResponseEntity.badRequest().body(ApiResponse.error(exception.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidation(
      MethodArgumentNotValidException exception) {
    // 返回第一个字段校验错误，使用户可以按顺序修正输入内容。
    String message = exception.getBindingResult().getAllErrors().get(0).getDefaultMessage();
    return ResponseEntity.badRequest().body(ApiResponse.error(message));
  }
}
