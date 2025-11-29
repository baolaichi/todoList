package com.example.inventory.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Bắt lỗi JSON không đọc được (Sai ngày tháng, sai kiểu dữ liệu...)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleJsonError(HttpMessageNotReadableException ex) {
        // IN LỖI RA CONSOLE ĐỂ BẠN THẤY "Ý ỚI"
        System.err.println("========== LỖI JSON PARSE ==========");
        ex.printStackTrace();
        System.err.println("====================================");

        Map<String, String> error = new HashMap<>();
        error.put("error", "Lỗi định dạng dữ liệu (JSON Parse)");
        error.put("message", ex.getMessage()); // Đọc dòng này để biết sai ở đâu

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}