import { useEffect, useState } from 'react'
import { getSavingsSummary } from '../services/api'
import type { SavingsRecord, SavingsSummary } from '../types'

type View = 'daily' | 'monthly'
type Metric = 'usageKwh' | 'usageCost' | 'energySavedKwh' | 'costSaved'

interface MetricDef {
  key: Metric
  label: string
  /** Format a value for display (with unit). */
  format: (v: number) => string
}

const METRICS: MetricDef[] = [
  { key: 'usageKwh', label: 'Usage (kWh)', format: (v) => `${v.toFixed(1)} kWh` },
  { key: 'usageCost', label: 'Usage cost', format: (v) => `$${v.toFixed(2)}` },
  { key: 'energySavedKwh', label: 'Energy saved', format: (v) => `${v.toFixed(1)} kWh` },
  { key: 'costSaved', label: 'Cost saved', format: (v) => `$${v.toFixed(2)}` },
]

function BarChart({
  records,
  metric,
}: {
  records: SavingsRecord[]
  metric: MetricDef
}) {
  const max = Math.max(...records.map((r) => r[metric.key]), 1)
  return (
    <div className="bar-chart">
      {records.map((r) => (
        <div
          key={r.period}
          className="bar-col"
          title={`${r.period}: ${metric.format(r[metric.key])}`}
        >
          <div className="bar-track">
            <div
              className="bar-fill"
              style={{ height: `${(r[metric.key] / max) * 100}%` }}
            />
          </div>
          <span className="bar-label">{r.period.slice(5)}</span>
        </div>
      ))}
    </div>
  )
}

export default function Savings() {
  const [summary, setSummary] = useState<SavingsSummary | null>(null)
  const [view, setView] = useState<View>('daily')
  const [metricKey, setMetricKey] = useState<Metric>('usageKwh')

  useEffect(() => {
    getSavingsSummary().then(setSummary)
  }, [])

  if (!summary) {
    return <p className="muted">Loading savings…</p>
  }

  const records = view === 'daily' ? summary.daily : summary.monthly
  const metric = METRICS.find((m) => m.key === metricKey) ?? METRICS[0]

  return (
    <div className="page">
      <header className="page-header">
        <h1>Savings &amp; Usage</h1>
        <p className="muted">Energy used and saved by the applied algorithms.</p>
      </header>

      <section className="stat-grid">
        <div className="stat-card">
          <span className="stat-label">Total energy used</span>
          <span className="stat-value">
            {summary.totalUsageKwh.toFixed(1)} <small>kWh</small>
          </span>
        </div>
        <div className="stat-card">
          <span className="stat-label">Total usage cost</span>
          <span className="stat-value">${summary.totalUsageCost.toFixed(2)}</span>
        </div>
        <div className="stat-card">
          <span className="stat-label">Total energy saved</span>
          <span className="stat-value">
            {summary.totalEnergySavedKwh.toFixed(1)} <small>kWh</small>
          </span>
        </div>
        <div className="stat-card">
          <span className="stat-label">Total cost saved</span>
          <span className="stat-value">${summary.totalCostSaved.toFixed(2)}</span>
        </div>
      </section>

      <section className="panel">
        <div className="panel-header">
          <div className="toggle">
            {METRICS.map((m) => (
              <button
                key={m.key}
                type="button"
                className={metricKey === m.key ? 'btn active' : 'btn'}
                onClick={() => setMetricKey(m.key)}
              >
                {m.label}
              </button>
            ))}
          </div>
          <div className="toggle">
            <button
              type="button"
              className={view === 'daily' ? 'btn active' : 'btn'}
              onClick={() => setView('daily')}
            >
              Daily
            </button>
            <button
              type="button"
              className={view === 'monthly' ? 'btn active' : 'btn'}
              onClick={() => setView('monthly')}
            >
              Monthly
            </button>
          </div>
        </div>
        <BarChart records={records} metric={metric} />
      </section>

      <section className="panel">
        <div className="panel-header">
          <h2>{view === 'daily' ? 'Daily' : 'Monthly'} breakdown</h2>
        </div>
        <table className="data-table">
          <thead>
            <tr>
              <th>Period</th>
              <th>Usage</th>
              <th>Usage cost</th>
              <th>Energy saved</th>
              <th>Cost saved</th>
            </tr>
          </thead>
          <tbody>
            {[...records].reverse().map((r) => (
              <tr key={r.period}>
                <td>{r.period}</td>
                <td>{r.usageKwh.toFixed(1)} kWh</td>
                <td>${r.usageCost.toFixed(2)}</td>
                <td>{r.energySavedKwh.toFixed(1)} kWh</td>
                <td>${r.costSaved.toFixed(2)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  )
}
