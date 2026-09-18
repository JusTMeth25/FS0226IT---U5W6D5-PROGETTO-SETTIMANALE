package com.example.demo.model;

/** Where a message is on its way to the recipient. */
public enum MessageStatus {

	/** Stored by the server. The recipient had no connected session yet. */
	SENT,

	/** Handed to at least one connected session of the recipient. */
	DELIVERED,

	/** The recipient opened the conversation. */
	READ
}
