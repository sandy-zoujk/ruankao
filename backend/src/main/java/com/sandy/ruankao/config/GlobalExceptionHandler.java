package com.sandy.ruankao.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(
      IllegalArgumentException exception) {
    log.warn("业务校验未通过 exceptionType={}", exception.getClass().getSimpleName());
    return ResponseEntity.badRequest().body(ApiResponse.error(exception.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidation(
      MethodArgumentNotValidException exception) {
    // 返回第一个字段校验错误，使用户可以按顺序修正输入内容。
    log.warn("请求字段校验未通过 errorCount={}", exception.getBindingResult().getErrorCount());
    String message = exception.getBindingResult().getAllErrors().get(0).getDefaultMessage();
    return ResponseEntity.badRequest().body(ApiResponse.error(message));
  }
}
