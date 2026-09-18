package com.example.demo.dto;

/** The user's AI token limit, with the range and the default the client needs to draw the control. */
public record UserSettings(int aiMaxTokens, int defaultMaxTokens, int minMaxTokens, int maxMaxTokens) {
}
