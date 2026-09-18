package com.example.demo.service;

import com.example.demo.dto.ConversationSummary;
import com.example.demo.dto.MessageDto;
import com.example.demo.dto.SendMessageRequest;
import com.example.demo.dto.UserDto;
import com.example.demo.exception.BadRequestException;
import com.example.demo.model.AppUser;
import com.example.demo.model.Message;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.MessageRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {

	/** The messages of one author whose state just changed. */
	public record ReceiptBatch(String senderUsername, List<Long> messageIds) {
	}

	private final MessageRepository messageRepository;
	private final AppUserService userService;
	private final AppUserRepository userRepository;

	/**
	 * Persists a message and returns it with the id and the instant assigned by the server.
	 * The sender comes from the session Principal, never from the client payload.
	 * Nothing is published on the broker here: delivery happens only after this
	 * transaction has committed.
	 */
	@Transactional
	public MessageDto send(String senderUsername, SendMessageRequest request) {
		AppUser sender = userService.require(senderUsername);
		AppUser recipient = userService.require(request.recipientUsername().trim().toLowerCase());

		if (sender.getId().equals(recipient.getId())) {
			throw new BadRequestException("Non puoi inviare un messaggio a te stesso");
		}

		Message message = new Message();
		message.setSender(sender);
		message.setRecipient(recipient);
		message.setContent(request.content());
		message.setSentAt(Instant.now());

		return MessageDto.from(messageRepository.save(message));
	}

	/**
	 * The contact list in one round trip: every person the user can talk to, the latest
	 * message of each conversation and the unread count. Two queries in total, instead of
	 * one history request per contact.
	 */
	@Transactional(readOnly = true)
	public List<ConversationSummary> conversations(String currentUsername) {
		AppUser me = userService.require(currentUsername);

		Map<Long, MessageDto> latestByPartner = new HashMap<>();
		for (Message message : messageRepository.findLatestPerConversation(me.getId())) {
			Long partnerId = message.getSender().getId().equals(me.getId())
					? message.getRecipient().getId()
					: message.getSender().getId();
			latestByPartner.put(partnerId, MessageDto.from(message));
		}

		Map<Long, Long> unreadBySender = new HashMap<>();
		for (MessageRepository.UnreadCount row : messageRepository.countUnreadBySender(me.getId())) {
			unreadBySender.put(row.getSenderId(), row.getTotal());
		}

		return userRepository.findByUsernameNotOrderByDisplayNameAsc(me.getUsername())
				.stream()
				.map(other -> new ConversationSummary(
						UserDto.from(other),
						latestByPartner.get(other.getId()),
						unreadBySender.getOrDefault(other.getId(), 0L)))
				.toList();
	}

	/**
	 * Marks as read what the user received from one person, and as delivered too, for
	 * anything read before a delivery receipt could be recorded. Returns the ids that
	 * changed, so the author can be told exactly which ticks to move.
	 */
	@Transactional
	public List<Long> markRead(String currentUsername, String otherUsername) {
		AppUser me = userService.require(currentUsername);
		AppUser other = userService.require(otherUsername.trim().toLowerCase());

		List<Long> ids = messageRepository.findUnreadIds(me.getId(), other.getId());
		if (ids.isEmpty()) {
			return List.of();
		}

		Instant now = Instant.now();
		messageRepository.markDeliveredByIds(ids, now);
		messageRepository.markReadByIds(ids, now);
		return ids;
	}

	/**
	 * Marks as delivered everything waiting for the user, grouped by author. Called when
	 * a channel opens, so messages stored while they were offline move to delivered.
	 */
	@Transactional
	public List<ReceiptBatch> markAllDelivered(String currentUsername) {
		AppUser me = userService.require(currentUsername);

		List<Message> pending = messageRepository.findUndelivered(me.getId());
		if (pending.isEmpty()) {
			return List.of();
		}

		Map<String, List<Long>> bySender = new LinkedHashMap<>();
		List<Long> ids = new ArrayList<>(pending.size());
		for (Message message : pending) {
			bySender.computeIfAbsent(message.getSender().getUsername(), key -> new ArrayList<>())
					.add(message.getId());
			ids.add(message.getId());
		}

		messageRepository.markDeliveredByIds(ids, Instant.now());

		return bySender.entrySet().stream()
				.map(entry -> new ReceiptBatch(entry.getKey(), entry.getValue()))
				.toList();
	}

	/** Marks one message as delivered, right after it reached a connected session. */
	@Transactional
	public boolean markDelivered(Long messageId) {
		return messageRepository.markDeliveredByIds(List.of(messageId), Instant.now()) > 0;
	}

	/** The stored conversation between the current user and another one, in server order. */
	@Transactional(readOnly = true)
	public List<MessageDto> history(String currentUsername, String otherUsername) {
		AppUser me = userService.require(currentUsername);
		AppUser other = userService.require(otherUsername.trim().toLowerCase());

		return messageRepository.findConversation(me.getId(), other.getId())
				.stream()
				.map(MessageDto::from)
				.toList();
	}
}
