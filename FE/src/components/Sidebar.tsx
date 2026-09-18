import { useMemo, useState } from 'react'
import type { ConnectionState } from '../hooks/useChatSocket.ts'
import type { Message, User } from '../api/types.ts'
import { formatStamp } from '../lib/format.ts'
import { lastOf } from '../lib/conversations.ts'
import type { Threads } from '../lib/conversations.ts'
import { Avatar } from './Avatar.tsx'
import { LogoutIcon, MoonIcon, SearchIcon, SunIcon } from './Icons.tsx'

type Props = {
  me: User
  contacts: User[]
  threads: Threads
  unread: Record<string, number>
  online: Set<string>
  selected: string | null
  connection: ConnectionState
  theme: 'dark' | 'light'
  onSelect: (username: string) => void
  onToggleTheme: () => void
  onLogout: () => void
}

const connectionLabel: Record<ConnectionState, string> = {
  idle: 'non connesso',
  connecting: 'connessione…',
  connected: 'in linea',
  error: 'canale interrotto',
}

function preview(me: string, message: Message | undefined) {
  if (!message) return 'Nessun messaggio'
  const prefix = message.senderUsername === me ? 'Tu: ' : ''
  return prefix + message.content
}

export function Sidebar(props: Props) {
  const { me, contacts, threads, unread, online, selected, connection, theme } = props
  const [query, setQuery] = useState('')

  const ordered = useMemo(() => {
    const term = query.trim().toLowerCase()
    return contacts
      .filter(
        (contact) =>
          contact.displayName.toLowerCase().includes(term) ||
          contact.username.toLowerCase().includes(term),
      )
      .map((contact) => ({ contact, last: lastOf(threads[contact.username]) }))
      .sort((a, b) => {
        if (!a.last && !b.last) return a.contact.displayName.localeCompare(b.contact.displayName)
        if (!a.last) return 1
        if (!b.last) return -1
        return Date.parse(b.last.sentAt) - Date.parse(a.last.sentAt)
      })
  }, [contacts, threads, query])

  return (
    <aside className="sidebar">
      <header className="sidebar-head">
        <div className="sidebar-me">
          <Avatar username={me.username} displayName={me.displayName} size={42} />
          <div className="sidebar-me-text">
            <strong>{me.displayName}</strong>
            <span className={`status status-${connection}`}>
              <i aria-hidden="true" />
              {connectionLabel[connection]}
            </span>
          </div>
        </div>
        <div className="sidebar-actions">
          <button
            type="button"
            onClick={props.onToggleTheme}
            title={theme === 'dark' ? 'Tema chiaro' : 'Tema scuro'}
            aria-label={theme === 'dark' ? 'Passa al tema chiaro' : 'Passa al tema scuro'}
          >
            {theme === 'dark' ? <SunIcon /> : <MoonIcon />}
          </button>
          <button type="button" onClick={props.onLogout} title="Esci" aria-label="Esci">
            <LogoutIcon />
          </button>
        </div>
      </header>

      <div className="sidebar-search">
        <SearchIcon />
        <input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Cerca una persona"
          aria-label="Cerca una persona"
        />
      </div>

      <nav className="contact-list">
        {ordered.length === 0 && <p className="contact-empty">Nessun risultato.</p>}

        {ordered.map(({ contact, last }) => {
          const count = unread[contact.username] ?? 0
          return (
            <button
              key={contact.username}
              type="button"
              className={`contact ${selected === contact.username ? 'selected' : ''}`}
              onClick={() => props.onSelect(contact.username)}
            >
              <Avatar
                username={contact.username}
                displayName={contact.displayName}
                online={online.has(contact.username)}
              />
              <span className="contact-text">
                <span className="contact-top">
                  <strong>{contact.displayName}</strong>
                  {last && <time dateTime={last.sentAt}>{formatStamp(last.sentAt)}</time>}
                </span>
                <span className="contact-bottom">
                  <span className={`contact-preview ${last ? '' : 'muted'}`}>
                    {preview(me.username, last)}
                  </span>
                  {count > 0 && <span className="badge">{count > 99 ? '99+' : count}</span>}
                </span>
              </span>
            </button>
          )
        })}
      </nav>
    </aside>
  )
}
