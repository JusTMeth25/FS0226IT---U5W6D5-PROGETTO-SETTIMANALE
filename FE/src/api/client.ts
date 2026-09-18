import type {
  AccountStats,
  ApiErrorBody,
  ConversationSummary,
  Message,
  StatsEmailResponse,
  SuggestionResponse,
  User,
  UserSettings,
} from './types.ts'

export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, { credentials: 'include', ...init })

  if (!response.ok) {
    let message = `Errore ${response.status}`
    try {
      const body = (await response.json()) as ApiErrorBody
      if (body.message) message = body.message
    } catch {
      // The body is not JSON: keep the generic message.
    }
    throw new ApiError(response.status, message)
  }

  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}

export const api = {
  register: (username: string, displayName: string, password: string) =>
    request<User>('/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, displayName, password }),
    }),

  login: (username: string, password: string) =>
    request<User>('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({ username, password }),
    }),

  logout: () => request<void>('/api/auth/logout', { method: 'POST' }),

  me: () => request<User>('/api/users/me'),

  /** Contacts, last message and unread counts in one round trip. */
  conversations: () => request<ConversationSummary[]>('/api/conversations'),

  history: (username: string) =>
    request<Message[]>(`/api/messages/${encodeURIComponent(username)}`),

  /** Who is connected right now; the channel keeps it up to date afterwards. */
  presence: () => request<string[]>('/api/presence'),

  markRead: (username: string) =>
    request<void>(`/api/messages/${encodeURIComponent(username)}/read`, { method: 'POST' }),

  /** AI proposal for the next message to that user. It fills the composer only. */
  suggestion: (username: string) =>
    request<SuggestionResponse>(`/api/ai/suggestion/${encodeURIComponent(username)}`, { method: 'POST' }),

  stats: () => request<AccountStats>('/api/stats/me'),

  settings: () => request<UserSettings>('/api/settings/me'),

  /** null resets the limit to the server default. */
  updateSettings: (aiMaxTokens: number | null) =>
    request<UserSettings>('/api/settings/me', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ aiMaxTokens }),
    }),

  /** Sends the stats by email; the server always uses its configured mailbox. */
  emailStats: () => request<StatsEmailResponse>('/api/stats/me/email', { method: 'POST' }),
}
