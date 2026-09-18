import { useEffect, useState } from 'react'
import { ApiError, api } from '../api/client.ts'
import type { AccountStats } from '../api/types.ts'
import { CloseIcon } from './Icons.tsx'

type Props = {
  onClose: () => void
  onNotice: (message: string) => void
}

const errorText = (cause: unknown, fallback: string) => (cause instanceof ApiError ? cause.message : fallback)

/** The account figures, read fresh on every opening, and the button that emails them. */
export function StatsPanel({ onClose, onNotice }: Props) {
  const [stats, setStats] = useState<AccountStats | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [sending, setSending] = useState(false)

  useEffect(() => {
    api
      .stats()
      .then(setStats)
      .catch((cause) => setError(errorText(cause, 'Statistiche non disponibili')))
  }, [])

  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  const email = async () => {
    setSending(true)
    try {
      const { sentTo } = await api.emailStats()
      onNotice(`Statistiche inviate a ${sentTo}`)
    } catch (cause) {
      onNotice(errorText(cause, 'Invio email non riuscito'))
    } finally {
      setSending(false)
    }
  }

  const figures = stats
    ? [
        { label: 'Messaggi inviati', value: stats.sent, tone: 'sent' },
        { label: 'Messaggi ricevuti', value: stats.received, tone: 'received' },
        { label: 'Chat aperte', value: stats.conversations, tone: 'chats' },
      ]
    : []

  return (
    <div className="stats-backdrop" onClick={onClose}>
      <section
        className="stats-panel"
        role="dialog"
        aria-modal="true"
        aria-labelledby="stats-title"
        onClick={(event) => event.stopPropagation()}
      >
        <header className="stats-head">
          <h2 id="stats-title">Le tue statistiche</h2>
          <button type="button" className="stats-close" onClick={onClose} aria-label="Chiudi">
            <CloseIcon />
          </button>
        </header>

        {error && <p className="stats-note error">{error}</p>}
        {!error && !stats && <p className="stats-note">Calcolo le statistiche…</p>}

        {stats && (
          <>
            <div className="stats-grid">
              {figures.map((figure) => (
                <div key={figure.tone} className={`stats-card ${figure.tone}`}>
                  <strong>{figure.value}</strong>
                  <span>{figure.label}</span>
                </div>
              ))}
            </div>
            <p className="stats-note">Le chat aperte sono le persone con cui hai scambiato almeno un messaggio.</p>
          </>
        )}

        <button type="button" className="stats-email" onClick={email} disabled={sending || !stats}>
          {sending ? 'Invio in corso…' : 'Invia per email'}
        </button>
      </section>
    </div>
  )
}
