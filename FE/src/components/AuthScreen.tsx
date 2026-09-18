import { useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError, api } from '../api/client.ts'
import type { User } from '../api/types.ts'

type Mode = 'login' | 'register'

export function AuthScreen({ onAuthenticated }: { onAuthenticated: (user: User) => void }) {
  const [mode, setMode] = useState<Mode>('login')
  const [username, setUsername] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const switchTo = (next: Mode) => {
    setMode(next)
    setError(null)
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)

    try {
      if (mode === 'register') {
        await api.register(username, displayName, password)
      }
      onAuthenticated(await api.login(username, password))
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'Server non raggiungibile')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="auth">
      <div className="auth-aside">
        <div className="auth-brand">
          <span className="auth-logo" aria-hidden="true">
            <i /><i /><i />
          </span>
          <h1>Zaffiro</h1>
        </div>
        <p className="auth-claim">
          Conversazioni uno a uno.<br />Recapitate a chi devono, e solo a lui.
        </p>
        <ul className="auth-points">
          <li>Il mittente lo decide la sessione, non il messaggio.</li>
          <li>Ogni messaggio è salvato prima di partire.</li>
          <li>Se sei offline lo ritrovi al rientro.</li>
        </ul>
      </div>

      <div className="auth-panel">
        <div className="auth-tabs" role="tablist">
          <button
            type="button"
            role="tab"
            aria-selected={mode === 'login'}
            className={mode === 'login' ? 'active' : ''}
            onClick={() => switchTo('login')}
          >
            Accedi
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={mode === 'register'}
            className={mode === 'register' ? 'active' : ''}
            onClick={() => switchTo('register')}
          >
            Registrati
          </button>
          <span className={`auth-tabs-thumb ${mode}`} aria-hidden="true" />
        </div>

        <form className="auth-form" onSubmit={submit}>
          <label>
            <span>Username</span>
            <input
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              autoComplete="username"
              placeholder="anna"
              required
              minLength={3}
            />
          </label>

          {mode === 'register' && (
            <label>
              <span>Nome visualizzato</span>
              <input
                value={displayName}
                onChange={(event) => setDisplayName(event.target.value)}
                placeholder="Anna Rossi"
                required
              />
            </label>
          )}

          <label>
            <span>Password</span>
            <input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
              placeholder="almeno 6 caratteri"
              required
              minLength={6}
            />
          </label>

          {error && <p className="auth-error" role="alert">{error}</p>}

          <button type="submit" className="auth-submit" disabled={busy}>
            {busy ? 'Un attimo…' : mode === 'login' ? 'Entra' : 'Crea account'}
          </button>
        </form>
      </div>
    </div>
  )
}
