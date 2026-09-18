package com.example.demo.exception;

/** A feature that depends on configuration that is missing, such as an API key. */
public class ServiceUnavailableException extends RuntimeException {

	public ServiceUnavailableException(String message) {
		super(message);
	}
}
