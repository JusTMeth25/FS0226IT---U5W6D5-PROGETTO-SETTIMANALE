package com.example.demo.dto;

/** aiMaxTokens null resets the limit to the application default. The range is checked by the service. */
public record UpdateSettingsRequest(Integer aiMaxTokens) {
}
