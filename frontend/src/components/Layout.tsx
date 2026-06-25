import { NavLink, Outlet } from 'react-router-dom'

const NAV = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/devices', label: 'Devices', end: false },
  { to: '/savings', label: 'Savings', end: false },
  { to: '/assistant', label: 'Assistant', end: false },
]

export default function Layout() {
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">⚡</span>
          <div>
            <strong>DEMS</strong>
            <small>Device Energy Management</small>
          </div>
        </div>
        <nav>
          {NAV.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                isActive ? 'nav-link active' : 'nav-link'
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-footer">
          <span className="status-dot online" /> On-device · Air-gapped
        </div>
      </aside>
      <main className="content">
        <Outlet />
      </main>
    </div>
  )
}
