# Tempo Not Time

Tempo Not Time is an addon for Iron's Spells 'n Spellbooks that turns mana into a cooldown-focused casting system.

Choose independent spell charges and cooldowns, or keep the shared **Casting Reserve** system. Iron's normal spell costs, cooldowns, gear bonuses, and addon content still matter; Tempo just gives those numbers a different job.

Built for Minecraft 1.21.1 / NeoForge.

## Casting modes

Set `general.casting_mode` in `config/temponottime-server.toml` and save the file:

```toml
[general]
    enabled = true
    casting_mode = "SPELL_COOLDOWNS"
```

Edit the existing `[general]` section rather than adding a second one.

| Mode | Behavior |
| --- | --- |
| `CASTING_RESERVE` (default) | Recovering casts occupy a shared reserve. Existing settings continue to apply. |
| `SPELL_COOLDOWNS` | Each spell has its own charges and recharge queue. No shared reserve limit or mana spending; the mana bar is hidden. |

In `SPELL_COOLDOWNS` mode:

- Max Mana is displayed as **Charge Capacity**, and spell Mana Cost as **Charge Cost**. Charge Capacity determines charge counts through the existing charge formula when `general.convert_max_mana_to_casting_reserve` is enabled.
- When Charge Capacity is below a spell's Charge Cost, its cooldown is increased by `(cost - capacity) / cost`. For example, 50 capacity against 100 cost makes a 10-second cooldown take 15 seconds. Zero capacity doubles the cooldown; capacity at or above the cost adds no penalty. The comparison uses Iron's Max Mana and the cast's mana cost, before reserve-specific scaling or credit.
- Mana Regeneration speeds recharge when `general.convert_mana_regeneration_to_casting_regeneration` is enabled. Cooldown Reduction still affects each spell's duration.
- Mana spending and shared reserve limits are bypassed even if their individual settings say otherwise. Cooldown load is optional and defaults to independent load per spell; sharing it is opt-in.
- `charges.enabled`, charge limits, sequential/parallel recovery, recharge normalization, and applicable per-spell overrides still work. Disabling charges gives each spell one use followed by its cooldown.
- Mana potions and other applications of Iron's Instant Mana effect advance currently recovering spell charges. Each spell receives `restored mana / that cast's mana cost` of a full charge's recharge time. For example, restoring 20 mana advances a 40-mana, 10-second charge by 5 seconds.
- Sequential recovery spends the dose on the oldest recovering charge, carrying unused recovery into that spell's next charge. Parallel recovery applies the dose to every recovering charge. Excess recovery is discarded; active recasts and unfinished casts are unaffected until their cooldown starts.

Iron's attribute identities remain unchanged. `CASTING_RESERVE` keeps the original Max Mana and Mana Cost labels; Mana Regeneration keeps its original name in both modes. The mod's own `temponottime:casting_reserve` attribute retains its name and can also contribute to charge scaling.

In shared-reserve mode, a spell with 60 Casting Draw initially occupies 60 of your Casting Reserve. With 300 reserve, other spells can use the remaining 240. Instant Mana restores available reserve in this mode.

`casting_reserve.prorated_mana_regen` defaults to `true`. Every 10 ticks, occupied reserve decreases in proportion to each charge's cooldown progress. A 40-cost charge halfway through its cooldown occupies 20 reserve, so the bar gradually refills and that reserve can fund other casts. Pending casts and charges waiting to recover keep their full cost. Sequential recovery only refunds the charge currently progressing; parallel recovery refunds each progressing charge.

Set `prorated_mana_regen = false` in the existing `[casting_reserve]` section to keep the full cost occupied until recharge completes. This setting only changes the shared-reserve economy; it does not enable ordinary mana regeneration when mana-free casting is selected.

## Spell charges

A spell can have more than one charge when your Casting Reserve is high enough compared with its Casting Draw.

The default charge curve doubles the requirement for each extra charge. For a 20-draw spell:

```text
1 charge  -> 20 reserve
2 charges -> 40 reserve
3 charges -> 80 reserve
4 charges -> 160 reserve
```

Charges normally recover one at a time, although parallel recovery is available in the server config.

Iron's recast spells still use their normal follow-up behavior. The initial cast spends a Tempo charge; follow-up recasts do not create extra Tempo cooldown entries.

## Delay between charge casts

By default, a successful cast adds a short delay before the same spell can use another charge. Other spells remain available, and native follow-up recasts bypass the delay. Both casting modes use it.

```toml
[delay_between_charge_casts]
    enabled = true
    min_delay_seconds = 0.1
    max_delay_seconds = 10.0
    delay_modifier_percentage = 10.0
    flat_base_modifier = 0.5
