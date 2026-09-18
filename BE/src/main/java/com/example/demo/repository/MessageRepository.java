package com.example.demo.repository;

import com.example.demo.model.Message;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {

	/** One row per conversation partner, with how many of their messages are still unread. */
	interface UnreadCount {
		Long getSenderId();

		long getTotal();
	}

	/**
	 * The one-to-one conversation between two users, in both directions.
	 * The id breaks ties so that the order is the same for both sides.
	 */
	@Query("""
			SELECT m FROM Message m
			WHERE (m.sender.id = :a AND m.recipient.id = :b)
			   OR (m.sender.id = :b AND m.recipient.id = :a)
			ORDER BY m.sentAt ASC, m.id ASC
			""")
	List<Message> findConversation(@Param("a") Long a, @Param("b") Long b);

	/**
	 * The latest message of every conversation the user takes part in, in one query.
	 * The id is monotonic, so the highest id per partner is the most recent message.
	 */
	@Query("""
			SELECT m FROM Message m
			WHERE m.id IN (
			    SELECT MAX(m2.id) FROM Message m2
			    WHERE m2.sender.id = :me OR m2.recipient.id = :me
			    GROUP BY CASE WHEN m2.sender.id = :me THEN m2.recipient.id ELSE m2.sender.id END
			)
			""")
	List<Message> findLatestPerConversation(@Param("me") Long me);

	/** How many messages the user has not read yet, grouped by who sent them. */
	@Query("""
			SELECT m.sender.id AS senderId, COUNT(m) AS total FROM Message m
			WHERE m.recipient.id = :me AND m.readAt IS NULL
			GROUP BY m.sender.id
			""")
	List<UnreadCount> countUnreadBySender(@Param("me") Long me);

	/**
	 * Ids of the messages one person sent to the user and that are still unread.
	 * Read before the update, so the sender can be told exactly which ones changed.
	 */
	@Query("""
			SELECT m.id FROM Message m
			WHERE m.recipient.id = :me AND m.sender.id = :other AND m.readAt IS NULL
			ORDER BY m.id ASC
			""")
	List<Long> findUnreadIds(@Param("me") Long me, @Param("other") Long other);

	/** Everything the user received and that no session of theirs has taken yet. */
	@Query("""
			SELECT m FROM Message m
			WHERE m.recipient.id = :me AND m.deliveredAt IS NULL
			ORDER BY m.id ASC
			""")
	List<Message> findUndelivered(@Param("me") Long me);

	long countBySenderId(Long senderId);

	long countByRecipientId(Long recipientId);

	/** How many people the user has exchanged at least one message with, in either direction. */
	@Query("""
			SELECT COUNT(DISTINCT CASE WHEN m.sender.id = :me THEN m.recipient.id ELSE m.sender.id END)
			FROM Message m
			WHERE m.sender.id = :me OR m.recipient.id = :me
			""")
	long countConversations(@Param("me") Long me);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE Message m SET m.readAt = :now WHERE m.id IN :ids AND m.readAt IS NULL")
	int markReadByIds(@Param("ids") List<Long> ids, @Param("now") Instant now);

	/** Reading also implies delivery, for a message that skipped the delivered step. */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE Message m SET m.deliveredAt = :now WHERE m.id IN :ids AND m.deliveredAt IS NULL")
	int markDeliveredByIds(@Param("ids") List<Long> ids, @Param("now") Instant now);
}
