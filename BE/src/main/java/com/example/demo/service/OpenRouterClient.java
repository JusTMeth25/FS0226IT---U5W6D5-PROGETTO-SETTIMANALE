package com.example.demo.service;

import com.example.demo.exception.ServiceUnavailableException;
import com.example.demo.exception.UpstreamException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Minimal client for the OpenRouter chat completions endpoint, which follows the OpenAI
 * format. Only what the suggestion needs: one request, the first choice back.
 */
@Component
public class OpenRouterClient {

	public record ChatMessage(String role, String content) {
	}

	record ChatRequest(
			String model,
			List<ChatMessage> messages,
			@JsonProperty("max_tokens") int maxTokens,
			double temperature,
			Reasoning reasoning) {
	}

	/** OpenRouter's unified reasoning switch. The thinking comes back apart from the content. */
	record Reasoning(boolean enabled) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record ChatResponse(List<Choice> choices) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Choice(ChatMessage message, @JsonProperty("finish_reason") String finishReason) {
	}

	private final RestClient restClient;
	private final String apiKey;
	private final String model;
	private final boolean reasoning;

	public OpenRouterClient(
			RestClient.Builder builder,
			@Value("${openrouter.base-url}") String baseUrl,
			@Value("${openrouter.api-key:}") String apiKey,
			@Value("${openrouter.model}") String model,
			@Value("${openrouter.reasoning:true}") boolean reasoning) {
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
				HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
		// Free reasoning models can queue for a while before answering.
		requestFactory.setReadTimeout(Duration.ofSeconds(90));

		this.restClient = builder
				.baseUrl(baseUrl)
				.requestFactory(requestFactory)
				// Optional headers OpenRouter uses to attribute the traffic to an app.
				.defaultHeader("HTTP-Referer", "http://localhost:5173")
				.defaultHeader("X-Title", "Zaffiro")
				.build();
		this.apiKey = apiKey;
		this.model = model;
		this.reasoning = reasoning;
	}

	/** Sends the conversation and returns the text of the first choice. */
	public String complete(List<ChatMessage> messages, int maxTokens, double temperature) {
		if (apiKey == null || apiKey.isBlank()) {
			throw new ServiceUnavailableException("Suggerimenti AI non configurati: imposta OPENROUTER_API_KEY");
		}

		ChatResponse response;
		try {
			response = restClient.post()
					.uri("/chat/completions")
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
					.contentType(MediaType.APPLICATION_JSON)
					.body(new ChatRequest(model, messages, maxTokens, temperature, new Reasoning(reasoning)))
					.retrieve()
					.body(ChatResponse.class);
		} catch (RestClientResponseException exception) {
			throw new UpstreamException(
					"Il servizio AI ha risposto con errore " + exception.getStatusCode().value(), exception);
		} catch (RestClientException exception) {
			throw new UpstreamException("Servizio AI non raggiungibile", exception);
		}

		if (response == null || response.choices() == null || response.choices().isEmpty()
				|| response.choices().getFirst().message() == null
				|| response.choices().getFirst().message().content() == null) {
			throw new UpstreamException("Il servizio AI non ha restituito un testo");
		}
		// Cut off by max_tokens: with reasoning on, the content may be half-finished thinking.
		if ("length".equals(response.choices().getFirst().finishReason())) {
			throw new UpstreamException("Il servizio AI ha interrotto la risposta, riprova");
		}
		return response.choices().getFirst().message().content();
	}
}
