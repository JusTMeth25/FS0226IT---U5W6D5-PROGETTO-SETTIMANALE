package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AppUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 32)
	private String username;

	@Column(nullable = false, length = 64)
	private String displayName;

	/** BCrypt hash. Never exposed through a DTO. */
	@Column(nullable = false)
	private String password;

	@Column(nullable = false)
	private Instant createdAt;

	/**
	 * max_tokens of each AI suggestion, chosen by the user. Null means the application
	 * default, so existing rows need no migration.
	 */
	@Column(name = "ai_max_tokens")
	private Integer aiMaxTokens;
}
