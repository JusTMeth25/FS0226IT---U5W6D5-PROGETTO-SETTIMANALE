package com.example.demo.service;

import com.example.demo.dto.AccountStats;
import com.example.demo.model.AppUser;
import com.example.demo.repository.MessageRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StatsService {

	private final AppUserService userService;
	private final MessageRepository messageRepository;

	/** Three count queries on the stored messages; nothing is cached. */
	@Transactional(readOnly = true)
	public AccountStats stats(String username) {
		AppUser user = userService.require(username);

		return new AccountStats(
				user.getUsername(),
				user.getDisplayName(),
				messageRepository.countBySenderId(user.getId()),
				messageRepository.countByRecipientId(user.getId()),
				messageRepository.countConversations(user.getId()),
				Instant.now());
	}
}
