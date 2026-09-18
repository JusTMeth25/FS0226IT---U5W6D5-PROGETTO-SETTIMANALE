import type { Message, MessageStatus, ReceiptUpdate } from '../api/types.ts'

/** Messages of one conversation, keyed by the other person's username. */
export type Threads = Record<string, Message[]>

const byServerOrder = (a: Message, b: Message) => {
  const delta = Date.parse(a.sentAt) - Date.parse(b.sentAt)
  return delta !== 0 ? delta : a.id - b.id
}

export const partnerOf = (message: Message, me: string) =>
  message.senderUsername === me ? message.recipientUsername : message.senderUsername

/**
 * Merges messages into a thread. The server id is the deduplication key, so the
 * REST history and the live channel can overlap freely; the order is always the
 * one decided by the server.
 */
export function mergeMessages(existing: Message[], incoming: Message[]): Message[] {
  const byId = new Map<number, Message>()
  for (const message of existing) byId.set(message.id, message)
  for (const message of incoming) byId.set(message.id, message)
  return [...byId.values()].sort(byServerOrder)
}

export function mergeIntoThreads(threads: Threads, me: string, incoming: Message[]): Threads {
  const next: Threads = { ...threads }
  for (const message of incoming) {
    const key = partnerOf(message, me)
    next[key] = mergeMessages(next[key] ?? [], [message])
  }
  return next
}

const rank: Record<MessageStatus, number> = { SENT: 0, DELIVERED: 1, READ: 2 }

/**
 * Moves the ticks of the messages named by a receipt. The state only ever moves
 * forward, so an out-of-order receipt cannot pull a message back to SENT.
 */
export function applyReceipt(threads: Threads, receipt: ReceiptUpdate): Threads {
  const thread = threads[receipt.withUsername]
  if (!thread) return threads

  const touched = new Set(receipt.messageIds)
  let changed = false

  const next = thread.map((message) => {
    if (!touched.has(message.id) || rank[message.status] >= rank[receipt.status]) return message
    changed = true
    return { ...message, status: receipt.status }
  })

  return changed ? { ...threads, [receipt.withUsername]: next } : threads
}

export const lastOf = (messages: Message[] | undefined) =>
  messages && messages.length > 0 ? messages[messages.length - 1] : undefined
