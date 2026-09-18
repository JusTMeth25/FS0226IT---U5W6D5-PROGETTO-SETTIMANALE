package com.example.demo.service;

import com.example.demo.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

	private final AppUserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String username) {
		// Usernames are stored lowercase at registration time, so the login is case insensitive.
		return userRepository.findByUsername(username.trim().toLowerCase())
				.map(user -> User.withUsername(user.getUsername())
						.password(user.getPassword())
						.roles("USER")
						.build())
				.orElseThrow(() -> new UsernameNotFoundException("Utente non trovato: " + username));
	}
}
