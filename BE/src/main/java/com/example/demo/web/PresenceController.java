package com.example.demo.web;

import com.example.demo.service.ChatNotifier;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/presence")
@RequiredArgsConstructor
public class PresenceController {

	private final ChatNotifier notifier;

	/** Who has an open channel right now. The client then keeps up through /topic/presence. */
	@GetMapping
	public List<String> online(Principal principal) {
		return notifier.onlineUsers().stream()
				.filter(username -> !username.equals(principal.getName()))
				.sorted()
				.toList();
	}
}
