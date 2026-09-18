package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	@Value("${app.cors.allowed-origin}")
	private String allowedOrigin;

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		// Plain WebSocket, no SockJS. The handshake is an HTTP request, so it carries
		// the JSESSIONID cookie and Spring binds the session Principal to the STOMP session.
		registry.addEndpoint("/ws").setAllowedOrigins(allowedOrigin);
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		// /queue carries messages and receipts through user destinations; /topic only
		// carries presence, which is not message content.
		registry.enableSimpleBroker("/queue", "/topic");
		registry.setApplicationDestinationPrefixes("/app");
		// User destinations: convertAndSendToUser resolves /user/queue/... on the
		// sessions of that single user, so a message is never broadcast.
		registry.setUserDestinationPrefix("/user");
	}
}
