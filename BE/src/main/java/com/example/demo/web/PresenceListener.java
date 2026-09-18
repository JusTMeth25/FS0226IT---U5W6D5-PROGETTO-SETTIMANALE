package com.example.demo.web;

import com.example.demo.dto.PresenceEvent;
import com.example.demo.dto.ReceiptUpdate;
import com.example.demo.model.MessageStatus;
import com.example.demo.service.AuditLogger;
import com.example.demo.service.ChatNotifier;
import com.example.demo.service.ChatService;
import java.security.Principal;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Turns channel lifecycle into two things: presence for everyone, and delivery
 * receipts for whoever wrote to the user while they were away.
 */
@Component
@RequiredArgsConstructor
public class PresenceListener {

	private final ChatService chatService;
	private final ChatNotifier notifier;
	private final SimpUserRegistry userRegistry;
	private final AuditLogger audit;

	@EventListener
	public void onConnected(SessionConnectedEvent event) {
		Principal principal = event.getUser();
		if (principal == null) {
			return;
		}
		String username = principal.getName();
		audit.connected(username, StompHeaderAccessor.wrap(event.getMessage()).getSessionId());

		notifier.broadcastPresence(new PresenceEvent(username, true));

		// Anything stored while this user was offline is delivered now.
		for (ChatService.ReceiptBatch batch : chatService.markAllDelivered(username)) {
			audit.messagesDelivered(batch.messageIds(), batch.senderUsername(), username);
			notifier.sendReceipt(batch.senderUsername(), new ReceiptUpdate(
					MessageStatus.DELIVERED,
					username,
					batch.messageIds(),
					Instant.now()));
		}
	}

	@EventListener
	public void onDisconnect(SessionDisconnectEvent event) {
		Principal principal = event.getUser();
		if (principal == null) {
			return;
		}
		String username = principal.getName();
		String closingSessionId = StompHeaderAccessor.wrap(event.getMessage()).getSessionId();

		// A user can have several tabs open: they go offline only with the last one.
		SimpUser user = userRegistry.getUser(username);
		boolean stillConnected = user != null && user.getSessions().stream()
				.anyMatch(session -> !session.getId().equals(closingSessionId));
		audit.disconnected(username, closingSessionId, !stillConnected);

		if (!stillConnected) {
			notifier.broadcastPresence(new PresenceEvent(username, false));
		}
	}
}
