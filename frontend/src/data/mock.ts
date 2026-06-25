import type { AlgorithmInfo, AlgorithmType } from '../types'

// Static algorithm metadata for the UI. Device and savings data now come from
// the backend (see services/api.ts).
export const ALGORITHMS: AlgorithmInfo[] = [
  {
    type: 'comfort',
    name: 'Comfort',
    description:
      'Keeps the room within a comfortable band while trimming runtime during low-demand periods.',
  },
  {
    type: 'target',
    name: 'Target',
    description:
      'Drives consumption toward a fixed energy/cost target by adjusting setpoints throughout the day.',
  },
]

export function algorithmName(type: AlgorithmType): string {
  if (type === 'none') return 'None'
  return ALGORITHMS.find((a) => a.type === type)?.name ?? type
}
