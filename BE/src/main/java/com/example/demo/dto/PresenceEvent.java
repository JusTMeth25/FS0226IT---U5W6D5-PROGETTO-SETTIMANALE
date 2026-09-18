package com.example.demo.dto;

/** Broadcast when someone opens or closes their channel. Carries no message content. */
public record PresenceEvent(String username, boolean online) {
}
