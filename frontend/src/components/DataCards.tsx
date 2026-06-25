import type { ReactNode } from 'react'
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import type { DataCard } from '../types'

// Renders structured tool data as rich cards. Each `kind` maps to a typed
// renderer; the payloads come from the orchestrator's data tools.

type Dict = Record<string, unknown>

const num = (v: unknown): number => (typeof v === 'number' ? v : Number(v ?? 0))
const str = (v: unknown): string => (v == null ? '' : String(v))

// Chart theme (Recharts can't read CSS vars from SVG, so use hex literals).
const ACCENT = '#2dd4bf'
const AXIS = '#8b98a8'
const GRID = '#2c3744'
const TOOLTIP = {
  background: '#1a212b',
  border: '1px solid #2c3744',
  borderRadius: 8,
  color: '#e6edf3',
}

function Tile({ label, value }: { label: string; value: string }) {
  return (
    <div className="stat-card">
      <span className="stat-label">{label}</span>
      <span className="stat-value">{value}</span>
    </div>
  )
}

function SavingsSummaryCard({ p }: { p: Dict }) {
  const isTotals = 'totalUsageKwh' in p || 'totalCostSaved' in p
  const title = isTotals
    ? 'Totals'
    : `Period ${str(p.period)}`
  const usageKwh = num(isTotals ? p.totalUsageKwh : p.usageKwh)
  const usageCost = num(isTotals ? p.totalUsageCost : p.usageCost)
  const energySaved = num(isTotals ? p.totalEnergySavedKwh : p.energySavedKwh)
  const costSaved = num(isTotals ? p.totalCostSaved : p.costSaved)
  return (
    <div className="chat-card">
      <div className="chat-card-title">Energy &amp; savings · {title}</div>
      <div className="chat-card-tiles">
        <Tile label="Energy used" value={`${usageKwh.toFixed(1)} kWh`} />
        <Tile label="Cost of usage" value={`$${usageCost.toFixed(2)}`} />
        <Tile label="Energy saved" value={`${energySaved.toFixed(1)} kWh`} />
        <Tile label="Cost saved" value={`$${costSaved.toFixed(2)}`} />
      </div>
    </div>
  )
}

