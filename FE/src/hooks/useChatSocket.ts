import { Client } from '@stomp/stompjs'
import { useCallback, useEffect, useRef, useState } from 'react'
import type { Message, PresenceEvent, ReceiptUpdate } from '../api/types.ts'

export type ConnectionState = 'idle' | 'connecting' | 'connected' | 'error'

type Options = {
  /** False while nobody is signed in: the channel stays closed. */
  enabled: boolean
  onMessage: (message: Message) => void
  onReceipt: (receipt: ReceiptUpdate) => void
  onPresence: (event: PresenceEvent) => void
  onError: (message: string) => void
}

const socketUrl = () => {
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  return `${protocol}://${window.location.host}/ws`
}

/**
 * Opens the STOMP channel and subscribes to everything that arrives on its own:
 * messages and receipts on user destinations, presence on a broadcast topic.
 * No credentials are sent here: the handshake carries the session cookie and the
 * server resolves the Principal from it.
 */
export function useChatSocket({ enabled, onMessage, onReceipt, onPresence, onError }: Options) {
  const [state, setState] = useState<ConnectionState>('idle')
  const clientRef = useRef<Client | null>(null)

  // Kept in refs so that changing the handlers never reopens the connection.
  const handlers = useRef({ onMessage, onReceipt, onPresence, onError })

  useEffect(() => {
    handlers.current = { onMessage, onReceipt, onPresence, onError }
  })

  // Adjust the state while rendering rather than from inside the effect, which
  // would cause a second render pass.
  const [wasEnabled, setWasEnabled] = useState(enabled)
  if (wasEnabled !== enabled) {
    setWasEnabled(enabled)
    setState(enabled ? 'connecting' : 'idle')
  }

  useEffect(() => {
    if (!enabled) return

    const client = new Client({
      brokerURL: socketUrl(),
      reconnectDelay: 3000,
      heartbeatIncoming: 10_000,
      heartbeatOutgoing: 10_000,
    })

    client.onConnect = () => {
      setState('connected')

      client.subscribe('/user/queue/messages', (frame) => {
        handlers.current.onMessage(JSON.parse(frame.body) as Message)
      })
      client.subscribe('/user/queue/receipts', (frame) => {
        handlers.current.onReceipt(JSON.parse(frame.body) as ReceiptUpdate)
      })
      client.subscribe('/topic/presence', (frame) => {
        handlers.current.onPresence(JSON.parse(frame.body) as PresenceEvent)
      })
      client.subscribe('/user/queue/errors', (frame) => {
        const body = JSON.parse(frame.body) as { message?: string }
        handlers.current.onError(body.message ?? 'Errore sul canale')
      })
    }

    client.onWebSocketClose = () => setState((current) => (current === 'connected' ? 'connecting' : current))
    client.onStompError = (frame) => {
      setState('error')
      handlers.current.onError(frame.headers['message'] ?? 'Errore STOMP')
    }
    client.onWebSocketError = () => setState('error')

    client.activate()
    clientRef.current = client

    return () => {
      clientRef.current = null
      void client.deactivate()
    }
  }, [enabled])

  const send = useCallback((recipientUsername: string, content: string) => {
    const client = clientRef.current
    if (!client?.connected) return false

    // The payload carries no sender: the server takes it from the Principal.
    client.publish({
      destination: '/app/chat.send',
      body: JSON.stringify({ recipientUsername, content }),
    })
    return true
  }, [])

  return { state, send }
}
