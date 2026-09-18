package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "message", indexes = {
		@Index(name = "idx_message_conversation", columnList = "sender_id, recipient_id, sent_at"),
		@Index(name = "idx_message_unread", columnList = "recipient_id, read_at"),
		@Index(name = "idx_message_undelivered", columnList = "recipient_id, delivered_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Message {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "sender_id", nullable = false)
	private AppUser sender;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "recipient_id", nullable = false)
	private AppUser recipient;

	@Column(nullable = false, length = 2000)
	private String content;

	/** Assigned by the server when the message is persisted, never by the client. */
	@Column(name = "sent_at", nullable = false)
	private Instant sentAt;

	/** When a connected session of the recipient received it. Null while undelivered. */
	@Column(name = "delivered_at")
	private Instant deliveredAt;

	/** When the recipient opened the conversation. Null while the message is unread. */
	@Column(name = "read_at")
	private Instant readAt;

	public MessageStatus status() {
		if (readAt != null) return MessageStatus.READ;
		if (deliveredAt != null) return MessageStatus.DELIVERED;
		return MessageStatus.SENT;
	}
}