```

Delay in seconds is `clamp(flat_base_modifier + actual_cast_seconds * delay_modifier_percentage / 100, min_delay_seconds, max_delay_seconds)`, rounded up to a game tick. Defaults give an instant spell a 0.5-second delay and a 2-second cast a 0.7-second delay. The flat modifier accepts positive or negative values. If the maximum is below the minimum, the minimum takes precedence.

The calculation uses the cast's actual duration after cast-time adjustments and gear modifiers. Its timer pauses while that spell is still casting, so channels retain a gap after finishing. It runs independently of charge recovery, Mana Regen, cooldown load, and Instant Mana. Available charges keep their count while the HUD shading shows the short delay. Creative players using the existing charge bypass also bypass this delay.

The delay follows cooldown save/logout/death policies. Disabling it clears active delay timers on the next player tick. `/temponottime clear` clears both recharge debt and inter-cast delays.

## Cooldown load

When `[cooldown_load].enabled = true`, spent charges slow recovery according to the existing free-cooldown allowance, penalty, and minimum speed. This works in both casting modes.

`shared_cooldown_load = false` is the default: each spell counts only its own spent and pending charges. With `shared_cooldown_load = true`, all spells contribute to the same penalty. `count_per_charge` only controls shared load: `true` counts charges, `false` counts distinct spells. Per-spell load always counts charges, including queued sequential charges. Per-spell overrides can still exclude a spell from load.

Cooldown load changes recovery speed; it never directly blocks casting.

## Timing normalization

Recharge and cast times have separate server config sections. Both support a signed `flat_modifer` in seconds, a curve center, separate short/long strengths, and a spread. The config key is spelled `flat_modifer`.

The flat adjustment applies to the base duration before normalization and native timing modifiers. It applies even when the section's `enabled` setting disables the curve. Adjusted durations have a one-tick minimum. Settings affect new casts and cooldowns; existing timers keep their recorded durations.

```toml
[recharge_normalization]
    enabled = true
    flat_modifer = 0.0
    normal_recharge_seconds = 10.0
    short_recharge_strength = 0.8
    long_recharge_strength = 0.5
    normalization_spread = 8.0

[cast_time_normalization]
    enabled = false
    flat_modifer = 0.0
    normal_cast_seconds = 1.0
    short_cast_strength = 0.8
    long_cast_strength = 0.5
    normalization_spread = 0.8
    affect_custom_cast_times = false
    affect_no_cast_time_spells = false
