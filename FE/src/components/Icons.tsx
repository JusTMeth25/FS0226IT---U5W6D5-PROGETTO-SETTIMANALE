type Props = { className?: string }

export const SendIcon = ({ className }: Props) => (
  <svg className={className} viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <path d="M4.5 12 3 4.8a.8.8 0 0 1 1.13-.87l16.2 7.34a.8.8 0 0 1 0 1.46L4.13 20.07A.8.8 0 0 1 3 19.2L4.5 12Zm0 0h7"
      stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
)

export const SearchIcon = ({ className }: Props) => (
  <svg className={className} viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <circle cx="11" cy="11" r="6.5" stroke="currentColor" strokeWidth="1.7" />
    <path d="m16 16 4 4" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
  </svg>
)

export const LogoutIcon = ({ className }: Props) => (
  <svg className={className} viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <path d="M14 8V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2v-2"
      stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
    <path d="M10 12h10m0 0-3-3m3 3-3 3" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
)

export const SunIcon = ({ className }: Props) => (
  <svg className={className} viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <circle cx="12" cy="12" r="4" stroke="currentColor" strokeWidth="1.7" />
    <path d="M12 3v2m0 14v2M3 12h2m14 0h2M5.6 5.6 7 7m10 10 1.4 1.4M18.4 5.6 17 7M7 17l-1.4 1.4"
      stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
  </svg>
)

export const MoonIcon = ({ className }: Props) => (
  <svg className={className} viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <path d="M20 14.2A8.2 8.2 0 0 1 9.8 4a8.2 8.2 0 1 0 10.2 10.2Z"
      stroke="currentColor" strokeWidth="1.7" strokeLinejoin="round" />
  </svg>
)

export const ArrowDownIcon = ({ className }: Props) => (
  <svg className={className} viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <path d="M12 5v14m0 0-5-5m5 5 5-5" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
)

export const ClockIcon = ({ className }: Props) => (
  <svg className={className} viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <circle cx="12" cy="12" r="8" stroke="currentColor" strokeWidth="1.8" />
    <path d="M12 8v4.3l2.6 1.6" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
  </svg>
)

export const CheckIcon = ({ className }: Props) => (
  <svg className={className} viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <path d="m4 13 4.5 4.5L20 6" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
)

export const DoubleCheckIcon = ({ className }: Props) => (
  <svg className={className} viewBox="0 0 30 24" fill="none" aria-hidden="true">
    <path d="m2 13 4.5 4.5L18 6" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
    <path d="m11 17.5 1 1L23.5 7" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
)

export const BackIcon = ({ className }: Props) => (
  <svg className={className} viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <path d="M19 12H5m0 0 6-6m-6 6 6 6" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
)
