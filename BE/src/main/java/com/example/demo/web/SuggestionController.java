package com.example.demo.web;

import com.example.demo.dto.SuggestionResponse;
import com.example.demo.service.AuditLogger;
import com.example.demo.service.SuggestionService;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class SuggestionController {

	private final SuggestionService suggestionService;
	private final AuditLogger audit;

	/**
	 * A proposed next message for the conversation with that user. It goes back to the
	 * client only: nothing is written to the database.
	 */
	@PostMapping("/suggestion/{username}")
	public SuggestionResponse suggest(@PathVariable String username, Principal principal) {
		String suggestion = suggestionService.suggest(principal.getName(), username);
		audit.aiSuggestion(principal.getName(), username.trim().toLowerCase());
		return new SuggestionResponse(suggestion);
	}
}
