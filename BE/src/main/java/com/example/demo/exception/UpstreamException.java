package com.example.demo.exception;

/** An external service (the LLM provider, the mail server) failed or answered badly. */
public class UpstreamException extends RuntimeException {

	public UpstreamException(String message, Throwable cause) {
		super(message, cause);
	}

	public UpstreamException(String message) {
		super(message);
	}
}
