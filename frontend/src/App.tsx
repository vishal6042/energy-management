import { Route, Routes } from 'react-router-dom'
import Layout from './components/Layout'
import Dashboard from './pages/Dashboard'
import Devices from './pages/Devices'
import Savings from './pages/Savings'
import Assistant from './pages/Assistant'
import './App.css'

function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Dashboard />} />
        <Route path="devices" element={<Devices />} />
        <Route path="savings" element={<Savings />} />
        <Route path="assistant" element={<Assistant />} />
      </Route>
    </Routes>
  )
}

export default App
