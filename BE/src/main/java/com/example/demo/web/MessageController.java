package com.example.demo.web;

import com.example.demo.dto.MessageDto;
import com.example.demo.dto.ReceiptUpdate;
import com.example.demo.model.MessageStatus;
import com.example.demo.service.AuditLogger;
import com.example.demo.service.ChatNotifier;
import com.example.demo.service.ChatService;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

	private final ChatService chatService;
	private final ChatNotifier notifier;
	private final AuditLogger audit;

	/**
	 * The stored history with one other user, in the order decided by the server.
	 * The client merges it with what arrives on the open channel, using the id
	 * to drop duplicates.
	 */
	@GetMapping("/{username}")
	public List<MessageDto> history(@PathVariable String username, Principal principal) {
		return chatService.history(principal.getName(), username);
	}

	/**
	 * Marks the conversation with that user as read. The unread count then survives a
	 * reload, because it is computed from the stored messages, and the author sees the
	 * ticks move straight away.
	 */
	@PostMapping("/{username}/read")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void markRead(@PathVariable String username, Principal principal) {
		List<Long> ids = chatService.markRead(principal.getName(), username);
		if (ids.isEmpty()) {
			return;
		}
		String author = username.trim().toLowerCase();
		audit.messagesRead(ids, author, principal.getName());
		notifier.sendReceipt(author, new ReceiptUpdate(
				MessageStatus.READ,
				principal.getName(),
				ids,
				Instant.now()));
	}
}
