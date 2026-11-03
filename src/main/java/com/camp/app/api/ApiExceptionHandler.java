package com.camp.app.api;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(BizException.class)
    public BaseResponse<Void> handleBiz(BizException ex) {
        return BaseResponse.error(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<Void> handleValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .orElse("参数错误");
        return BaseResponse.error(400, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public BaseResponse<Void> handleUnreadable(HttpMessageNotReadableException ex) {
        return BaseResponse.error(400, "请求体无法解析");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public BaseResponse<Void> handleIllegal(IllegalArgumentException ex) {
        return BaseResponse.error(400, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public BaseResponse<Void> handleConflict(DataIntegrityViolationException ex) {
        return BaseResponse.error(409, "数据冲突或仍被引用，无法保存");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public BaseResponse<Void> handleTooLarge(MaxUploadSizeExceededException ex) {
        return BaseResponse.error(400, "文件不能超过 5MB");
    }
}
