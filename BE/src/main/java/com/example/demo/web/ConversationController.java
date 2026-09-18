package com.example.demo.web;

import com.example.demo.dto.ConversationSummary;
import com.example.demo.service.ChatService;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

	private final ChatService chatService;

	/**
	 * Everything the contact list needs in one request: the people the user can talk to,
	 * the latest message of each conversation and the unread count.
	 */
	@GetMapping
	public List<ConversationSummary> list(Principal principal) {
		return chatService.conversations(principal.getName());
	}
}
