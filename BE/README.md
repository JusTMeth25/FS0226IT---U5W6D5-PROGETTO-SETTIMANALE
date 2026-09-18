# Mini WhatsApp — Backend

Backend of a minimal one-to-one chat: Spring Boot 4.1.1, Java 25, PostgreSQL, STOMP over WebSocket.

## Requirements

- JDK 25
- A running PostgreSQL instance with the `U5W6D5-PROGETTO-SETTIMANALE` database. It already
  exists for this project; otherwise create it (the quotes keep the exact case and the hyphens):

  ```sql
  CREATE DATABASE "U5W6D5-PROGETTO-SETTIMANALE";
  ```

The datasource password has **no default**: set `DB_PASSWORD` in the environment, so that
no credential ends up in the repository. `DB_USERNAME` defaults to `postgres`.

## Run

```bash
# PowerShell
$env:DB_PASSWORD = "<your password>"
./mvnw spring-boot:run
```

```bash
# bash
DB_PASSWORD='<your password>' ./mvnw spring-boot:run
```

Optional variables, each enabling one feature (the application starts without them, and
the matching endpoint answers `503` with a clear message):

| Variable | Used for |
| --- | --- |
| `OPENROUTER_API_KEY` | AI reply suggestion through OpenRouter |
| `OPENROUTER_MODEL` | Model id, default `nvidia/nemotron-3.5-lightning:free` |
| `OPENROUTER_REASONING` | `true` (default) sends `"reasoning": {"enabled": true}`; `false` answers faster |
| `OPENROUTER_REASONING_EFFORT` | Reasoning budget: `low` (default), `medium`, `high` |
| `MAIL_USERNAME` | Gmail address: sender **and** recipient of every stats email |
| `MAIL_PASSWORD` | Google App Password for that address (not the account password) |

Starting without `DB_PASSWORD` fails fast with
`Could not resolve placeholder 'DB_PASSWORD'`.

The application listens on `http://localhost:8080`. Hibernate creates the `app_user` and
`message` tables on the first start.

## REST API

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | no | Creates an account. Body: `{"username","displayName","password"}`. Returns `201` + the user. |
| `POST` | `/api/auth/login` | no | Form login (`application/x-www-form-urlencoded`, fields `username` and `password`). Returns `200` + the user and sets the `JSESSIONID` cookie. |
| `POST` | `/api/auth/logout` | session | Invalidates the session. Returns `204`. |
| `GET` | `/api/users` | session | The people the current user can talk to, themselves excluded. |
| `GET` | `/api/users/me` | session | The current user. |
| `GET` | `/api/conversations` | session | The whole contact list in one request: each person, the latest message of the conversation and the unread count. |
| `GET` | `/api/messages/{username}` | session | The stored conversation with that user, in server order. |
| `POST` | `/api/messages/{username}/read` | session | Marks as read every message received from that user. Returns `204` and pushes a READ receipt to the author. |
| `GET` | `/api/presence` | session | Who has an open channel right now. The channel keeps it up to date afterwards. |
| `POST` | `/api/ai/suggestion/{username}` | session | AI proposal for the next message to that user: `{"suggestion": "..."}`. Nothing is stored. |
| `GET` | `/api/settings/me` | session | The user's AI token limit with the allowed range: `{aiMaxTokens, defaultMaxTokens, minMaxTokens, maxMaxTokens}`. |
| `PUT` | `/api/settings/me` | session | Body `{"aiMaxTokens": 800}` sets the limit (`400` outside the range); `null` resets it to the default. |
| `GET` | `/api/stats/me` | session | Account stats: `{username, displayName, sent, received, conversations, generatedAt}`. |
| `POST` | `/api/stats/me/email` | session | Emails the stats (Thymeleaf template) to `MAIL_USERNAME`. Returns `202` + `{"sentTo": "..."}`. |

Every other request without a valid session returns `401` with a JSON body.

Usernames are stored and matched in lowercase.

### Read state

`message.read_at` is null while the recipient has not opened the conversation.
`POST /api/messages/{username}/read` stamps it, and `GET /api/conversations` counts the
rows that are still null. The unread count is therefore derived from stored data and
survives a reload or a new sign in; it is not kept in the client.

`GET /api/conversations` runs two queries whatever the number of contacts:
`findLatestPerConversation` (the highest message id per partner) and
`countUnreadBySender`. It exists so the client does not have to request one history per
contact just to draw the list.

### AI reply suggestion

`SuggestionService` reads the last 20 messages of the conversation (`openrouter.context-messages`)
through `ChatService.history` and sends them to the OpenRouter chat completions endpoint
(`OpenRouterClient`, a plain `RestClient`, OpenAI format). The current user's messages are
the `assistant` turns and the partner's are the `user` turns, so the model writes as the
current user. The system prompt asks for one short message in the tone of the conversation. The language
follows the current user: the one they write their own messages in, even when the other
person uses another language. If they have not written yet, it follows the other person;
an empty conversation gets an opening line in Italian.

Each user sets the `max_tokens` of their suggestions in the settings panel. The value is
stored in `app_user.ai_max_tokens` (null = default) and must stay within
`openrouter.max-tokens.min`/`max` (1000–4000, default 1500). It counts the reasoning tokens
too: a low limit is faster but the model may be cut off, which answers `502`.

The suggestion is **never stored**: the endpoint only reads, and the text goes back to the
client, which puts it in the composer. The user edits it and sends it like any other
message, through the WebSocket channel. Errors: `503` without `OPENROUTER_API_KEY`, `502` when
OpenRouter fails, times out (90 s) or cuts the answer at `max_tokens`. Reasoning is
sent as `{"effort": "low"}`: without a budget the free model can reason until it runs out
of tokens. A reply cut at `max_tokens` is retried once with reasoning off. With reasoning on,
the free model can take 15–60 s to answer; only `message.content` is used, the thinking is discarded.

