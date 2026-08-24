# Changelog

## 1.2.4 - 2026-08-23

### Added

- Iron-compatible maximum and current mana reporting now mirrors maximum and available Casting Reserve for HUD and addon integrations without becoming authoritative gameplay state.

### Changed

- Charge-count scaling is now controlled by a server-configurable Casting Reserve threshold equation. The default doubles each subsequent charge threshold without summing prior thresholds, while generated config comments include the original linear and cumulative-doubling equations as alternatives. Every cast retains the spell's normal flat Casting Draw.
- Spent charges now recover sequentially per spell by default; parallel recovery remains available through `charges.recovery_mode`.

## 1.2.3 - 2026-08-17

### Fixed

- Multi-blast spells now consume a Tempo charge and reserve Casting Draw only for the initial activation. Follow-up blasts use Iron's recast meter without creating additional Tempo recharge instances.
