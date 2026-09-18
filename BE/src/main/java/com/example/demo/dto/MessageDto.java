package com.example.demo.dto;

import com.example.demo.model.Message;
import com.example.demo.model.MessageStatus;
import java.time.Instant;

public record MessageDto(
		Long id,
		String senderUsername,
		String recipientUsername,
		String content,
		Instant sentAt,
		MessageStatus status) {

	public static MessageDto from(Message message) {
		return new MessageDto(
				message.getId(),
				message.getSender().getUsername(),
				message.getRecipient().getUsername(),
				message.getContent(),
				message.getSentAt(),
				message.status());
	}
}
