# Zaffiro — Mini WhatsApp

A minimal one-to-one chat: every registered user sees who they can talk to and opens a
private conversation. A message reaches **only** its recipient, through the broker user
destinations, and never the other subscribers.

- `BE/` — Spring Boot 4.1.1, Java 25, PostgreSQL, STOMP over WebSocket. See [BE/README.md](BE/README.md).
- `FE/` — React 19, TypeScript, Vite. See [FE/README.md](FE/README.md).

## The four rules of the assignment

| Rule | Where it lives |
| --- | --- |
| The message reaches only the recipient, through the broker user destinations | `ChatSocketController` publishes with `convertAndSendToUser` on `/user/queue/messages`. No broadcast destination exists. |
| The sender comes from the session `Principal`, not from the client payload | `SendMessageRequest` has no sender field; `ChatService.send` takes `principal.getName()`. |
| A message is stored first, with the id and the instant assigned by the server, and delivered only after | `ChatService.send` is `@Transactional` and publishes nothing; the controller delivers after it returns. |
| The client merges the REST history with the open channel, without duplicates and in server order; an offline recipient finds the message later | `mergeMessages` in `FE/src/lib/conversations.ts` keys by server id and sorts by `sentAt, id`. Messages for an offline user stay in the database. |

## Beyond the assignment

- `GET /api/conversations` builds the contact list in one request — people, latest
  message and unread counts — with two queries regardless of how many contacts there are.
- Unread counts are derived from `message.read_at`, so they survive a reload instead of
  living in the browser.
- Three delivery states per message — `SENT`, `DELIVERED`, `READ` — stored in columns and
  pushed to the author on `/user/queue/receipts` as they change, so the ticks move on
  their own.
- Presence on `/topic/presence`, the one broadcast in the application, carrying no
  message content.

## Quick start on Windows

1. PostgreSQL running, with the `U5W6D5-PROGETTO-SETTIMANALE` database. It already exists
   for this project; otherwise create it (the quotes keep the exact case and the hyphens):

   ```sql
   CREATE DATABASE "U5W6D5-PROGETTO-SETTIMANALE";
   ```

2. Copy `.env.example` to `.env` and put your PostgreSQL password in it. That file is
   git-ignored, so the password never reaches the repository. Without it the script
   simply asks for the password each time.

3. Double click **`start.cmd`**, or run it from a terminal.

   It checks that Java and npm are there, installs the frontend dependencies the first
   time, opens the backend and the frontend in one window each, waits for both to answer
   and then opens the browser.

4. Create two accounts in two different browser profiles (or one normal window and one
   private window) and write to each other.

Close the two windows to stop the servers.

## Manual start

1. Backend — the password comes from the environment, never from the repository:

   ```bash
   cd BE
   DB_PASSWORD='<your password>' ./mvnw spring-boot:run
   ```

   ```powershell
   # PowerShell
   cd BE
   $env:DB_PASSWORD = "<your password>"
   ./mvnw spring-boot:run
   ```

2. Frontend:

   ```bash
   cd FE
   npm install
   npm run dev
   ```

3. Open <http://localhost:5173>.

## Log agent

`.claude/agents/log-agent.md` is a Claude Code subagent that works on `BE/logs/`
(see [Logging](BE/README.md#logging)). Ask Claude Code, for example:

- `use log-agent: error report for today`
- `use log-agent: chat usage statistics`
- `use log-agent: what happened to user mario in the last hour`
- `use log-agent: log maintenance`

It writes reports to `BE/logs/reports/` and one line per run to `BE/logs/agent.log`.
It never edits source code and asks before deleting any file.

## Note

CSRF protection is disabled in this build, which is acceptable for a local exercise only.
The reason and the fix are documented in [BE/README.md](BE/README.md#security-notes).