### Account stats by email

`StatsService` counts, for the current user, the messages sent, the messages received and
the open chats — the distinct people with at least one message in either direction
(`MessageRepository.countConversations`). `MailService` renders
`templates/mail/account-stats.html` with Thymeleaf (inline styles, table layout, so email
clients keep it) and sends it over Gmail SMTP (`smtp.gmail.com:587`, STARTTLS).
Every stats email goes to `MAIL_USERNAME`, whichever user asks for it. Errors: `503`
without `MAIL_USERNAME`, `502` when the SMTP server refuses the credentials or fails.

## WebSocket channel

- Endpoint: `ws://localhost:8080/ws` — plain WebSocket, no SockJS.
- The handshake is an ordinary HTTP request, so it carries the `JSESSIONID` cookie.
  Spring binds the session `Principal` to the STOMP session; the client sends no
  credentials in the `CONNECT` frame.
- Publish to `/app/chat.send` with `{"recipientUsername": "...", "content": "..."}`.
- Subscribe to `/user/queue/messages` for messages, `/user/queue/receipts` for the state
  of the messages you wrote, `/user/queue/errors` for delivery errors, and
  `/topic/presence` for who is online.

### Message state

Every message carries a `status`:

| Status | Meaning | Set by |
| --- | --- | --- |
| `SENT` | Stored by the server. The recipient had no open channel. | `ChatService.send` |
| `DELIVERED` | It reached a connected session of the recipient. | `ChatSocketController` when the recipient is online, or `PresenceListener` when they connect later |
| `READ` | The recipient opened the conversation. | `POST /api/messages/{username}/read` |

Each change is pushed to the **author** of the messages on `/user/queue/receipts` as a
`ReceiptUpdate {status, withUsername, messageIds, at}`. The receipt carries ids only,
never content, and goes to one user through a user destination. The recipient of a
message never receives receipts for it.

`delivered_at` and `read_at` are columns, so the state is not a live-only fact: it comes
back with the history after a reload. The state only moves forward — reading a message
also stamps it delivered, for one that skipped that step.

### Presence

`/topic/presence` is the only broadcast in the application, and it carries no message
content: just `{username, online}`. `PresenceListener` publishes it on
`SessionConnectedEvent` and on `SessionDisconnectEvent`, and a user goes offline only
when their last session closes, so several open tabs behave correctly.

### How the four requirements are met

1. **Only the recipient receives the message.** `ChatSocketController` uses
   `convertAndSendToUser`, which resolves the `/user/queue/messages` destination on the
   sessions of one user. There is no broadcast destination in the application.
2. **The sender comes from the `Principal`.** `SendMessageRequest` has no sender field
   at all, so a forged one in the payload is simply ignored. `ChatService.send` takes
   the username from `principal.getName()`.
3. **Store first, deliver after.** `ChatService.send` is `@Transactional` and publishes
   nothing. The controller calls `convertAndSendToUser` only after that method returns,
   so a rolled back message is never delivered. The `id` comes from the database and
   `sentAt` is assigned by the server.
4. **History and live channel merge without duplicates.** `GET /api/messages/{username}`
   returns the conversation ordered by `sentAt, id`; every message carries the server
   `id`, which the client uses as the deduplication key. The sender receives an echo of
   its own message so both sides render the same `id` and `sentAt`. If the recipient is
   offline, `convertAndSendToUser` finds no session and does nothing — the row stays in
   the database and shows up on the next history request.

## Logging

The console stays plain text; the file `BE/logs/app.log` gets the same events as JSON,
one per line (Spring Boot structured logging, `logstash` format). Files roll at 10MB and
are kept for 14 days. `logs/` is git-ignored.

- `ApiExceptionHandler` logs 4xx answers at `WARN`. Any unexpected exception is logged at
  `ERROR` with its stack trace, and the client gets a generic 500.
- `ChatSocketController` logs rejected WebSocket messages at `WARN`.
- `AuditLogger` writes chat events on the `AUDIT` logger, with an `event` field and
  filterable key-value fields:

  | event | fields |
  |---|---|
  | `LOGIN_OK`, `LOGOUT` | `username` |
  | `LOGIN_FAIL` (WARN) | `username` as typed |
  | `WS_CONNECT` | `username`, `sessionId` |
  | `WS_DISCONNECT` | `username`, `sessionId`, `wentOffline` |
  | `MSG_SENT` | `messageId`, `sender`, `recipient` |
  | `MSG_DELIVERED`, `MSG_READ` | `messageIds`, `count`, `sender`, `recipient` |
  | `AI_SUGGESTION` | `username`, `partner` |
  | `STATS_EMAILED` | `username` |

Only metadata is logged: never message content, never AI suggestions, never passwords.

## Security notes

- Authentication is session based, with BCrypt password hashing.
- CORS allows `http://localhost:5173` (see `app.cors.allowed-origin`) with credentials,
  because the session travels in a cookie.
- **CSRF protection is currently disabled.** This is acceptable only for a local
  exercise. Before exposing this service, enable it with
  `csrf(c -> c.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))`
  and have the frontend echo the `XSRF-TOKEN` cookie in the `X-XSRF-TOKEN` header.
  Note that `@EnableWebSocketSecurity` also installs a CSRF check on the STOMP `CONNECT`
  frame, so message level authorization rules should be added in the same step.
- The `/ws` handshake itself requires an authenticated session, so an anonymous client
  cannot open the channel.
