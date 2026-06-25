// Core domain types for DEMS (Device Energy Management System)

/** Energy-saving algorithms that can be applied to a device. */
export type AlgorithmType = 'comfort' | 'target' | 'none'

export interface AlgorithmInfo {
  type: AlgorithmType
  name: string
  description: string
}

export type DeviceStatus = 'online' | 'offline'

/** An AC device that can have an energy-saving algorithm applied. */
export interface Device {
  id: string
  name: string
  location: string
  model: string
  status: DeviceStatus
  /** Current measured room temperature in °C. */
  currentTempC: number
  /** Configured setpoint in °C. */
  setpointC: number
  /** Instantaneous power draw in watts (0 when offline). */
  powerW: number
  /** Rated power draw in watts (restored when turned on). */
  nominalPowerW: number
  /** Algorithm currently applied to this device. */
  algorithm: AlgorithmType
}

/** A single data point (usage + savings) for a period. */
export interface SavingsRecord {
  /** ISO date string (YYYY-MM-DD for daily, YYYY-MM for monthly). */
  period: string
  /** Money saved in the period, in the account currency. */
  costSaved: number
  /** Energy saved in the period, in kWh. */
  energySavedKwh: number
  /** Energy consumed in the period, in kWh. */
  usageKwh: number
  /** Cost of the energy consumed in the period. */
  usageCost: number
}

export interface SavingsSummary {
  totalCostSaved: number
  totalEnergySavedKwh: number
  totalUsageKwh: number
  totalUsageCost: number
  daily: SavingsRecord[]
  monthly: SavingsRecord[]
}

/** Per-device savings series, used for charts and device filtering. */
export interface DeviceSavings {
  deviceId: string
  daily: SavingsRecord[]
  monthly: SavingsRecord[]
}

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: number
}

/** A drafted, not-yet-executed device action awaiting confirmation (HITL). */
export interface PendingAction {
  tool: string
  args: Record<string, unknown>
  summary: string
}

/** A structured, typed payload rendered as a rich card in the chat. */
export interface DataCard {
  kind: string
  payload: Record<string, unknown>
}

/** Orchestrator chat reply: a normal message or a HITL confirmation request. */
export interface AssistantResponse {
  type: 'message' | 'confirm'
  content: string
  citations: string[]
  cards: DataCard[]
  pendingActions: PendingAction[]
  /** Which agent produced this response, e.g. "SQL Agent", "Knowledge (RAG)", "Action". */
  agent: string
}
