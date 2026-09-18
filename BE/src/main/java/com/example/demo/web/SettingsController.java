package com.example.demo.web;

import com.example.demo.dto.UpdateSettingsRequest;
import com.example.demo.dto.UserSettings;
import com.example.demo.service.SettingsService;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

	private final SettingsService settingsService;

	@GetMapping("/me")
	public UserSettings me(Principal principal) {
		return settingsService.settings(principal.getName());
	}

	@PutMapping("/me")
	public UserSettings update(@RequestBody UpdateSettingsRequest request, Principal principal) {
		return settingsService.update(principal.getName(), request.aiMaxTokens());
	}
}
