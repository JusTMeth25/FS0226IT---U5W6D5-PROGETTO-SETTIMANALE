package com.example.demo.service;

import com.example.demo.dto.ApiError;
import com.example.demo.dto.MessageDto;
import com.example.demo.dto.PresenceEvent;
import com.example.demo.dto.ReceiptUpdate;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;

/**
 * Every outbound frame goes through here, so the destinations live in one place.
 * Messages and receipts use user destinations and reach one person; only presence,
 * which carries no message content, is a broadcast.
 */
@Component
@RequiredArgsConstructor
public class ChatNotifier {

	public static final String MESSAGES_QUEUE = "/queue/messages";
	public static final String RECEIPTS_QUEUE = "/queue/receipts";
	public static final String ERRORS_QUEUE = "/queue/errors";
	public static final String PRESENCE_TOPIC = "/topic/presence";

	private final SimpMessagingTemplate messagingTemplate;
	private final SimpUserRegistry userRegistry;

	/** True when the user has at least one open channel. */
	public boolean isOnline(String username) {
		return userRegistry.getUser(username) != null;
	}

	public Set<String> onlineUsers() {
		return userRegistry.getUsers().stream()
				.map(user -> user.getName())
				.collect(Collectors.toSet());
	}

	/** Delivers the message to the recipient, then echoes it to its author. */
	public void sendMessage(MessageDto message) {
		messagingTemplate.convertAndSendToUser(message.recipientUsername(), MESSAGES_QUEUE, message);
		messagingTemplate.convertAndSendToUser(message.senderUsername(), MESSAGES_QUEUE, message);
	}

	/** Tells the author of those messages that their state moved. */
	public void sendReceipt(String toUsername, ReceiptUpdate receipt) {
		messagingTemplate.convertAndSendToUser(toUsername, RECEIPTS_QUEUE, receipt);
	}

	public void sendError(String toUsername, ApiError error) {
		messagingTemplate.convertAndSendToUser(toUsername, ERRORS_QUEUE, error);
	}

	public void broadcastPresence(PresenceEvent event) {
		messagingTemplate.convertAndSend(PRESENCE_TOPIC, event);
	}
}
