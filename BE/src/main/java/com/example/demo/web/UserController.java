package com.example.demo.web;

import com.example.demo.dto.UserDto;
import com.example.demo.service.AppUserService;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final AppUserService userService;

	/** The people the current user can talk to. */
	@GetMapping
	public List<UserDto> list(Principal principal) {
		return userService.listOthers(principal.getName());
	}

	@GetMapping("/me")
	public UserDto me(Principal principal) {
		return userService.findByUsername(principal.getName());
	}
}