```

Edit the existing sections rather than duplicating them. For example, recharge `flat_modifer = -2.5` subtracts 2.5 seconds from base cooldowns before gear reductions and the normalization curve. Cast-time `flat_modifer = 0.5` adds half a second before cast-speed modifiers.

Normalization pulls short and long durations toward the center using a soft curve. Zero strength leaves that side unchanged. Cast-time adjustments change channel duration for continuous spells and preserve Iron's native cast-speed ratio.

By default, cast-time adjustments skip spells with custom base/effective cast-time methods and spells with no cast time. `affect_custom_cast_times = true` opts custom implementations into both the flat adjustment and curve. `affect_no_cast_time_spells = true` lets zero-duration spells receive a delay, using a zero base and no cast-speed multiplier. Custom zero-duration spells require both options. These options do nothing if the curve is disabled and the flat adjustment is zero.

## HUD

Tempo reuses Iron's existing spell and mana UI instead of replacing it with a separate HUD.

While the mod is active:

- the mana bar displays available Casting Reserve in mana-free `CASTING_RESERVE` mode and is hidden in `SPELL_COOLDOWNS` mode;
- Max Mana and Mana Cost display as Charge Capacity and Charge Cost in `SPELL_COOLDOWNS`, and retain their original labels in `CASTING_RESERVE`;
- spell slots show remaining charges when a spell has more than one;
- cooldown shading tracks the next returning charge; and
- scroll and active-spell tooltips show adjusted timing, including the current capacity shortfall penalty in `SPELL_COOLDOWNS`; and
- the inscription table shows adjusted base timings without player gear modifiers.

There is also an optional setting to hide unbound quick-cast slots without changing their actual quick-cast indices. The toggle is available from the inscription table.

## Configuration

Server gameplay settings are stored in:

```text
config/temponottime-server.toml
```

Saving this TOML applies changes live after NeoForge detects the edit. Connected clients refresh automatically, including idle players. No command, reconnect, world reload, or server restart is required. If the world has its own `serverconfig/temponottime-server.toml`, edit that file instead; it takes precedence over the global config. File watching must remain enabled in NeoForge's configuration.

Modes, mechanic toggles, charge limits/formulas, reserve capacity, regeneration, cooldown load, and sequential/parallel recovery update live. Turning off inter-cast delays clears active delay timers. Disabling charges retains the longest remaining recharge per spell. Entering `SPELL_COOLDOWNS` clears banked reserve credit.

Disabling Tempo releases its reserve, charges, and delay state while preserving Iron's native mana, cooldowns, and any ongoing cast. Re-enabling imports each remaining native cooldown as one recovering charge without charging reserve again. Timing and cast-cost adjustments apply to subsequent casts; casts already underway and recorded recharge/delay durations are not restarted or rescaled.

Client HUD settings are stored in:

```text
config/temponottime-client.toml
```

Per-spell overrides are written to:

```text
config/temponottime-spell-overrides.json
```

An override can change Casting Draw, maximum charges, cooldown scaling, or whether a spell participates in reserve/load behavior. Spells not listed continue using the normal calculations.

Reload the override file with:

```text
/temponottime reload
```

## Compatibility

Tempo uses Iron's real spell definitions, costs, cooldowns, attributes, equipment modifiers, and addon spells, which means most content does not need a dedicated compatibility patch.

Scrolls keep Iron's consumption and charge behavior but receive configured cast-time adjustments. Mob casting is unchanged.

Simply Swords is optional. When installed, its Iron's-compatible weapon mana costs can use Casting Reserve and its effective item cooldown can become Tempo recharge debt. In `SPELL_COOLDOWNS` mode, weapon abilities bypass mana/reserve costs but retain Simply Swords' native item cooldowns; Instant Mana only accelerates Iron's spell charges. See [the Simply Swords notes](docs/SIMPLY_SWORDS_INTEGRATION.md) for the exact behavior.

The Iron's integration points are documented in [docs/IRONS_INTEGRATION.md](docs/IRONS_INTEGRATION.md) for maintainers and addon authors.

## Commands

Commands require permission level 2:

```text
/temponottime info
/temponottime reserve
/temponottime cooldowns
/temponottime charges <namespace:spell>
/temponottime clear
/temponottime reload
```

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.200 or newer compatible 21.1 build
- Iron's Spells 'n Spellbooks 1.21.1-3.16.2 or compatible 1.21.1 3.x build
- Iron's normal dependencies
- Java 21

Install Tempo Not Time on both the server and clients.

## API

The public API exposes Casting Reserve, Casting Draw, spell charges, recovery state, and cast-reservation hooks for integrations that need to participate directly in the system. Server integrations should use `TempoNotTimeApi.castingRecoveryMultiplier(player, spell)` for the actual speed of a specific spell. The overload without a spell returns baseline recovery, including load only when shared load is enabled.

## Building

```powershell
.\gradlew.bat test build
```

Run the headless server integration test with:

```powershell
.\gradlew.bat runGameTestServer
```

Built jars are written to `build/libs/`.

## License

Tempo Not Time is licensed under [CC BY-NC-SA 4.0 with a Modpack/Server Exception](LICENSE). Modpacks and Minecraft servers, including monetized ones, may use it under the additional permission in the LICENSE.
