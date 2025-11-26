package com.example.inventory.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Đây là URL để Frontend kết nối vào: ws://localhost:8080/ws
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Cho phép mọi nguồn kết nối
                .withSockJS(); // Hỗ trợ trình duyệt cũ
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Các tin nhắn bắt đầu bằng /topic sẽ được gửi về Client
        registry.enableSimpleBroker("/topic");
        // Các tin nhắn từ Client gửi lên bắt đầu bằng /app (nếu dùng socket 2 chiều)
        registry.setApplicationDestinationPrefixes("/app");
    }
}
