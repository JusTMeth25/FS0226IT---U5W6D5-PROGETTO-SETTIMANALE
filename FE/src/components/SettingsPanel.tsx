import { useEffect, useState } from 'react'
import { ApiError, api } from '../api/client.ts'
import type { UserSettings } from '../api/types.ts'
import { CloseIcon } from './Icons.tsx'

type Props = {
  onClose: () => void
  onNotice: (message: string) => void
}

const STEP = 100

const PRESETS = [
  { label: 'Breve', value: 1000 },
  { label: 'Normale', value: 1500 },
  { label: 'Ampio', value: 3000 },
]

const errorText = (cause: unknown, fallback: string) => (cause instanceof ApiError ? cause.message : fallback)

/** Per-user preferences, stored on the server. For now: the token limit of the AI suggestion. */
export function SettingsPanel({ onClose, onNotice }: Props) {
  const [settings, setSettings] = useState<UserSettings | null>(null)
  const [value, setValue] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    api
      .settings()
      .then((loaded) => {
        setSettings(loaded)
        setValue(loaded.aiMaxTokens)
      })
      .catch((cause) => setError(errorText(cause, 'Impostazioni non disponibili')))
  }, [])

  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  const save = async (next: number | null) => {
    setSaving(true)
    try {
      const saved = await api.updateSettings(next)
      setSettings(saved)
      setValue(saved.aiMaxTokens)
      onNotice(`Limite AI impostato a ${saved.aiMaxTokens} token`)
    } catch (cause) {
      onNotice(errorText(cause, 'Salvataggio non riuscito'))
    } finally {
      setSaving(false)
    }
  }

  const clamp = (n: number) => (settings ? Math.min(settings.maxMaxTokens, Math.max(settings.minMaxTokens, n)) : n)

  return (
    <div className="stats-backdrop" onClick={onClose}>
      <section
        className="stats-panel"
        role="dialog"
        aria-modal="true"
        aria-labelledby="settings-title"
        onClick={(event) => event.stopPropagation()}
      >
        <header className="stats-head">
          <h2 id="settings-title">Impostazioni</h2>
          <button type="button" className="stats-close" onClick={onClose} aria-label="Chiudi">
            <CloseIcon />
          </button>
        </header>

        {error && <p className="stats-note error">{error}</p>}
        {!error && !settings && <p className="stats-note">Carico le impostazioni…</p>}

        {settings && (
          <div className="settings-group">
            <div className="settings-row">
              <label htmlFor="ai-tokens">Limite token del suggerimento AI</label>
              <strong className="settings-value">{value}</strong>
            </div>

            <input
              id="ai-tokens"
              type="range"
              min={settings.minMaxTokens}
              max={settings.maxMaxTokens}
              step={STEP}
              value={value}
              onChange={(event) => setValue(Number(event.target.value))}
            />
            <div className="settings-scale">
              <span>{settings.minMaxTokens}</span>
              <span>{settings.maxMaxTokens}</span>
            </div>

            <div className="settings-presets">
              {PRESETS.map((preset) => (
                <button
                  key={preset.label}
                  type="button"
                  className={value === clamp(preset.value) ? 'active' : ''}
                  onClick={() => setValue(clamp(preset.value))}
                >
                  {preset.label}
                </button>
              ))}
            </div>

            <p className="stats-note">
              I token comprendono anche il ragionamento del modello. Un limite più alto dà risposte più curate ma
              più lente; se è troppo basso la risposta può interrompersi. Predefinito: {settings.defaultMaxTokens}.
            </p>

            <div className="settings-actions">
              <button
                type="button"
                className="settings-reset"
                onClick={() => save(null)}
                disabled={saving}
              >
                Ripristina
              </button>
              <button
                type="button"
                className="stats-email"
                onClick={() => save(value)}
                disabled={saving || value === settings.aiMaxTokens}
              >
                {saving ? 'Salvo…' : 'Salva'}
              </button>
            </div>
          </div>
        )}
      </section>
    </div>
  )
}
