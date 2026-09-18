import type { MessageStatus } from '../api/types.ts'
import { formatTime } from '../lib/format.ts'
import { CheckIcon, ClockIcon, DoubleCheckIcon } from './Icons.tsx'

type Props = {
  content: string
  sentAt: string
  mine: boolean
  /** True when the previous bubble is from the same person: the tail is dropped. */
  grouped: boolean
  /** Absent while the message is still on its way to the server. */
  status?: MessageStatus
}

const label: Record<MessageStatus, string> = {
  SENT: 'Inviato',
  DELIVERED: 'Consegnato',
  READ: 'Letto',
}

/** Clock while in flight, one tick sent, two delivered, two coloured read. */
function Ticks({ status }: { status?: MessageStatus }) {
  if (!status) return <ClockIcon className="tick" />
  if (status === 'SENT') return <CheckIcon className="tick" />

  return <DoubleCheckIcon className={`tick tick-double ${status === 'READ' ? 'tick-read' : ''}`} />
}

export function MessageBubble({ content, sentAt, mine, grouped, status }: Props) {
  const side = mine ? 'mine' : 'theirs'

  return (
    <div className={`bubble-row ${side} ${grouped ? 'grouped' : ''}`}>
      <div className={`bubble ${side} ${status ? '' : 'pending'}`}>
        <p>{content}</p>
        <span className="bubble-meta">
          <time dateTime={sentAt}>{formatTime(sentAt)}</time>
          {mine && (
            <span className="bubble-ticks" title={status ? label[status] : 'In invio'}>
              <Ticks status={status} />
            </span>
          )}
        </span>
      </div>
    </div>
  )
}
