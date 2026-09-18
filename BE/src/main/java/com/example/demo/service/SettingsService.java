package com.example.demo.service;

import com.example.demo.dto.UserSettings;
import com.example.demo.exception.BadRequestException;
import com.example.demo.model.AppUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Per-user preferences. For now only the token limit of the AI suggestion. */
@Service
public class SettingsService {

	private final AppUserService userService;
	private final int defaultMaxTokens;
	private final int minMaxTokens;
	private final int maxMaxTokens;

	public SettingsService(
			AppUserService userService,
			@Value("${openrouter.max-tokens.default:1500}") int defaultMaxTokens,
			@Value("${openrouter.max-tokens.min:1000}") int minMaxTokens,
			@Value("${openrouter.max-tokens.max:4000}") int maxMaxTokens) {
		this.userService = userService;
		this.defaultMaxTokens = defaultMaxTokens;
		this.minMaxTokens = minMaxTokens;
		this.maxMaxTokens = maxMaxTokens;
	}

	@Transactional(readOnly = true)
	public UserSettings settings(String username) {
		return toDto(userService.require(username));
	}

	@Transactional
	public UserSettings update(String username, Integer aiMaxTokens) {
		// Null resets to the application default.
		if (aiMaxTokens != null && (aiMaxTokens < minMaxTokens || aiMaxTokens > maxMaxTokens)) {
			throw new BadRequestException(
					"Il limite di token deve essere tra " + minMaxTokens + " e " + maxMaxTokens);
		}
		AppUser user = userService.require(username);
		user.setAiMaxTokens(aiMaxTokens);
		return toDto(user);
	}

	/** The limit to send to the model for this user, within the allowed range. */
	public int aiMaxTokens(AppUser user) {
		Integer chosen = user.getAiMaxTokens();
		if (chosen == null) {
			return defaultMaxTokens;
		}
		return Math.clamp(chosen, minMaxTokens, maxMaxTokens);
	}

	private UserSettings toDto(AppUser user) {
		return new UserSettings(aiMaxTokens(user), defaultMaxTokens, minMaxTokens, maxMaxTokens);
	}
}
