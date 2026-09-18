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

export type ApiErrorBody = {
  status: number
  error: string
  message: string
  timestamp: string
}
