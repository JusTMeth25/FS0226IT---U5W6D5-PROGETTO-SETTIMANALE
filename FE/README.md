# Zaffiro — Frontend

React 19 + TypeScript + Vite client for the Mini WhatsApp backend.

## Run

Start the backend first (see `../BE/README.md`), then:

```bash
npm install
npm run dev
```

Open <http://localhost:5173>.

The dev server proxies `/api` and `/ws` to `http://localhost:8080` (`vite.config.ts`), so
the browser sees a single origin: the session cookie is a plain first-party cookie and no
CORS preflight is involved. Change `backend` in `vite.config.ts` if the API moves.

## Structure

```
src/
  api/          client.ts (fetch wrapper), types.ts
  hooks/        useChatSocket.ts — the STOMP connection
  lib/          conversations.ts (merge + order), format.ts (dates, avatar hue)
  components/   AuthScreen, Sidebar, Conversation, MessageBubble, Composer, Avatar, Icons
  App.tsx       state: session, contacts, threads, unread counters
  index.css     design tokens for the two themes
  app.css       component styles
```

## How the client meets the requirements

**One channel, two sources.** `App.tsx` keeps a `Threads` map, one entry per contact.
Two things write into it: `api.history(username)` over REST and the STOMP subscription
to `/user/queue/messages`.

**One request for the list.** At startup `api.conversations()` returns every contact with
the latest message and the unread count, and those previews seed the threads. Opening a
conversation then reads its full history and merges by server id.

**Receipts move the ticks.** `applyReceipt` in `lib/conversations.ts` takes the ids named
by a receipt and raises their status. It never lowers one, so a receipt that arrives out
of order cannot pull a message back from READ to DELIVERED.

**No duplicates, server order.** `mergeMessages` in `lib/conversations.ts` indexes both
sources by the server `id` in a `Map`, then sorts by `sentAt` with `id` as the tie
breaker. The two sources can overlap freely: opening a conversation re-reads the history
even if messages already arrived on the channel, and nothing is shown twice.

**The sender is never sent.** The publish payload is `{recipientUsername, content}`.
There is no field for the sender anywhere in the client, by design.

**Offline recipients.** Nothing special happens client side: the message a contact
missed simply appears in the history the next time they open the conversation.

## Interface

- Sign in and sign up on one screen, with a sliding tab.
- Contact list sorted by the latest message, with a preview, a timestamp and an unread
  counter for conversations that are not open. The counter is stored server side, so it
  survives a reload; opening a conversation marks it read through
  `POST /api/messages/{username}/read`, and a message that arrives while the conversation
  is on screen is marked read as it lands.
- Day separators, and consecutive messages from the same person grouped.
- Three delivery states on every outgoing message, with a tooltip: a clock while it is in
  flight, one tick once the server stored it (*Inviato*), two ticks when it reached the
  recipient (*Consegnato*), two coloured ticks once they opened it (*Letto*). The ticks
  move on their own, pushed over `/user/queue/receipts`.
- A green dot on the avatar of whoever is connected, and *in linea* in the conversation
  header, kept live through `/topic/presence`. Someone signing in for the first time
  appears in the list without a reload.
- Live connection state in the header; the composer is disabled while the channel is down.
- Dark and light themes, remembered in `localStorage`.
- Single column under 760px, with a back button to the contact list.
- `Enter` sends, `Shift+Enter` adds a line, and the composer grows with the text.

## Known limits

- The history of a conversation is read in full, with no pagination. Fine for an
  exercise, not for a long conversation.
- Presence is in-memory on the server: restarting the backend clears it until each
  client reconnects.
- A message is marked read for the whole conversation, not one by one: opening a
  conversation reads everything in it.

## Commands

```bash
npm run dev      # dev server on :5173
npm run build    # type-check and build into dist/
npm run lint     # ESLint
npm run preview  # serve the build
```

`npm run preview` has no proxy: serve `dist/` behind the backend, or add one.
