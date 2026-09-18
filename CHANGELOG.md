# Changelog

## 1.3.2 - 2026-09-18

### Added

- Server TOML edits now refresh mechanics and connected clients live, including mode changes and enabling/disabling Tempo, without a command or restart.
- Added default-on delay between casts of the same spell: 0.5 seconds plus 10% of actual cast time, clamped to configurable 0.1-10 second bounds. The flat base accepts positive or negative values; other spells and native follow-up recasts remain available.
- Added signed `recharge_normalization.flat_modifer` to adjust base cooldowns in seconds before modifiers and normalization.
- Added a separate cast-time normalization section with a flat adjustment, curve center, short/long strengths, and spread. Custom cast-time spells and zero-duration spells each have an opt-in switch, both off by default.
- Added `cooldown_load.shared_cooldown_load`, defaulting to `false`: load applies separately to each spell's spent charges when enabled. Shared load remains available.

### Changed

- Updated the license to CC BY-NC-SA 4.0 with the Modpack/Server Exception and included the license notice in the mod JAR.
- Enabled cooldown load now applies in both casting modes. Per-spell load always counts charges; `count_per_charge` controls shared load only.
- Spell tooltips and inscription-table timing previews reflect the new timing settings.

## 1.3.1 - 2026-09-18

### Added

- Added default-on `casting_reserve.prorated_mana_regen`: shared reserve returns every 10 ticks in proportion to each charge's cooldown progress.

### Changed

- In `SPELL_COOLDOWNS` mode, Max Mana displays as Charge Capacity and spell Mana Cost displays as Charge Cost. Other modes keep the original names.
- Spells costing more than the player's Charge Capacity have longer cooldowns in `SPELL_COOLDOWNS` mode. The increase is the missing fraction of the spell's cost: 50 capacity against 100 cost adds 50%, up to a 100% increase at zero capacity.
- Spell cooldown previews in `SPELL_COOLDOWNS` include the current capacity shortfall penalty.

## 1.3 - 2026-09-17

### Added

- Added `SPELL_COOLDOWNS` mode: Max Mana scales each spell's charges and Mana Regeneration speeds recharge, without shared reserve limits, mana spending, cooldown-load penalties, or a mana bar.
- Mana potions and other Instant Mana applications advance active spell cooldowns in the new mode based on restored mana and each cast's mana cost, carrying excess recovery into the next spent charge during sequential recovery.

### Changed

- Max Mana, Mana Regeneration, and Mana Cost retain Iron's original names while enabled conversions still change their function.

## 1.2.6 - 2026-08-26

### Added

- The inscription table now has an icon toggle for showing only bound quick cast slots, with colored On and grayscale Off states.
- Simply Swords weapon mana costs now use Casting Reserve affordability, Casting Draw, recharge duration, Instant Mana credit, and cooldown load while Tempo's mana replacement is active.

## 1.2.5 - 2026-08-24

### Added

- An off-by-default client option can limit the spell-bar HUD to slots with a bound Iron's quick cast keybinding.

## 1.2.4 - 2026-08-23

### Added

- Iron-compatible maximum and current mana reporting now mirrors maximum and available Casting Reserve for HUD and addon integrations without becoming authoritative gameplay state.

### Changed

- Charge-count scaling is now controlled by a server-configurable Casting Reserve threshold equation. The default doubles each subsequent charge threshold without summing prior thresholds, while generated config comments include the original linear and cumulative-doubling equations as alternatives. Every cast retains the spell's normal flat Casting Draw.
- Spent charges now recover sequentially per spell by default; parallel recovery remains available through `charges.recovery_mode`.

## 1.2.3 - 2026-08-17

### Fixed

- Multi-blast spells now consume a Tempo charge and reserve Casting Draw only for the initial activation. Follow-up blasts use Iron's recast meter without creating additional Tempo recharge instances.
