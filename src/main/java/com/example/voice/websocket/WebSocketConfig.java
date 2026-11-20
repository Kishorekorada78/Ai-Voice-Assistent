package com.example.voice.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer{
	
	private final SttWebSocketHandler handler;
	
	public WebSocketConfig(SttWebSocketHandler handler) {
		this.handler=handler;
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(handler, "/ws/stt").setAllowedOriginPatterns("*");   // fine for learning only . replace "*" with your real domain. 
		
	}
	
	// Default buffers are small; audio sentences need more room
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxBinaryMessageBufferSize(1024 * 1024);   // 1 MB 
        container.setMaxTextMessageBufferSize(8 * 1024);        // 8 kB
        return container;
    }

}
