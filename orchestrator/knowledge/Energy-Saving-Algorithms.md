# Energy-Saving Algorithms

This document describes the energy-saving algorithms available in the DEMS
(Device Energy Management System) and how each one reduces an AC device's
energy consumption. Exactly one algorithm can be applied to a device at a time;
applying `none` leaves the device running on its built-in thermostat with no
optimization.

---

## Comfort Algorithm

The **Comfort** algorithm keeps a room within a comfortable temperature band
while trimming AC runtime during low-demand periods. Instead of holding a single
fixed setpoint, it lets the room temperature drift within a small tolerance
around the target so the compressor can rest whenever the room is already close
to the desired temperature.

### How it works
- It monitors the current room temperature against the configured setpoint.
- While the room stays inside the comfort band (typically the setpoint ±1 °C),
  it reduces compressor cycling, which is the main source of energy use.
- When the temperature drifts outside the band, normal cooling resumes
  immediately, so occupants never notice a meaningful change.

### Key parameters
- **Setpoint** — the target temperature the room is managed around.
- **Comfort band** — the allowed drift above/below the setpoint (default ±1 °C).
  A wider band saves more energy but allows larger temperature swings.

### When to use it
Comfort is best for **occupied spaces** such as conference rooms and open
offices, where occupant comfort matters but small temperature variations are
acceptable. It typically delivers **moderate energy savings** with negligible
comfort impact, and is the recommended default for most human-occupied rooms.

---

## Target Algorithm

The **Target** algorithm drives a device's consumption toward a fixed energy or
cost target over the day. It is a demand-oriented strategy: rather than
optimizing only for comfort, it actively shapes the load so total usage stays
within a budget.

### How it works
- A daily energy (kWh) or cost budget is configured for the device.
- The algorithm continuously adjusts setpoints through the day — pre-cooling when
  capacity is cheap or plentiful and easing off later — to keep cumulative
  consumption tracking toward the target.
- It prioritizes hitting the budget while respecting hard safety limits on
  temperature, so equipment and contents are never put at risk.

### Key parameters
- **Daily budget** — the energy (kWh) or cost ceiling the device should stay under.
- **Temperature safety limits** — hard min/max bounds the algorithm will never cross.

### When to use it
Target is best for **high-consumption or critical equipment** such as
server-room or large-capacity units, where predictable energy spend matters more
than tight comfort control. Of the available algorithms it generally produces the
**largest savings**, because it manages the whole daily load profile rather than
just reacting to the current temperature.

---

## Comfort vs Target — quick comparison

| Aspect            | Comfort                                  | Target                                      |
|-------------------|------------------------------------------|---------------------------------------------|
| Optimizes for     | Occupant comfort + modest savings        | Hitting an energy/cost budget               |
| Control style     | Temperature band around setpoint         | Whole-day load shaping toward a budget      |
| Typical savings   | Moderate                                 | Highest                                     |
| Best for          | Occupied rooms (offices, conference)     | High-consumption / critical units           |
| Comfort trade-off | Minimal                                  | Larger swings possible within safety limits |

## Notes
- Savings (energy saved and cost saved) only accrue while an algorithm is applied
  and the device is online. An offline device contributes no usage and no savings
  for the period it is offline.
