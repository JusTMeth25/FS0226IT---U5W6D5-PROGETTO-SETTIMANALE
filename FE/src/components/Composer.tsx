import { useEffect, useRef, useState } from 'react'
import type { KeyboardEvent } from 'react'
import { SendIcon } from './Icons.tsx'

type Props = {
  recipientName: string
  disabled: boolean
  onSend: (content: string) => void
}

const MAX_LENGTH = 2000

export function Composer({ recipientName, disabled, onSend }: Props) {
  const [value, setValue] = useState('')
  const textarea = useRef<HTMLTextAreaElement>(null)

  // Grow with the content, up to the max height set in CSS.
  useEffect(() => {
    const element = textarea.current
    if (!element) return
    element.style.height = 'auto'
    element.style.height = `${Math.min(element.scrollHeight, 160)}px`
  }, [value])

  const submit = () => {
    const content = value.trim()
    if (!content || disabled) return
    onSend(content)
    setValue('')
  }

  const onKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault()
      submit()
    }
  }

  const remaining = MAX_LENGTH - value.length

  return (
    <div className="composer">
      <textarea
        ref={textarea}
        value={value}
        onChange={(event) => setValue(event.target.value.slice(0, MAX_LENGTH))}
        onKeyDown={onKeyDown}
        placeholder={disabled ? 'Canale non disponibile' : `Scrivi a ${recipientName}`}
        rows={1}
        disabled={disabled}
        aria-label="Testo del messaggio"
      />
      {remaining < 200 && <span className="composer-count">{remaining}</span>}
      <button
        type="button"
        className="composer-send"
        onClick={submit}
        disabled={disabled || value.trim().length === 0}
        aria-label="Invia"
      >
        <SendIcon />
      </button>
    </div>
  )
}
