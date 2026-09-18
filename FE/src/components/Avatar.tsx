import { hueOf, initialsOf } from '../lib/format.ts'

type Props = {
  username: string
  displayName: string
  size?: number
  online?: boolean
}

export function Avatar({ username, displayName, size = 44, online }: Props) {
  const hue = hueOf(username)

  return (
    <span
      className="avatar"
      style={{
        width: size,
        height: size,
        fontSize: size * 0.38,
        background: `linear-gradient(140deg, hsl(${hue} 72% 58%), hsl(${(hue + 48) % 360} 74% 46%))`,
      }}
      aria-hidden="true"
    >
      {initialsOf(displayName)}
      {online === true && <i className="avatar-dot" />}
    </span>
  )
}
