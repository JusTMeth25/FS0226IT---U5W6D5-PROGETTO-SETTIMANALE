package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Kept apart from SecurityConfig: AppUserService needs the encoder and SecurityConfig
 * needs AppUserService, so declaring both in the same class would be a cycle.
 */
@Configuration
public class CryptoConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
