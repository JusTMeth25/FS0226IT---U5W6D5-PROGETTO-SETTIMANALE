package com.example.demo.service;

import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.UserDto;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.model.AppUser;
import com.example.demo.repository.AppUserRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppUserService {

	private final AppUserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public UserDto register(RegisterRequest request) {
		String username = request.username().trim().toLowerCase();
		if (userRepository.existsByUsername(username)) {
			throw new ConflictException("Username già in uso: " + username);
		}

		AppUser user = new AppUser();
		user.setUsername(username);
		user.setDisplayName(request.displayName().trim());
		user.setPassword(passwordEncoder.encode(request.password()));
		user.setCreatedAt(Instant.now());

		return UserDto.from(userRepository.save(user));
	}

	/** The list of people the current user can start a conversation with. */
	@Transactional(readOnly = true)
	public List<UserDto> listOthers(String currentUsername) {
		return userRepository.findByUsernameNotOrderByDisplayNameAsc(currentUsername)
				.stream()
				.map(UserDto::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public UserDto findByUsername(String username) {
		return UserDto.from(require(username));
	}

	public AppUser require(String username) {
		return userRepository.findByUsername(username)
				.orElseThrow(() -> new NotFoundException("Utente non trovato: " + username));
	}
}
