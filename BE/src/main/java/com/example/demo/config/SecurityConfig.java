package com.example.demo.config;

import com.example.demo.dto.ApiError;
import com.example.demo.service.AppUserService;
import com.example.demo.service.AuditLogger;
import tools.jackson.databind.json.JsonMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final AppUserService userService;
	private final JsonMapper jsonMapper;
	private final AuditLogger audit;

	@Value("${app.cors.allowed-origin}")
	private String allowedOrigin;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				// Local exercise only: see the CSRF note in the README before exposing this.
				.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.anyRequest().authenticated())
				.formLogin(form -> form
						.loginProcessingUrl("/api/auth/login")
						.successHandler((request, response, authentication) -> {
							audit.loginSuccess(authentication.getName());
							writeJson(response, HttpServletResponse.SC_OK,
									userService.findByUsername(authentication.getName()));
						})
						.failureHandler((request, response, exception) -> {
							audit.loginFailure(request.getParameter("username"));
							writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
									ApiError.of(401, "Unauthorized", "Credenziali non valide"));
						}))
				.logout(logout -> logout
						.logoutUrl("/api/auth/logout")
						.logoutSuccessHandler((request, response, authentication) -> {
							if (authentication != null) {
								audit.logout(authentication.getName());
							}
							response.setStatus(HttpServletResponse.SC_NO_CONTENT);
						}))
				// Answer with 401 instead of redirecting a REST client to a login page.
				.exceptionHandling(handling -> handling
						.authenticationEntryPoint((request, response, exception) ->
								writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
										ApiError.of(401, "Unauthorized", "Autenticazione richiesta"))));

		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(List.of(allowedOrigin));
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		// The session travels in a cookie, so credentials must be allowed.
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	private void writeJson(HttpServletResponse response, int status, Object body) throws java.io.IOException {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		jsonMapper.writeValue(response.getWriter(), body);
	}
}
