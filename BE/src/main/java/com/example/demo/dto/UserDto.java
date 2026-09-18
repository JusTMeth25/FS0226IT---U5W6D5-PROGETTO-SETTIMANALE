package com.example.demo.dto;

import com.example.demo.model.AppUser;

public record UserDto(Long id, String username, String displayName) {

	public static UserDto from(AppUser user) {
		return new UserDto(user.getId(), user.getUsername(), user.getDisplayName());
	}
}
