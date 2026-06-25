-- DEMS PostgreSQL schema (read-only) for the SQL agent.
-- Generate ONLY PostgreSQL SELECT statements against these tables.

-- AC devices.
CREATE TABLE devices (
    id             VARCHAR PRIMARY KEY,   -- e.g. 'ac-001'
    name           VARCHAR,               -- e.g. 'Conference Room AC'
    location       VARCHAR,               -- e.g. 'Floor 1 — East Wing'
    model          VARCHAR,
    status         VARCHAR,               -- enum, UPPERCASE: 'ONLINE' | 'OFFLINE'
    current_tempc  DOUBLE PRECISION,      -- current room temperature in °C
    setpointc      DOUBLE PRECISION,      -- configured setpoint in °C
    powerw         INTEGER,               -- current power draw in watts (0 when offline)
    nominal_powerw INTEGER,               -- rated power draw in watts
    algorithm      VARCHAR                -- enum, UPPERCASE: 'COMFORT' | 'TARGET' | 'NONE'
);

-- Per-device usage and savings, one row per device per period.
CREATE TABLE saving_records (
    id               BIGINT PRIMARY KEY,
    device_id        VARCHAR REFERENCES devices(id),
    granularity      VARCHAR,             -- enum, UPPERCASE: 'DAILY' | 'MONTHLY'
    period           VARCHAR,             -- DAILY: 'YYYY-MM-DD'; MONTHLY: 'YYYY-MM' (sortable as text)
    cost_saved       DOUBLE PRECISION,    -- money saved in the period
    energy_saved_kwh DOUBLE PRECISION,    -- energy saved (kWh)
    usage_kwh        DOUBLE PRECISION,    -- energy consumed (kWh)
    usage_cost       DOUBLE PRECISION     -- cost of the energy consumed
);

-- Notes for query generation:
--   * Enum columns store UPPERCASE values (status, algorithm, granularity) — match exactly.
--   * To filter by device name use devices.name (join saving_records.device_id = devices.id).
--   * "Savings" = cost_saved / energy_saved_kwh; "usage" = usage_kwh / usage_cost.
--   * period is sortable TEXT; cast with period::date for date math. "Today" = CURRENT_DATE.
--     "last N days": granularity='DAILY' AND period::date >= CURRENT_DATE - INTERVAL '<N-1> days'.
--     For months use granularity='MONTHLY'.
--   * Savings only accrue when algorithm <> 'NONE' and the device is ONLINE; offline periods are 0.
