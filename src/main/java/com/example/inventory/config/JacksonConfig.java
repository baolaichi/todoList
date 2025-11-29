package com.example.inventory.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    // Định dạng chuẩn ISO-8601 (Giống hệt Frontend gửi lên)
    private static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    @Bean
    @Primary // Đánh dấu đây là cấu hình ưu tiên số 1
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Module hỗ trợ Java 8 Date/Time (LocalDateTime)
        JavaTimeModule module = new JavaTimeModule();

        // Tạo Formatter chuẩn
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATETIME_FORMAT);

        // 1. Cấu hình chiều GHI (Server trả về JSON)
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));

        // 2. Cấu hình chiều ĐỌC (Frontend gửi JSON lên)
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));

        mapper.registerModule(module);

        // Tắt chế độ viết ngày tháng dạng số (Timestamp)
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return mapper;
    }
}