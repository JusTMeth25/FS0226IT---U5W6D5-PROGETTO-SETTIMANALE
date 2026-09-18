package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload sent by the client over STOMP. It carries no sender field on purpose:
 * the sender is taken from the session Principal.
 */
public record SendMessageRequest(
		@NotBlank String recipientUsername,
		@NotBlank @Size(max = 2000) String content) {
}
