package com.example.demo.service;

import com.example.demo.dto.AccountStats;
import com.example.demo.exception.ServiceUnavailableException;
import com.example.demo.exception.UpstreamException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class MailService {

	private static final DateTimeFormatter STAMP = DateTimeFormatter
			.ofPattern("dd/MM/yyyy HH:mm")
			.withZone(ZoneId.of("Europe/Rome"));

	private final JavaMailSender mailSender;
	private final SpringTemplateEngine templateEngine;
	private final String mailbox;

	public MailService(
			JavaMailSender mailSender,
			SpringTemplateEngine templateEngine,
			@Value("${spring.mail.username:}") String mailbox) {
		this.mailSender = mailSender;
		this.templateEngine = templateEngine;
		this.mailbox = mailbox;
	}

	/**
	 * Renders the stats template and sends it. The address is always MAIL_USERNAME, for
	 * every user: it is both the sender and the recipient. Returns that address.
	 */
	public String sendStats(AccountStats stats) {
		if (mailbox == null || mailbox.isBlank()) {
			throw new ServiceUnavailableException("Email non configurata: imposta MAIL_USERNAME e MAIL_PASSWORD");
		}

		Context context = new Context(Locale.ITALIAN);
		context.setVariable("stats", stats);
		context.setVariable("generatedAt", STAMP.format(stats.generatedAt()));
		String html = templateEngine.process("mail/account-stats", context);

		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
			helper.setFrom(mailbox);
			helper.setTo(mailbox);
			helper.setSubject("Zaffiro — le statistiche di " + stats.displayName());
			helper.setText(html, true);
			mailSender.send(message);
		} catch (MailAuthenticationException exception) {
			throw new UpstreamException(
					"Il server email ha rifiutato le credenziali: controlla MAIL_USERNAME e MAIL_PASSWORD (App Password Gmail)",
					exception);
		} catch (MessagingException | MailException exception) {
			throw new UpstreamException("Invio email non riuscito", exception);
		}
		return mailbox;
	}
}
