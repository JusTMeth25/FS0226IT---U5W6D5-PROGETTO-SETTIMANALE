package com.example.demo.service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.stereotype.Component;

/**
 * Writes one structured line per chat event on the AUDIT logger, so the log file can be
 * filtered by event, user or message id. Only metadata is recorded: never message
 * content, never passwords.
 */
@Component
public class AuditLogger {

	private static final Logger log = LoggerFactory.getLogger("AUDIT");

	public void loginSuccess(String username) {
		event("LOGIN_OK").addKeyValue("username", username).log("Login succeeded");
	}

	/** The username is whatever the client typed, so it may not exist. */
	public void loginFailure(String username) {
		log.atWarn()
				.addKeyValue("event", "LOGIN_FAIL")
				.addKeyValue("username", username)
				.log("Login failed");
	}

	public void logout(String username) {
		event("LOGOUT").addKeyValue("username", username).log("Logout");
	}

	public void connected(String username, String sessionId) {
		event("WS_CONNECT")
				.addKeyValue("username", username)
				.addKeyValue("sessionId", sessionId)
				.log("Channel opened");
	}

	/** wentOffline is false when the user still has another tab open. */
	public void disconnected(String username, String sessionId, boolean wentOffline) {
		event("WS_DISCONNECT")
				.addKeyValue("username", username)
				.addKeyValue("sessionId", sessionId)
				.addKeyValue("wentOffline", wentOffline)
				.log("Channel closed");
	}

	public void messageSent(Long messageId, String sender, String recipient) {
		event("MSG_SENT")
				.addKeyValue("messageId", messageId)
				.addKeyValue("sender", sender)
				.addKeyValue("recipient", recipient)
				.log("Message stored");
	}

	public void messagesDelivered(List<Long> messageIds, String sender, String recipient) {
		event("MSG_DELIVERED")
				.addKeyValue("messageIds", messageIds.toString())
				.addKeyValue("count", messageIds.size())
				.addKeyValue("sender", sender)
				.addKeyValue("recipient", recipient)
				.log("Messages delivered");
	}

	public void messagesRead(List<Long> messageIds, String sender, String reader) {
		event("MSG_READ")
				.addKeyValue("messageIds", messageIds.toString())
				.addKeyValue("count", messageIds.size())
				.addKeyValue("sender", sender)
				.addKeyValue("recipient", reader)
				.log("Messages read");
	}

	/** The suggested text is not logged, like any message content. */
	public void aiSuggestion(String username, String partner) {
		event("AI_SUGGESTION")
				.addKeyValue("username", username)
				.addKeyValue("partner", partner)
				.log("Reply suggested");
	}

	public void statsEmailed(String username) {
		event("STATS_EMAILED").addKeyValue("username", username).log("Account stats emailed");
	}

	private LoggingEventBuilder event(String name) {
		return log.atInfo().addKeyValue("event", name);
	}
}
