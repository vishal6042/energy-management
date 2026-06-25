import {
  createContext,
  useContext,
  useEffect,
  useState,
  type Dispatch,
  type ReactNode,
  type SetStateAction,
} from 'react'
import type { DataCard, PendingAction } from '../types'

export interface ChatItem {
  id: string
  role: 'user' | 'assistant'
  content: string
  citations?: string[]
  cards?: DataCard[]
  pendingActions?: PendingAction[]
  agent?: string
  resolved?: 'confirmed' | 'cancelled'
  /** The initial greeting; excluded from the history sent to the LLM. */
  seed?: boolean
}

export const makeId = () => crypto.randomUUID()

const STORAGE_KEY = 'dems.assistant.chat'

function greeting(): ChatItem {
  return {
    id: makeId(),
    role: 'assistant',
    seed: true,
    content:
      "Hi! I'm the on-device BEMS copilot. Ask about your devices and savings, " +
      "how the algorithms work, or tell me to turn a device on/off (I'll ask you to confirm first).",
  }
}

function loadItems(): ChatItem[] {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    if (raw) {
      const parsed = JSON.parse(raw) as ChatItem[]
      if (Array.isArray(parsed) && parsed.length > 0) {
        return parsed
      }
    }
  } catch {
    // ignore malformed storage
  }
  return [greeting()]
}

interface ChatContextValue {
  items: ChatItem[]
  setItems: Dispatch<SetStateAction<ChatItem[]>>
  input: string
  setInput: Dispatch<SetStateAction<string>>
  busy: boolean
  setBusy: Dispatch<SetStateAction<boolean>>
  reset: () => void
}

const ChatContext = createContext<ChatContextValue | null>(null)

/**
 * Holds the assistant conversation above the router so it survives navigation
 * between screens, and mirrors it to sessionStorage so it survives a reload for
 * the lifetime of the browser session.
 */
export function ChatProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ChatItem[]>(loadItems)
  const [input, setInput] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    try {
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(items))
    } catch {
      // storage full / unavailable — keep in memory only
    }
  }, [items])

  function reset() {
    setItems([greeting()])
  }

  return (
    <ChatContext.Provider
      value={{ items, setItems, input, setInput, busy, setBusy, reset }}
    >
      {children}
    </ChatContext.Provider>
  )
}

export function useChat(): ChatContextValue {
  const ctx = useContext(ChatContext)
  if (!ctx) {
    throw new Error('useChat must be used within a ChatProvider')
  }
  return ctx
}
