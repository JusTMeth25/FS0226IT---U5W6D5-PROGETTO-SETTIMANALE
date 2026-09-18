package com.example.demo.service;

import com.example.demo.dto.MessageDto;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.UpstreamException;
import com.example.demo.model.AppUser;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Proposes the next message of a conversation. It only reads the history: the suggestion
 * is returned to the client and never stored.
 */
@Service
public class SuggestionService {

	private static final int MAX_LENGTH = 2000;

	private final ChatService chatService;
	private final AppUserService userService;
	private final OpenRouterClient openRouter;
	private final SettingsService settingsService;
	private final int contextMessages;

	public SuggestionService(
			ChatService chatService,
			AppUserService userService,
			OpenRouterClient openRouter,
			SettingsService settingsService,
			@Value("${openrouter.context-messages:20}") int contextMessages) {
		this.chatService = chatService;
		this.userService = userService;
		this.openRouter = openRouter;
		this.settingsService = settingsService;
		this.contextMessages = contextMessages;
	}

	public String suggest(String currentUsername, String partnerUsername) {
		AppUser me = userService.require(currentUsername);
		AppUser partner = userService.require(partnerUsername.trim().toLowerCase());
		if (me.getId().equals(partner.getId())) {
			throw new BadRequestException("Non puoi chiedere un suggerimento per una chat con te stesso");
		}

		List<MessageDto> history = chatService.history(me.getUsername(), partner.getUsername());
		List<MessageDto> recent = history.subList(Math.max(0, history.size() - contextMessages), history.size());

		List<OpenRouterClient.ChatMessage> prompt = new ArrayList<>();
		boolean meHasWritten = recent.stream().anyMatch(message -> message.senderUsername().equals(me.getUsername()));
		prompt.add(new OpenRouterClient.ChatMessage("system",
				systemPrompt(me, partner, recent.isEmpty(), meHasWritten)));
		// The model writes as the current user: their own messages are the assistant
		// turns, the partner's are the user turns.
		for (MessageDto message : recent) {
			String role = message.senderUsername().equals(me.getUsername()) ? "assistant" : "user";
			prompt.add(new OpenRouterClient.ChatMessage(role, message.content()));
		}
		if (recent.isEmpty() || recent.getLast().senderUsername().equals(me.getUsername())) {
			// Models expect the last turn to be the user's: ask explicitly for the next line.
			prompt.add(new OpenRouterClient.ChatMessage("user",
					"[Scrivi il prossimo messaggio di " + me.getDisplayName() + ".]"));
		}

		String suggestion = clean(openRouter.complete(prompt, settingsService.aiMaxTokens(me), 0.7));
		if (suggestion.isEmpty()) {
			throw new UpstreamException("Il servizio AI non ha restituito un testo");
		}
		return suggestion;
	}

	/**
	 * The suggestion follows the language the current user writes in. When they have not
	 * written yet it follows the partner, and an empty conversation falls back to Italian.
	 */
	private static String systemPrompt(AppUser me, AppUser partner, boolean empty, boolean meHasWritten) {
		String prompt = """
				Aiuti %1$s a rispondere in una chat privata con %2$s.
				Scrivi solo il prossimo messaggio di %1$s: naturale, breve (al massimo due frasi), \
				con lo stesso tono della conversazione.
				Niente virgolette, niente prefissi come "%1$s:", niente spiegazioni.
				""".formatted(me.getDisplayName(), partner.getDisplayName());
		if (empty) {
			prompt += "La conversazione è ancora vuota: proponi un messaggio di apertura cordiale, in italiano.\n";
		} else if (meHasWritten) {
			prompt += ("Lingua: scrivi nella stessa lingua che %1$s usa nei propri messaggi più recenti "
					+ "(i turni assistant), anche se %2$s scrive in un'altra lingua.\n")
					.formatted(me.getDisplayName(), partner.getDisplayName());
		} else {
			prompt += "Lingua: %1$s non ha ancora scritto, quindi usa la lingua dei messaggi di %2$s.\n"
					.formatted(me.getDisplayName(), partner.getDisplayName());
		}
		return prompt;
	}

	/** Trims, drops surrounding quotes and keeps the message size limit. */
	private static String clean(String text) {
		String result = text.strip();
		if (result.length() >= 2
				&& ((result.startsWith("\"") && result.endsWith("\""))
						|| (result.startsWith("“") && result.endsWith("”"))
						|| (result.startsWith("«") && result.endsWith("»")))) {
			result = result.substring(1, result.length() - 1).strip();
		}
		return result.length() > MAX_LENGTH ? result.substring(0, MAX_LENGTH) : result;
	}
}
