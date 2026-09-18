import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import type { Message, PendingMessage, User } from '../api/types.ts'
import { formatDayLabel, sameDay } from '../lib/format.ts'
import { Avatar } from './Avatar.tsx'
import { ArrowDownIcon, BackIcon } from './Icons.tsx'
import { Composer } from './Composer.tsx'
import { MessageBubble } from './MessageBubble.tsx'

type Props = {
  me: User
  partner: User
  messages: Message[]
  pending: PendingMessage[]
  loading: boolean
  canSend: boolean
  partnerOnline: boolean
  onSend: (content: string) => void
  onSuggest: () => Promise<string | null>
  onBack: () => void
}

/** Distance from the bottom under which the view follows new messages. */
const STICKY_PX = 120

export function Conversation({ me, partner, messages, pending, loading, canSend, partnerOnline, onSend, onSuggest, onBack }: Props) {
  const scroller = useRef<HTMLDivElement>(null)
  const [atBottom, setAtBottom] = useState(true)

  const onScroll = () => {
    const element = scroller.current
    if (!element) return
    const distance = element.scrollHeight - element.scrollTop - element.clientHeight
    setAtBottom(distance < STICKY_PX)
  }

  const scrollToBottom = (behavior: ScrollBehavior = 'smooth') => {
    const element = scroller.current
    if (element) element.scrollTo({ top: element.scrollHeight, behavior })
  }

  // Reset while rendering when the conversation changes, then jump to the end.
  const [shownPartner, setShownPartner] = useState(partner.username)
  if (shownPartner !== partner.username) {
    setShownPartner(partner.username)
    setAtBottom(true)
  }

  useLayoutEffect(() => {
    scrollToBottom('auto')
  }, [partner.username])

  // Follow new messages only when the reader is already at the bottom.
  useEffect(() => {
    if (atBottom) scrollToBottom()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [messages.length, pending.length])

  const items = messages.map((message, index) => {
    const previous = messages[index - 1]
    const newDay = !previous || !sameDay(previous.sentAt, message.sentAt)
    const grouped = !newDay && previous?.senderUsername === message.senderUsername

    return (
      <div key={message.id}>
        {newDay && (
          <div className="day-divider">
            <span>{formatDayLabel(message.sentAt)}</span>
          </div>
        )}
        <MessageBubble
          content={message.content}
          sentAt={message.sentAt}
          mine={message.senderUsername === me.username}
          grouped={grouped}
          status={message.status}
        />
      </div>
    )
  })

  return (
    <section className="conversation">
      <header className="conversation-head">
        <button type="button" className="back-button" onClick={onBack} aria-label="Torna all’elenco">
          <BackIcon />
        </button>
        <Avatar username={partner.username} displayName={partner.displayName} size={44} online={partnerOnline} />
        <div className="conversation-head-text">
          <strong>{partner.displayName}</strong>
          <span className={partnerOnline ? 'is-online' : ''}>
            {partnerOnline ? 'in linea' : `@${partner.username}`}
          </span>
        </div>
      </header>

      <div className="thread" ref={scroller} onScroll={onScroll}>
        {loading && <p className="thread-note">Carico la cronologia…</p>}

        {!loading && messages.length === 0 && pending.length === 0 && (
          <div className="thread-empty">
            <Avatar username={partner.username} displayName={partner.displayName} size={78} />
            <h3>Ancora nessun messaggio</h3>
            <p>Scrivi tu la prima riga a {partner.displayName}.</p>
          </div>
        )}

        {items}

        {pending.map((message) => (
          <MessageBubble
            key={message.tempId}
            content={message.content}
            sentAt={message.sentAt}
            mine
            grouped={false}
          />
        ))}
      </div>

      {!atBottom && (
        <button type="button" className="jump-down" onClick={() => scrollToBottom()} aria-label="Vai in fondo">
          <ArrowDownIcon />
        </button>
      )}

      {/* Keyed by partner: a draft or a pending suggestion never moves to another chat. */}
      <Composer
        key={partner.username}
        recipientName={partner.displayName}
        disabled={!canSend}
        onSend={onSend}
        onSuggest={onSuggest}
      />
    </section>
  )
}
