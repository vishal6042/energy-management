import type {
  AlgorithmType,
  AssistantResponse,
  Device,
  DeviceSavings,
  DeviceStatus,
  PendingAction,
  SavingsSummary,
} from '../types'

// Talks to the Spring Boot backend. In dev, Vite proxies /api → :8080
// (see vite.config.ts); in prod set VITE_API_BASE to the backend origin.
const BASE = `${import.meta.env.VITE_API_BASE ?? ''}/api`

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...init,
  })
  if (!res.ok) {
    throw new Error(`${init?.method ?? 'GET'} ${path} failed: ${res.status}`)
  }
  return res.json() as Promise<T>
}

export function getDevices(): Promise<Device[]> {
  return request<Device[]>('/devices')
}

export function getDevice(id: string): Promise<Device> {
  return request<Device>(`/devices/${id}`)
}

export function applyAlgorithm(
  deviceId: string,
  algorithm: AlgorithmType,
): Promise<Device> {
  return request<Device>(`/devices/${deviceId}/algorithm`, {
    method: 'PUT',
    body: JSON.stringify({ algorithm }),
  })
}

export function setDeviceStatus(
  deviceId: string,
  status: DeviceStatus,
): Promise<Device> {
  return request<Device>(`/devices/${deviceId}/status`, {
    method: 'PUT',
    body: JSON.stringify({ status }),
  })
}

export function getSavingsSummary(): Promise<SavingsSummary> {
  return request<SavingsSummary>('/savings/summary')
}

export function getDeviceSavings(): Promise<DeviceSavings[]> {
  return request<DeviceSavings[]>('/savings/devices')
}

// --- Assistant (orchestrator microservice, proxied via /api/assistant) ---

type WireMessage = { role: 'user' | 'assistant'; content: string }

export function assistantChat(
  messages: WireMessage[],
): Promise<AssistantResponse> {
  return request<AssistantResponse>('/assistant/chat', {
    method: 'POST',
    body: JSON.stringify({ messages }),
  })
}

export function assistantConfirm(
  actions: PendingAction[],
): Promise<AssistantResponse> {
  return request<AssistantResponse>('/assistant/confirm', {
    method: 'POST',
    body: JSON.stringify({ actions }),
  })
}
