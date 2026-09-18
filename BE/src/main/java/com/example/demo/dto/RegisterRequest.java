package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank @Size(min = 3, max = 32) String username,
		@NotBlank @Size(max = 64) String displayName,
		@NotBlank @Size(min = 6, max = 72) String password) {
}
