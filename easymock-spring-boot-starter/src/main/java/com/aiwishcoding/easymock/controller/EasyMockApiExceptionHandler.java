package com.aiwishcoding.easymock.controller;

import com.aiwishcoding.easymock.exception.RuleNotFoundException;
import com.aiwishcoding.easymock.exception.RuleValidationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice(basePackages = "com.aiwishcoding.easymock.controller")
public class EasyMockApiExceptionHandler {

    @ExceptionHandler(RuleValidationException.class)
    public ResponseEntity<Map<String, Object>> validationError(RuleValidationException e) {
        return error(400, e.getMessage());
    }

    @ExceptionHandler(RuleNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(RuleNotFoundException e) {
        return error(404, e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MaxUploadSizeExceededException.class})
    public ResponseEntity<Map<String, Object>> requestError(Exception e) {
        String message = "请求格式不正确";
        if (e instanceof MethodArgumentNotValidException) {
            message = ((MethodArgumentNotValidException) e).getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .sorted()
                    .collect(Collectors.joining("; "));
        }
        return error(400, message);
    }

    private ResponseEntity<Map<String, Object>> error(int status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