function DeviceListCard({ p }: { p: Dict }) {
  const devices = Array.isArray(p.devices) ? (p.devices as Dict[]) : []
  return (
    <div className="chat-card">
      <div className="chat-card-title">Devices ({devices.length})</div>
      <table className="data-table">
        <tbody>
          {devices.map((d) => (
            <tr key={str(d.id)}>
              <td>
                <strong>{str(d.name)}</strong>
                <div className="muted small">{str(d.location)}</div>
              </td>
              <td>
                <span className={`badge ${str(d.status)}`}>{str(d.status)}</span>
              </td>
              <td className="muted">{str(d.algorithm)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function DeviceCountCard({ p }: { p: Dict }) {
  const matched = Array.isArray(p.matched) ? (p.matched as unknown[]).map(str) : []
  const filters = (p.filters ?? {}) as Dict
  const activeFilters = Object.entries(filters).filter(([, v]) => str(v) !== 'any')
  return (
    <div className="chat-card">
      <div className="chat-card-title">Device count</div>
      <div className="chat-card-count">{num(p.count)}</div>
      {activeFilters.length > 0 && (
        <div className="chip-row">
          {activeFilters.map(([k, v]) => (
            <span key={k} className="chip">
              {k}: {str(v)}
            </span>
          ))}
        </div>
      )}
      {matched.length > 0 && <div className="muted small">{matched.join(', ')}</div>}
    </div>
  )
}

function DeviceDetailCard({ p }: { p: Dict }) {
  return (
    <div className="chat-card">
      <div className="chat-card-title">
        {str(p.name)}{' '}
        <span className={`badge ${str(p.status)}`}>{str(p.status)}</span>
      </div>
      <div className="muted small">{str(p.location)}</div>
      <div className="chat-card-tiles">
        {'currentTempC' in p && <Tile label="Current" value={`${num(p.currentTempC).toFixed(1)}°C`} />}
        {'setpointC' in p && <Tile label="Setpoint" value={`${num(p.setpointC)}°C`} />}
        {'powerKw' in p && <Tile label="Power" value={`${num(p.powerKw).toFixed(2)} kW`} />}
        <Tile label="Algorithm" value={str(p.algorithm) || 'none'} />
      </div>
    </div>
  )
}

function UsageHistoryCard({ p }: { p: Dict }) {
  const rows = Array.isArray(p.rows) ? (p.rows as Dict[]) : []
  return (
    <div className="chat-card">
      <div className="chat-card-title">
        Usage history · {str(p.scope)} · last {num(p.days)} days
      </div>
      <table className="data-table">
        <thead>
          <tr>
            <th>Date</th>
            <th>Usage</th>
            <th>Usage cost</th>
            <th>Energy saved</th>
            <th>Cost saved</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((r) => (
            <tr key={str(r.period)}>
              <td>{str(r.period)}</td>
              <td>{num(r.usageKwh).toFixed(1)} kWh</td>
              <td>${num(r.usageCost).toFixed(2)}</td>
              <td>{num(r.energySavedKwh).toFixed(1)} kWh</td>
              <td>${num(r.costSaved).toFixed(2)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <div className="muted small">
        Total: {num(p.totalUsageKwh).toFixed(1)} kWh · ${num(p.totalUsageCost).toFixed(2)}
      </div>
    </div>
  )
}

function CompareCard({ p }: { p: Dict }) {
  const a = (p.periodA ?? {}) as Dict
  const b = (p.periodB ?? {}) as Dict
  const col = (rec: Dict) => (
    <div className="compare-col">
      <div className="chat-card-title">{str(rec.period)}</div>
      <div className="muted small">Usage: {num(rec.usageKwh).toFixed(1)} kWh</div>
      <div className="muted small">Usage cost: ${num(rec.usageCost).toFixed(2)}</div>
      <div className="muted small">Saved: {num(rec.energySavedKwh).toFixed(1)} kWh</div>
      <div className="muted small">Cost saved: ${num(rec.costSaved).toFixed(2)}</div>
    </div>
  )
  return (
    <div className="chat-card">
      <div className="chat-card-title">Comparison</div>
      <div className="compare-row">
        {col(a)}
        {col(b)}
      </div>
      <div className="muted small">
        Δ cost saved: ${num(p.deltaCostSaved).toFixed(2)} · Δ usage: {num(p.deltaUsageKwh).toFixed(1)} kWh
      </div>
    </div>
  )
}

// --- Adaptive SQL result card: pick a viz from the data shape ---

type ColKind = 'date' | 'number' | 'text'
const DATE_RE = /^\d{4}-\d{2}(-\d{2})?$/

function isNumeric(v: unknown): boolean {
  if (v == null || v === '') return false
  if (typeof v === 'number') return Number.isFinite(v)
  return typeof v === 'string' && v.trim() !== '' && Number.isFinite(Number(v))
}

function classify(col: string, rows: Dict[]): ColKind {
  const vals = rows.map((r) => r[col]).filter((v) => v != null && v !== '')
  if (vals.length === 0) return 'text'
  if (vals.every((v) => DATE_RE.test(String(v)))) return 'date'
  if (vals.every(isNumeric)) return 'number'
  return 'text'
}

function distinct(rows: Dict[], col: string): number {
  return new Set(rows.map((r) => str(r[col]))).size
}

function ScalarViz({ label, value }: { label: string; value: unknown }) {
  return (
    <div className="scalar-viz">
      <span className="chat-card-count">{isNumeric(value) ? num(value) : str(value)}</span>
      <span className="muted small">{label}</span>
    </div>
  )
}

function BarViz({ rows, xCol, yCol }: { rows: Dict[]; xCol: string; yCol: string }) {
  const data = rows.slice(0, 12).map((r) => ({ x: str(r[xCol]), y: num(r[yCol]) }))
  return (
    <ResponsiveContainer width="100%" height={200}>
      <BarChart data={data} margin={{ top: 8, right: 8, bottom: 0, left: -8 }}>
        <CartesianGrid stroke={GRID} vertical={false} />
        <XAxis dataKey="x" tick={{ fill: AXIS, fontSize: 11 }} />
        <YAxis tick={{ fill: AXIS, fontSize: 11 }} />
        <Tooltip contentStyle={TOOLTIP} cursor={{ fill: 'rgba(45,212,191,0.08)' }} />
        <Bar dataKey="y" name={yCol} fill={ACCENT} radius={[4, 4, 0, 0]} />
      </BarChart>
    </ResponsiveContainer>
  )
}

function LineViz({ rows, xCol, yCol }: { rows: Dict[]; xCol: string; yCol: string }) {
  const data = [...rows]
    .sort((a, b) => str(a[xCol]).localeCompare(str(b[xCol])))
    .map((r) => ({ x: str(r[xCol]).slice(5), y: num(r[yCol]) }))
  return (
    <ResponsiveContainer width="100%" height={200}>
      <AreaChart data={data} margin={{ top: 8, right: 8, bottom: 0, left: -8 }}>
        <CartesianGrid stroke={GRID} vertical={false} />
        <XAxis dataKey="x" tick={{ fill: AXIS, fontSize: 11 }} />
        <YAxis tick={{ fill: AXIS, fontSize: 11 }} />
        <Tooltip contentStyle={TOOLTIP} />
        <Area
          type="monotone"
          dataKey="y"
          name={yCol}
          stroke={ACCENT}
          fill={ACCENT}
          fillOpacity={0.2}
        />
      </AreaChart>
    </ResponsiveContainer>
  )
}

function TableViz({ columns, rows }: { columns: string[]; rows: Dict[] }) {
  return (
    <table className="data-table">
      <thead>
        <tr>
          {columns.map((c) => (
            <th key={c}>{c}</th>
          ))}
        </tr>
      </thead>
      <tbody>
        {rows.map((r, i) => (
          <tr key={i}>
            {columns.map((c) => (
              <td key={c}>{str(r[c])}</td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  )
}

function QueryResultCard({ p }: { p: Dict }) {
  const columns = Array.isArray(p.columns) ? (p.columns as string[]) : []
  const rows = Array.isArray(p.rows) ? (p.rows as Dict[]) : []
  const sql = str(p.sql)
  if (!sql) {
    return null
  }

  const kinds = Object.fromEntries(columns.map((c) => [c, classify(c, rows)]))
  const dateCols = columns.filter((c) => kinds[c] === 'date')
  const numCols = columns.filter((c) => kinds[c] === 'number')
  const textCols = columns.filter((c) => kinds[c] === 'text')
  const maxCategory = textCols.length
    ? Math.max(...textCols.map((c) => distinct(rows, c)))
    : 0

  let body: ReactNode
  if (rows.length === 0) {
    body = <div className="muted small">No rows returned.</div>
  } else if (rows.length === 1 && columns.length === 1) {
    body = <ScalarViz label={columns[0]} value={rows[0][columns[0]]} />
  } else if (dateCols.length >= 1 && numCols.length >= 1 && rows.length > 1 && maxCategory <= 1) {
    body = <LineViz rows={rows} xCol={dateCols[0]} yCol={numCols[0]} />
  } else if (textCols.length >= 1 && numCols.length >= 1 && rows.length <= 12) {
    body = <BarViz rows={rows} xCol={textCols[0]} yCol={numCols[0]} />
  } else {
    body = <TableViz columns={columns} rows={rows} />
  }

  return (
    <div className="chat-card">
      <div className="chat-card-title">
        Query{' '}
        <span className="muted small">
          · {rows.length} row{rows.length === 1 ? '' : 's'}
        </span>
      </div>
      {body}
      <code className="query-sql">{sql}</code>
    </div>
  )
}

function renderCard(card: DataCard, key: number) {
  const p = card.payload
  switch (card.kind) {
    case 'savings_summary':
      return <SavingsSummaryCard key={key} p={p} />
    case 'device_list':
      return <DeviceListCard key={key} p={p} />
    case 'device_count':
      return <DeviceCountCard key={key} p={p} />
    case 'device_detail':
      return <DeviceDetailCard key={key} p={p} />
    case 'usage_history':
      return <UsageHistoryCard key={key} p={p} />
    case 'query_result':
      return <QueryResultCard key={key} p={p} />
    case 'savings_compare':
      return <CompareCard key={key} p={p} />
    default:
      return null
  }
}

export default function DataCards({ cards }: { cards: DataCard[] }) {
  if (!cards || cards.length === 0) {
    return null
  }
  return <>{cards.map((c, i) => renderCard(c, i))}</>
}
