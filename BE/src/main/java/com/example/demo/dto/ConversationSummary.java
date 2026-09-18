package com.example.demo.dto;

/**
 * One row of the contact list: who the other person is, the latest message of the
 * conversation, and how many of their messages are still unread.
 */
public record ConversationSummary(UserDto user, MessageDto lastMessage, long unreadCount) {
}
