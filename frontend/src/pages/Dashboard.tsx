import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Area,
  CartesianGrid,
  ComposedChart,
  Legend,
  Line,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { getDeviceSavings, getDevices, setDeviceStatus } from '../services/api'
import { algorithmName } from '../data/mock'
import type { Device, DeviceSavings, SavingsRecord } from '../types'

type Granularity = 'daily' | 'monthly'

const COLORS = {
  energy: '#2dd4bf',
  usage: '#60a5fa',
  cost: '#f59e0b',
  axis: '#8b98a8',
  grid: '#2c3744',
}

const tooltipStyle = {
  background: '#1a212b',
  border: '1px solid #2c3744',
  borderRadius: 8,
  color: '#e6edf3',
}

/** YYYY-MM-DD → MM-DD, YYYY-MM → MM, for compact axis labels. */
function shortLabel(period: string): string {
  return period.slice(5)
}

/** Tooltip formatter: cost series in $, energy series in kWh. */
function formatMetric(value: unknown, name: unknown): [string, string] {
  const v = Number(value)
  const label = String(name)
  if (label.toLowerCase().includes('cost')) {
    return [`$${v.toFixed(2)}`, label]
  }
  return [`${v.toFixed(1)} kWh`, label]
}

export default function Dashboard() {
  const [devices, setDevices] = useState<Device[]>([])
  const [deviceSavings, setDeviceSavings] = useState<DeviceSavings[]>([])
  const [loading, setLoading] = useState(true)

  const [granularity, setGranularity] = useState<Granularity>('monthly')
  const [deviceId, setDeviceId] = useState<string>('all')
  const [fromPeriod, setFromPeriod] = useState('')
  const [toPeriod, setToPeriod] = useState('')
  const [busyId, setBusyId] = useState<string | null>(null)

  function reload() {
    return Promise.all([getDevices(), getDeviceSavings()]).then(([d, s]) => {
      setDevices(d)
      setDeviceSavings(s)
      setLoading(false)
    })
  }

  useEffect(() => {
    reload()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function togglePower(device: Device) {
    setBusyId(device.id)
    const next = device.status === 'online' ? 'offline' : 'online'
    await setDeviceStatus(device.id, next)
    await reload()
    setBusyId(null)
  }

  const seriesFor = (ds: DeviceSavings): SavingsRecord[] =>
    granularity === 'daily' ? ds.daily : ds.monthly

  // Periods available for the current granularity (recomputed on toggle).
  const periods = useMemo(() => {
    const set = new Set<string>()
    deviceSavings.forEach((ds) =>
      seriesFor(ds).forEach((r) => set.add(r.period)),
    )
    return [...set].sort()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [deviceSavings, granularity])

  // Reset the from/to range whenever the available periods change.
  useEffect(() => {
    if (periods.length) {
      setFromPeriod(periods[0])
      setToPeriod(periods[periods.length - 1])
    }
  }, [periods])

  const selectedDevices = useMemo(
    () =>
      deviceId === 'all'
        ? deviceSavings
        : deviceSavings.filter((d) => d.deviceId === deviceId),
    [deviceSavings, deviceId],
  )

  const inRange = (p: string) =>
    (!fromPeriod || p >= fromPeriod) && (!toPeriod || p <= toPeriod)

  // All four metrics aggregated per period across the selected device set.
  const trend = useMemo(() => {
    const map = new Map<
      string,
      { energySavedKwh: number; costSaved: number; usageKwh: number; usageCost: number }
    >()
    selectedDevices.forEach((ds) =>
      seriesFor(ds).forEach((r) => {
        if (!inRange(r.period)) return
        const acc =
          map.get(r.period) ??
          { energySavedKwh: 0, costSaved: 0, usageKwh: 0, usageCost: 0 }
        acc.energySavedKwh += r.energySavedKwh
        acc.costSaved += r.costSaved
        acc.usageKwh += r.usageKwh
        acc.usageCost += r.usageCost
        map.set(r.period, acc)
      }),
    )
    return [...map.entries()]
      .sort((a, b) => a[0].localeCompare(b[0]))
      .map(([period, acc]) => ({
        period,
        label: shortLabel(period),
        energySavedKwh: Math.round(acc.energySavedKwh * 10) / 10,
        costSaved: Math.round(acc.costSaved * 100) / 100,
        usageKwh: Math.round(acc.usageKwh * 10) / 10,
        usageCost: Math.round(acc.usageCost * 100) / 100,
      }))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedDevices, granularity, fromPeriod, toPeriod])

  if (loading) {
    return <p className="muted">Loading dashboard…</p>
  }

  const totalEnergySaved = trend.reduce((s, r) => s + r.energySavedKwh, 0)
  const totalCostSaved = trend.reduce((s, r) => s + r.costSaved, 0)
  const totalUsage = trend.reduce((s, r) => s + r.usageKwh, 0)
  const totalUsageCost = trend.reduce((s, r) => s + r.usageCost, 0)
  const online = devices.filter((d) => d.status === 'online').length
  const offline = devices.length - online
  const periodLabel = granularity === 'monthly' ? 'Monthly' : 'Daily'

  return (
    <div className="page">
      <header className="page-header">
        <h1>Dashboard</h1>
        <p className="muted">Overview of devices, usage and savings.</p>
      </header>

      {/* ---- Filters ---- */}
      <div className="filter-bar">
        <label>
          <span className="muted small">Device</span>
          <select value={deviceId} onChange={(e) => setDeviceId(e.target.value)}>
            <option value="all">All devices</option>
            {devices.map((d) => (
              <option key={d.id} value={d.id}>
                {d.name}
              </option>
            ))}
          </select>
        </label>

        <div className="toggle">
          <button
            type="button"
            className={granularity === 'daily' ? 'btn active' : 'btn'}
            onClick={() => setGranularity('daily')}
          >
            Daily
          </button>
          <button
            type="button"
            className={granularity === 'monthly' ? 'btn active' : 'btn'}
            onClick={() => setGranularity('monthly')}
          >
            Monthly
          </button>
        </div>

        <label>
          <span className="muted small">From</span>
          <select
            value={fromPeriod}
            onChange={(e) => setFromPeriod(e.target.value)}
          >
            {periods
              .filter((p) => !toPeriod || p <= toPeriod)
              .map((p) => (
                <option key={p} value={p}>
                  {p}
                </option>
              ))}
          </select>
        </label>

        <label>
          <span className="muted small">To</span>
          <select value={toPeriod} onChange={(e) => setToPeriod(e.target.value)}>
            {periods
              .filter((p) => !fromPeriod || p >= fromPeriod)
              .map((p) => (
                <option key={p} value={p}>
                  {p}
                </option>
              ))}
          </select>
        </label>
      </div>

      {/* ---- Summary: Usage / Saving sections + Devices card ---- */}
      <div className="summary-row">
        <section className="summary-group">
          <h3 className="summary-title">Usage</h3>
          <div className="stat-grid">
            <div className="stat-card">
              <span className="stat-label">Energy used</span>
              <span className="stat-value">
                {totalUsage.toFixed(1)} <small>kWh</small>
              </span>
            </div>
            <div className="stat-card">
              <span className="stat-label">Cost of usage</span>
              <span className="stat-value">${totalUsageCost.toFixed(2)}</span>
            </div>
          </div>
        </section>

        <section className="summary-group">
          <h3 className="summary-title">Saving</h3>
          <div className="stat-grid">
            <div className="stat-card">
              <span className="stat-label">Energy saved</span>
              <span className="stat-value">
                {totalEnergySaved.toFixed(1)} <small>kWh</small>
              </span>
            </div>
            <div className="stat-card">
              <span className="stat-label">Cost saved</span>
              <span className="stat-value">${totalCostSaved.toFixed(2)}</span>
            </div>
          </div>
        </section>

        <section className="summary-group">
          <h3 className="summary-title">Devices</h3>
          <div className="stat-card devices-card">
            <span className="stat-value">
              {online} <small>online</small>
            </span>
            <span className="muted small">
              <span className="status-dot offline" /> {offline} offline ·{' '}
              {devices.length} total
            </span>
          </div>
        </section>
      </div>

      {/* ---- Saving chart (area + line) ---- */}
      <section className="panel">
        <div className="panel-header">
          <h2>{periodLabel} savings — energy &amp; cost</h2>
        </div>
        <ResponsiveContainer width="100%" height={280}>
          <ComposedChart data={trend} margin={{ top: 8, right: 8, bottom: 0, left: -8 }}>
            <CartesianGrid stroke={COLORS.grid} vertical={false} />
            <XAxis dataKey="label" tick={{ fill: COLORS.axis, fontSize: 12 }} />
            <YAxis
              yAxisId="kwh"
              tick={{ fill: COLORS.axis, fontSize: 12 }}
              tickFormatter={(v) => `${v}`}
            />
            <YAxis
              yAxisId="cost"
              orientation="right"
              tick={{ fill: COLORS.axis, fontSize: 12 }}
              tickFormatter={(v) => `$${v}`}
            />
            <Tooltip contentStyle={tooltipStyle} formatter={formatMetric} />
            <Legend />
            <Area
              yAxisId="kwh"
              type="monotone"
              dataKey="energySavedKwh"
              name="Energy saved"
              stroke={COLORS.energy}
              fill={COLORS.energy}
              fillOpacity={0.2}
            />
            <Line
              yAxisId="cost"
              type="monotone"
              dataKey="costSaved"
              name="Cost saved"
              stroke={COLORS.cost}
              strokeWidth={2}
              dot={false}
            />
          </ComposedChart>
        </ResponsiveContainer>
      </section>

      {/* ---- Usage chart (area + line) ---- */}
      <section className="panel">
        <div className="panel-header">
          <h2>{periodLabel} usage — energy &amp; cost</h2>
        </div>
        <ResponsiveContainer width="100%" height={280}>
          <ComposedChart data={trend} margin={{ top: 8, right: 8, bottom: 0, left: -8 }}>
            <CartesianGrid stroke={COLORS.grid} vertical={false} />
            <XAxis dataKey="label" tick={{ fill: COLORS.axis, fontSize: 12 }} />
            <YAxis
              yAxisId="kwh"
              tick={{ fill: COLORS.axis, fontSize: 12 }}
              tickFormatter={(v) => `${v}`}
            />
            <YAxis
              yAxisId="cost"
              orientation="right"
              tick={{ fill: COLORS.axis, fontSize: 12 }}
              tickFormatter={(v) => `$${v}`}
            />
            <Tooltip contentStyle={tooltipStyle} formatter={formatMetric} />
            <Legend />
            <Area
              yAxisId="kwh"
              type="monotone"
              dataKey="usageKwh"
              name="Energy used"
              stroke={COLORS.usage}
              fill={COLORS.usage}
              fillOpacity={0.2}
            />
            <Line
              yAxisId="cost"
              type="monotone"
              dataKey="usageCost"
              name="Cost of usage"
              stroke={COLORS.cost}
              strokeWidth={2}
              dot={false}
            />
          </ComposedChart>
        </ResponsiveContainer>
      </section>

      {/* ---- Device table ---- */}
      <section className="panel">
        <div className="panel-header">
          <h2>Devices</h2>
          <Link to="/devices" className="link">
            Manage →
          </Link>
        </div>
        <table className="data-table">
          <thead>
            <tr>
              <th>Device</th>
              <th>Status</th>
              <th>Temp</th>
              <th>Power</th>
              <th>Algorithm</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {devices.map((d) => (
              <tr key={d.id}>
                <td>
                  <strong>{d.name}</strong>
                  <div className="muted small">{d.location}</div>
                </td>
                <td>
                  <span className={`badge ${d.status}`}>{d.status}</span>
                </td>
                <td>{d.currentTempC.toFixed(1)}°C</td>
                <td>{(d.powerW / 1000).toFixed(2)} kW</td>
                <td>{algorithmName(d.algorithm)}</td>
                <td>
                  <button
                    type="button"
                    className={d.status === 'online' ? 'btn ghost' : 'btn active'}
                    disabled={busyId === d.id}
                    onClick={() => togglePower(d)}
                  >
                    {d.status === 'online' ? 'Turn off' : 'Turn on'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  )
}
