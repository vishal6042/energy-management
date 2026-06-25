import { useEffect, useRef } from 'react'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { assistantChat, assistantConfirm } from '../services/api'
import DataCards from '../components/DataCards'
import { makeId, useChat, type ChatItem } from '../context/ChatContext'

export default function Assistant() {
  const { items, setItems, input, setInput, busy, setBusy, reset } = useChat()
  const listRef = useRef<HTMLDivElement>(null)

  function scrollToEnd() {
    requestAnimationFrame(() =>
      listRef.current?.scrollTo({ top: listRef.current.scrollHeight }),
    )
  }

  // Jump to the latest message when returning to this screen.
  useEffect(() => {
    listRef.current?.scrollTo({ top: listRef.current.scrollHeight })
  }, [])

  function historyFor(extra: ChatItem[]): { role: 'user' | 'assistant'; content: string }[] {
    return [...items, ...extra]
      .filter((m) => !m.seed)
      .map((m) => ({ role: m.role, content: m.content }))
  }

  async function send() {
    const text = input.trim()
    if (!text || busy) return
    const userMsg: ChatItem = { id: makeId(), role: 'user', content: text }
    setItems((prev) => [...prev, userMsg])
    setInput('')
    setBusy(true)
    scrollToEnd()
    try {
      const resp = await assistantChat(historyFor([userMsg]))
      const reply: ChatItem = {
        id: makeId(),
        role: 'assistant',
        content: resp.content,
        citations: resp.citations?.length ? resp.citations : undefined,
        cards: resp.cards?.length ? resp.cards : undefined,
        pendingActions:
          resp.type === 'confirm' && resp.pendingActions?.length
            ? resp.pendingActions
            : undefined,
        agent: resp.agent || undefined,
      }
      setItems((prev) => [...prev, reply])
    } catch {
      setItems((prev) => [
        ...prev,
        { id: makeId(), role: 'assistant', content: '⚠️ The assistant service is unreachable.' },
      ])
    } finally {
      setBusy(false)
      scrollToEnd()
    }
  }

  async function confirmAction(item: ChatItem) {
    if (!item.pendingActions || busy) return
    setBusy(true)
    setItems((prev) =>
      prev.map((m) => (m.id === item.id ? { ...m, resolved: 'confirmed' } : m)),
    )
    try {
      const resp = await assistantConfirm(item.pendingActions)
      setItems((prev) => [
        ...prev,
        { id: makeId(), role: 'assistant', content: resp.content, agent: resp.agent || undefined },
      ])
    } catch {
      setItems((prev) => [
        ...prev,
        { id: makeId(), role: 'assistant', content: '⚠️ The action could not be completed.' },
      ])
    } finally {
      setBusy(false)
      scrollToEnd()
    }
  }

  function cancelAction(item: ChatItem) {
    setItems((prev) =>
      prev.map((m) => (m.id === item.id ? { ...m, resolved: 'cancelled' } : m)),
    )
    setItems((prev) => [
      ...prev,
      { id: makeId(), role: 'assistant', content: 'Okay, cancelled — no changes made.' },
    ])
    scrollToEnd()
  }

  return (
    <div className="page chat-page">
      <header className="page-header assistant-header">
        <div>
          <h1>Assistant</h1>
          <p className="muted">
            On-device copilot · Qwen via Ollama · Qdrant retrieval · air-gapped
          </p>
        </div>
        <button
          type="button"
          className="btn ghost"
          disabled={busy}
          onClick={reset}
        >
          New chat
        </button>
      </header>

      <div className="chat-window">
        <div className="chat-messages" ref={listRef}>
          {items.map((m) =>
            m.pendingActions ? (
              <div key={m.id} className="confirm-card">
                <div className="confirm-title">
                  {m.agent && <span className="agent-badge">{m.agent}</span>}
                  Confirm {m.pendingActions.length > 1 ? `${m.pendingActions.length} actions` : 'action'}
                </div>
                {m.pendingActions.length > 1 ? (
                  <ul className="confirm-list">
                    {m.pendingActions.map((a, i) => (
                      <li key={i}>{a.summary}</li>
                    ))}
                  </ul>
                ) : (
                  <p className="confirm-summary">{m.pendingActions[0].summary}</p>
                )}
                {m.resolved ? (
                  <span className="muted small">
                    {m.resolved === 'confirmed' ? 'Confirmed.' : 'Cancelled.'}
                  </span>
                ) : (
                  <div className="confirm-actions">
                    <button
                      type="button"
                      className="btn active"
                      disabled={busy}
                      onClick={() => confirmAction(m)}
                    >
                      Confirm
                    </button>
                    <button
                      type="button"
                      className="btn ghost"
                      disabled={busy}
                      onClick={() => cancelAction(m)}
                    >
                      Cancel
                    </button>
                  </div>
                )}
              </div>
            ) : (
              <div key={m.id} className="chat-row">
                {m.role === 'assistant' && m.agent && (
                  <span className="agent-badge">{m.agent}</span>
                )}
                <div className={`chat-bubble ${m.role}`}>
                  {m.role === 'assistant' ? (
                    <div className="markdown">
                      <ReactMarkdown remarkPlugins={[remarkGfm]}>
                        {m.content}
                      </ReactMarkdown>
                    </div>
                  ) : (
                    m.content
                  )}
                  {m.citations && (
                    <div className="muted small citations">
                      grounded on: {m.citations.join(', ')}
                    </div>
                  )}
                </div>
                {m.cards && <DataCards cards={m.cards} />}
              </div>
            ),
          )}
          {busy && <div className="chat-bubble assistant typing">…thinking</div>}
        </div>

        <form
          className="chat-input"
          onSubmit={(e) => {
            e.preventDefault()
            send()
          }}
        >
          <input
            type="text"
            value={input}
            placeholder="Ask about devices, savings, or request an action…"
            disabled={busy}
            onChange={(e) => setInput(e.target.value)}
          />
          <button type="submit" className="btn active" disabled={!input.trim() || busy}>
            Send
          </button>
        </form>
      </div>
    </div>
  )
}
