/** Deterministic hue from a username, so an avatar keeps the same colours everywhere. */
export function hueOf(value: string): number {
  let hash = 0
  for (let i = 0; i < value.length; i++) {
    hash = (hash << 5) - hash + value.charCodeAt(i)
    hash |= 0
  }
  return Math.abs(hash) % 360
}

export function initialsOf(displayName: string): string {
  const parts = displayName.trim().split(/\s+/).slice(0, 2)
  return parts.map((part) => part[0] ?? '').join('').toUpperCase() || '?'
}

const time = new Intl.DateTimeFormat('it-IT', { hour: '2-digit', minute: '2-digit' })
const dayLong = new Intl.DateTimeFormat('it-IT', { weekday: 'long', day: 'numeric', month: 'long' })
const dayShort = new Intl.DateTimeFormat('it-IT', { day: '2-digit', month: '2-digit', year: 'numeric' })

export const formatTime = (iso: string) => time.format(new Date(iso))

function startOfDay(date: Date): number {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime()
}

/** "Oggi" / "Ieri" / the weekday within the last week / the full date. */
export function formatDayLabel(iso: string): string {
  const date = new Date(iso)
  const days = Math.round((startOfDay(new Date()) - startOfDay(date)) / 86_400_000)

  if (days === 0) return 'Oggi'
  if (days === 1) return 'Ieri'
  if (days < 7) return dayLong.format(date)
  return dayShort.format(date)
}

/** Short label used in the contact list: the time today, otherwise the date. */
export function formatStamp(iso: string): string {
  const date = new Date(iso)
  return startOfDay(date) === startOfDay(new Date()) ? time.format(date) : dayShort.format(date)
}

export const sameDay = (a: string, b: string) =>
  startOfDay(new Date(a)) === startOfDay(new Date(b))
