import { useCallback, useEffect, useRef, useState } from 'react'
import { ApiError, api } from './api/client.ts'
import type { Message, PendingMessage, PresenceEvent, ReceiptUpdate, User } from './api/types.ts'
import { AuthScreen } from './components/AuthScreen.tsx'
import { Conversation } from './components/Conversation.tsx'
import { Sidebar } from './components/Sidebar.tsx'
import { useChatSocket } from './hooks/useChatSocket.ts'
import { applyReceipt, mergeIntoThreads, mergeMessages, partnerOf } from './lib/conversations.ts'
import type { Threads } from './lib/conversations.ts'

type Theme = 'dark' | 'light'

const storedTheme = (): Theme => (localStorage.getItem('zaffiro-theme') === 'light' ? 'light' : 'dark')

export default function App() {
  const [me, setMe] = useState<User | null>(null)
  const [booting, setBooting] = useState(true)
  const [contacts, setContacts] = useState<User[]>([])
  const [threads, setThreads] = useState<Threads>({})
  const [pending, setPending] = useState<PendingMessage[]>([])
  const [unread, setUnread] = useState<Record<string, number>>({})
  const [online, setOnline] = useState<Set<string>>(new Set())
  const [selected, setSelected] = useState<string | null>(null)
  const [loadingThread, setLoadingThread] = useState(false)
  const [toast, setToast] = useState<string | null>(null)
  const [theme, setTheme] = useState<Theme>(storedTheme)

  // Read inside the socket callback, which must not be recreated on every render.
  const selectedRef = useRef<string | null>(null)
  const meRef = useRef<User | null>(null)
  const knownContacts = useRef<Set<string>>(new Set())

  useEffect(() => {
    selectedRef.current = selected
    meRef.current = me
  })

  useEffect(() => {
    document.documentElement.dataset.theme = theme
    localStorage.setItem('zaffiro-theme', theme)
  }, [theme])

  useEffect(() => {
    if (!toast) return
    const timer = setTimeout(() => setToast(null), 4000)
    return () => clearTimeout(timer)
  }, [toast])

  // An existing session survives a page reload, so ask the server who we are.
  useEffect(() => {
    api
      .me()
      .then(setMe)
      .catch(() => undefined)
      .finally(() => setBooting(false))
  }, [])

  // One request builds the whole contact list: the people, the latest message of each
  // conversation and the unread counts, which are stored and so survive a reload.
  const refreshConversations = useCallback(() => {
    const currentUser = meRef.current
    if (!currentUser) return Promise.resolve()

    return api
      .conversations()
      .then((summaries) => {
        setContacts(summaries.map((summary) => summary.user))
        knownContacts.current = new Set(summaries.map((summary) => summary.user.username))
        setUnread(
          Object.fromEntries(summaries.map((summary) => [summary.user.username, summary.unreadCount])),
        )

        // Seed the threads with the previews; the full history is read when a
        // conversation is opened and merges by server id.
        const latest = summaries
          .map((summary) => summary.lastMessage)
          .filter((message): message is Message => message !== null)
        setThreads((current) => mergeIntoThreads(current, currentUser.username, latest))
      })
      .catch((cause) => setToast(cause instanceof ApiError ? cause.message : 'Contatti non disponibili'))
  }, [])

  useEffect(() => {
    if (!me) return

    void refreshConversations()
    api
      .presence()
      .then((usernames) => setOnline(new Set(usernames)))
      .catch(() => undefined)
  }, [me, refreshConversations])

  const onIncoming = useCallback((message: Message) => {
    const currentUser = meRef.current
    if (!currentUser) return

    const key = partnerOf(message, currentUser.username)
    setThreads((current) => mergeIntoThreads(current, currentUser.username, [message]))

    if (message.senderUsername === currentUser.username) {
      // Echo of one of our own messages: drop the matching optimistic bubble.
      setPending((current) => {
        const index = current.findIndex(
          (item) => item.recipientUsername === message.recipientUsername && item.content === message.content,
        )
        return index === -1 ? current : current.filter((_, position) => position !== index)
      })
    } else if (selectedRef.current === key) {
      // The conversation is on screen, so the message is read as it arrives.
      void api.markRead(key).catch(() => undefined)
    } else {
      setUnread((current) => ({ ...current, [key]: (current[key] ?? 0) + 1 }))
    }
  }, [])

  // A receipt only moves the ticks of messages we wrote; it carries no content.
  const onReceipt = useCallback((receipt: ReceiptUpdate) => {
    setThreads((current) => applyReceipt(current, receipt))
  }, [])

  const onPresence = useCallback((event: PresenceEvent) => {
    const currentUser = meRef.current
    if (!currentUser || event.username === currentUser.username) return

    setOnline((current) => {
      const next = new Set(current)
      if (event.online) next.add(event.username)
      else next.delete(event.username)
      return next
    })

    // Somebody we have never seen just signed in: refresh the contact list.
    if (event.online && !knownContacts.current.has(event.username)) {
      void refreshConversations()
    }
  }, [refreshConversations])

  const onChannelError = useCallback((message: string) => setToast(message), [])

  const socket = useChatSocket({
    enabled: me !== null,
    onMessage: onIncoming,
    onReceipt,
    onPresence,
    onError: onChannelError,
  })

  const openConversation = useCallback(
    async (username: string) => {
      setSelected(username)
      setUnread((current) => ({ ...current, [username]: 0 }))
      setLoadingThread(true)

      try {
        // Re-read the history: it is the source of truth and it merges with whatever
        // already arrived on the channel, the server id keeping duplicates out.
        const history = await api.history(username)
        await api.markRead(username).catch(() => undefined)
        setThreads((current) => ({
          ...current,
          [username]: mergeMessages(current[username] ?? [], history),
        }))
      } catch (cause) {
        setToast(cause instanceof ApiError ? cause.message : 'Cronologia non disponibile')
      } finally {
        setLoadingThread(false)
      }
    },
    [],
  )

  const send = useCallback(
    (content: string) => {
      if (!selected) return
      const accepted = socket.send(selected, content)

      if (!accepted) {
        setToast('Canale non connesso: messaggio non inviato')
        return
      }
      setPending((current) => [
        ...current,
        { tempId: crypto.randomUUID(), recipientUsername: selected, content, sentAt: new Date().toISOString() },
      ])
    },
    [selected, socket],
  )

  const logout = useCallback(async () => {
    await api.logout().catch(() => undefined)
    setMe(null)
    setContacts([])
    setThreads({})
    setPending([])
    setUnread({})
    setOnline(new Set())
    setSelected(null)
    knownContacts.current = new Set()
  }, [])

  if (booting) {
    return (
      <div className="boot">
        <span className="boot-pulse" aria-hidden="true" />
        <p>Apro Zaffiro…</p>
      </div>
    )
  }

  if (!me) return <AuthScreen onAuthenticated={setMe} />

  const partner = contacts.find((contact) => contact.username === selected) ?? null

  return (
    <div className="app">
      <Sidebar
        me={me}
        contacts={contacts}
        threads={threads}
        unread={unread}
        online={online}
        selected={selected}
        connection={socket.state}
        theme={theme}
        onSelect={openConversation}
        onToggleTheme={() => setTheme((current) => (current === 'dark' ? 'light' : 'dark'))}
        onLogout={logout}
      />

      {partner ? (
        <Conversation
          me={me}
          partner={partner}
          messages={threads[partner.username] ?? []}
          pending={pending.filter((item) => item.recipientUsername === partner.username)}
          loading={loadingThread}
          canSend={socket.state === 'connected'}
          partnerOnline={online.has(partner.username)}
          onSend={send}
          onBack={() => setSelected(null)}
        />
      ) : (
        <section className="placeholder">
          <span className="placeholder-glow" aria-hidden="true" />
          <h2>Scegli una conversazione</h2>
          <p>
            {contacts.length === 0
              ? 'Non c’è ancora nessun altro iscritto. Registra un secondo account per provare la chat.'
              : 'Le persone con cui puoi parlare sono nell’elenco a sinistra.'}
          </p>
        </section>
      )}

      {toast && (
        <div className="toast" role="status">
          {toast}
        </div>
      )}
    </div>
  )
}
