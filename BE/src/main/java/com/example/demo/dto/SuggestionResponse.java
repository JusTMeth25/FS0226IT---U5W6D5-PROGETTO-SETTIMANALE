package com.example.demo.dto;

/** A proposed next message. It is never stored: the user decides whether to send it. */
public record SuggestionResponse(String suggestion) {
}
