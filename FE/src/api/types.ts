export type User = {
  id: number
  username: string
  displayName: string
}

/** SENT: stored. DELIVERED: it reached a session of the recipient. READ: they opened it. */
export type MessageStatus = 'SENT' | 'DELIVERED' | 'READ'

export type Message = {
  id: number
  senderUsername: string
  recipientUsername: string
  content: string
  sentAt: string
  status: MessageStatus
}

/** State change of messages we wrote, pushed to us over the channel. */
export type ReceiptUpdate = {
  status: MessageStatus
  /** The other side of the conversation those messages belong to. */
  withUsername: string
  messageIds: number[]
  at: string
}

export type PresenceEvent = {
  username: string
  online: boolean
}

/** A message shown before the server has confirmed it. It has no server id yet. */
export type PendingMessage = {
  tempId: string
  recipientUsername: string
  content: string
  sentAt: string
}

/** One contact-list row, built by the server in a single request. */
export type ConversationSummary = {
  user: User
  /** Null when the two have never written to each other. */
  lastMessage: Message | null
  unreadCount: number
}

/** Figures of the current account; conversations = people with at least one message. */
export type AccountStats = {
  username: string
  displayName: string
  sent: number
  received: number
  conversations: number
  generatedAt: string
}

/** Proposed next message. Never stored by the server. */
export type SuggestionResponse = {
  suggestion: string
}

/** Per-user AI token limit, with the range allowed by the server. */
export type UserSettings = {
  aiMaxTokens: number
  defaultMaxTokens: number
  minMaxTokens: number
  maxMaxTokens: number
}

export type StatsEmailResponse = {
  sentTo: string
}

export type ApiErrorBody = {
  status: number
  error: string
  message: string
  timestamp: string
}
