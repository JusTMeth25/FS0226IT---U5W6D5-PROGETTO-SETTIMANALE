package com.example.demo.web;

import com.example.demo.dto.AccountStats;
import com.example.demo.dto.StatsEmailResponse;
import com.example.demo.service.AuditLogger;
import com.example.demo.service.MailService;
import com.example.demo.service.StatsService;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

	private final StatsService statsService;
	private final MailService mailService;
	private final AuditLogger audit;

	/** Messages sent, messages received and open chats of the current user. */
	@GetMapping("/me")
	public AccountStats me(Principal principal) {
		return statsService.stats(principal.getName());
	}

	/** Emails the same figures, rendered with the Thymeleaf template, to MAIL_USERNAME. */
	@PostMapping("/me/email")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public StatsEmailResponse email(Principal principal) {
		String sentTo = mailService.sendStats(statsService.stats(principal.getName()));
		audit.statsEmailed(principal.getName());
		return new StatsEmailResponse(sentTo);
	}
}
