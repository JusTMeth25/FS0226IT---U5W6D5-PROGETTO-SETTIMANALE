package com.example.demo.dto;

import java.time.Instant;

/** Figures of one account. conversations counts the people with at least one message, either way. */
public record AccountStats(
		String username,
		String displayName,
		long sent,
		long received,
		long conversations,
		Instant generatedAt) {
}
