package com.example.demo.dto;

import com.example.demo.model.MessageStatus;
import java.time.Instant;
import java.util.List;

/**
 * Sent to the author of the messages when their state changes, so the ticks move
 * without the client asking. Never carries the content, only the ids.
 */
public record ReceiptUpdate(
		MessageStatus status,
		/** The other side of the conversation, from the recipient of this update. */
		String withUsername,
		List<Long> messageIds,
		Instant at) {
}
