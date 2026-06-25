import { useEffect, useState } from 'react'
import { applyAlgorithm, getDevices, setDeviceStatus } from '../services/api'
import { ALGORITHMS, algorithmName } from '../data/mock'
import type { AlgorithmType, Device } from '../types'

export default function Devices() {
  const [devices, setDevices] = useState<Device[]>([])
  const [loading, setLoading] = useState(true)
  const [savingId, setSavingId] = useState<string | null>(null)

  useEffect(() => {
    getDevices().then((d) => {
      setDevices(d)
      setLoading(false)
    })
  }, [])

  async function onApply(deviceId: string, algorithm: AlgorithmType) {
    setSavingId(deviceId)
    const updated = await applyAlgorithm(deviceId, algorithm)
    setDevices((prev) => prev.map((d) => (d.id === deviceId ? updated : d)))
    setSavingId(null)
  }

  async function onTogglePower(device: Device) {
    setSavingId(device.id)
    const next = device.status === 'online' ? 'offline' : 'online'
    const updated = await setDeviceStatus(device.id, next)
    setDevices((prev) => prev.map((d) => (d.id === device.id ? updated : d)))
    setSavingId(null)
  }

  if (loading) {
    return <p className="muted">Loading devices…</p>
  }

  return (
    <div className="page">
      <header className="page-header">
        <h1>Devices</h1>
        <p className="muted">
          Apply a single energy-saving algorithm to each AC device.
        </p>
      </header>

      <div className="card-grid">
        {devices.map((d) => (
          <div key={d.id} className="device-card">
            <div className="device-card-head">
              <div>
                <strong>{d.name}</strong>
                <div className="muted small">{d.location} · {d.model}</div>
              </div>
              <div className="device-card-actions">
                <span className={`badge ${d.status}`}>{d.status}</span>
                <button
                  type="button"
                  className={d.status === 'online' ? 'btn ghost' : 'btn active'}
                  disabled={savingId === d.id}
                  onClick={() => onTogglePower(d)}
                >
                  {d.status === 'online' ? 'Turn off' : 'Turn on'}
                </button>
              </div>
            </div>

            <dl className="device-metrics">
              <div>
                <dt>Current</dt>
                <dd>{d.currentTempC.toFixed(1)}°C</dd>
              </div>
              <div>
                <dt>Setpoint</dt>
                <dd>{d.setpointC}°C</dd>
              </div>
              <div>
                <dt>Power</dt>
                <dd>{(d.powerW / 1000).toFixed(2)} kW</dd>
              </div>
            </dl>

            <div className="algo-row">
              <span className="muted small">
                Algorithm: <strong>{algorithmName(d.algorithm)}</strong>
              </span>
              <div className="algo-buttons">
                {ALGORITHMS.map((a) => (
                  <button
                    key={a.type}
                    type="button"
                    title={a.description}
                    disabled={d.status === 'offline' || savingId === d.id}
                    className={d.algorithm === a.type ? 'btn active' : 'btn'}
                    onClick={() => onApply(d.id, a.type)}
                  >
                    {a.name}
                  </button>
                ))}
                <button
                  type="button"
                  disabled={d.algorithm === 'none' || savingId === d.id}
                  className="btn ghost"
                  onClick={() => onApply(d.id, 'none')}
                >
                  Clear
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
