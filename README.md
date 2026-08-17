# Tempo Not Time

Tempo Not Time is a server-authoritative Minecraft 1.21.1 NeoForge addon for Iron's Spells 'n Spellbooks. It replaces mana spending with a modular cooldown economy while continuing to use Iron's calculated spell costs, cooldowns, equipment attributes, and addon content as the balance source.

- Technical mod ID: `temponottime`
- Java namespace: `com.cappleapple.temponottime`

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.200 or newer 21.1.x
- Iron's Spells 'n Spellbooks 1.21.1-3.16.2 or newer compatible 1.21.1 3.x release
- Iron's own required runtime dependencies

The mod must be installed on both the dedicated server and every connecting client.

## Resource mapping

```text
Iron's Max Mana       -> Casting Reserve
Iron's Mana Regen     -> Casting Regeneration
Iron's Spell Mana Cost-> Casting Draw
Iron's Cooldown       -> Recharge Duration
Cooldown Reduction    -> Cooldown Reduction
```

Iron's attributes remain intact internally for addon compatibility. Tempo Not Time reads their fully modified player values, gives them new gameplay meaning, bypasses current-mana gating, prevents mana spending/regeneration, and replaces misleading mana presentation while enabled.

When Casting Reserve is disabled but charges remain enabled, converted Max Mana and the native reserve attribute are displayed as Charge Capacity to describe their remaining role in charge calculation.

## Casting mechanics

The three advanced mechanics are independent. Any combination of A, B, and C can be enabled, and the master switch can restore normal Iron's behavior.

### A: Cooldown load scaling

Every recovering use is an active cooldown instance. Additional instances slow recovery with:

```text
load = max(0, activeCooldowns - freeCooldowns)
loadMultiplier = max(minimumRecoveryMultiplier,
                     1 / (1 + penaltyPerAdditionalCooldown * load))
```

`count_per_charge = false` changes the count to unique recovering spells. Load never blocks a cast; it only changes recovery speed.

### B: Spell charges

Maximum charges derive generically from the player's effective Casting Reserve and the spell's Casting Draw:

```text
maxCharges = clamp(floor(castingReserve / castingDraw), minimumCharges, maximumCharges)
```

A player with 300 Casting Reserve therefore receives five charges for a spell with 60 Casting Draw. Each spent charge owns an independent recharge instance. Parallel recovery is the default; sequential per-spell recovery is available.

When equipment or effects increase the maximum, new slots start available. When the maximum falls, existing recovery debt is retained and available charges clamp to zero until enough instances complete.

### C: Casting Reserve reservation

Each active recharge instance occupies its spell's former mana cost as Casting Draw:

```text
usedReserve + prospectiveDraw <= maximumReserve
```

Three recovering casts with 60 Casting Draw occupy 180 Casting Reserve. Reserve is released only when each corresponding instance completes. With B+C, each spent charge reserves independently.

Iron's Instant Mana effect also restores the same numerical amount of available Casting Reserve that it would have restored as mana. Repeated doses can refill occupied reserve, but reserve above the normal maximum is capped to one most-recent dose and is consumed by later casts before ordinary recharge debt is created.

### All mechanics disabled

With the master switch enabled and A/B/C disabled, spells consume no mana but retain ordinary one-cast-per-cooldown behavior. With `general.enabled = false`, Tempo Not Time clears its replacement state and restores Iron's normal validation, mana, cooldown, and HUD paths.

## Recharge normalization and Casting Recovery

Before charge recovery begins, Tempo Not Time passes each spell's runtime base recharge through a configurable asymmetric `tanh` curve. Short and long sides have independent strengths, the normal duration remains fixed, ordering is preserved, and extreme values approach the configured normal band smoothly rather than hitting a hard clamp. Iron's final effective cooldown ratio, including cooldown reduction and cooldown events, is then applied to the normalized base.

```text
scale = normalizationSpread / sideStrength
normalized = normal + scale * tanh((original - normal) / scale)
```

A negligible positive tie-breaking slope is added after the curve so even durations beyond floating-point `tanh` saturation remain ordered. A side strength of zero bypasses normalization for that side.

Fractional progress advances each server tick using Casting Recovery:

```text
castingRecovery = castingRegenerationMultiplier * loadMultiplier
```

The converted Casting Regeneration component treats Iron's Mana Regeneration baseline value of `1.0` as normal speed, applies the configured conversion multiplier to its deviation from baseline, bounds it to a positive safe value and the configured maximum, then applies the load floor. Durations and progress are finite, positive, and persistent.

## Configuration

Gameplay configuration is server-controlled in `config/temponottime-server.toml`. Client presentation is in `config/temponottime-client.toml`. NeoForge generates both with comments.

Important server settings:

- `general.enabled`: master replacement switch.
- `general.convert_max_mana_to_casting_reserve`: add effective Iron's Max Mana to the native Casting Reserve attribute.
- `general.convert_mana_regeneration_to_casting_regeneration`: apply effective Iron's Mana Regeneration to Casting Regeneration.
- `disable_mana_consumption`: bypass mana requirements and suppress spend/regen.
- `death_cooldown_behavior`: `PRESERVE` or `CLEAR`.
- `general.creative_bypasses_casting_reserve`, `general.creative_bypasses_charges`: independent creative exceptions.
- `casting_reserve.enabled`: enable reserve gating.
- `casting_reserve.max_mana_to_casting_reserve_multiplier`, `casting_reserve.mana_cost_to_casting_draw_multiplier`: global conversion balance.
- `casting_reserve.minimum_casting_reserve`, `casting_reserve.maximum_casting_reserve`: effective reserve bounds.
- `casting_reserve.allow_overreserve_single_cast`: permit one oversized draw only while no reserve is occupied.
- `casting_reserve.zero_mana_spell_casting_draw`: finite fallback for zero/negative/invalid draws.
- `charges.enabled`, `minimum_charges`, `maximum_charges`, `recovery_mode`.
- `cooldown_load.enabled`, `free_cooldowns`, `penalty_per_additional_cooldown`, `minimum_recovery_multiplier`, `count_per_charge`.
- `casting_recovery.casting_regeneration_to_recovery_multiplier`, `casting_recovery.maximum_total_recovery_multiplier`.
- `recharge_normalization.enabled`, `normal_recharge_seconds`, `short_recharge_strength`, `long_recharge_strength`, `normalization_spread`.
- `diagnostics.debug_logging`: state-transition logging; never per-tick logging.

Tempo Not Time's client config controls per-slot charge numbers. The Casting Reserve bar reads Iron's mana-bar display, anchor, bar offsets, numerical toggle, and text offsets directly from `irons_spellbooks-client.toml`.

The native synchronized attribute is:

```text
temponottime:casting_reserve
```

Its own modified value is added to the converted effective Max Mana contribution, then the configured Casting Reserve bounds are applied. No third-party modifiers are copied, deleted, or rewritten.

## Per-spell overrides

The server creates `config/temponottime-spell-overrides.json`. Unknown or omitted spells automatically use calculated defaults. Reload it with `/temponottime reload`.

```json
{
  "irons_spellbooks:fireball": {
    "casting_draw": 75.0,
    "max_charges": 3,
    "cooldown_multiplier": 1.0,
    "charges_allowed": true,
    "casting_reserve_applies": true,
    "load_scaling_applies": true
  }
}
```

Invalid files retain the last valid override set and log a clear error instead of corrupting active state.

## HUD and feedback

- Iron's mana bar becomes the Casting Reserve meter while server-side mana suppression is active. Its display mode, anchor, bar offsets, numerical-text toggle, and text offsets are read directly from Iron's client config. The bar and plain `available/max` display are full at zero occupied reserve and drain as reserve becomes occupied; Iron's contextual mode hides the full bar unless a magic item is held.
- Each spell-bar slot shows its available-charge number on the spell icon only when more than one charge is ready.
- Iron's normal cooldown shading shows the progress of the next returning charge, including while other charges remain usable.
- Scroll tooltips show the normalized effective recharge duration, including Iron's normal cooldown-reduction ratio, using the server's synchronized normalization settings.
- Mana-cost text becomes Casting Draw, while Max Mana and Mana Regeneration labels become Casting Reserve and Casting Regeneration. Recharge speed is called Casting Recovery.
- Casting Reserve, charge, and reservation failures use localized action-bar feedback throttled to once per second.

## Persistence and synchronization

Cooldown debt and temporary Instant Mana reserve credit are stored in a versioned NeoForge player data attachment. They survive logout, reconnect, and dimension travel. Death preserves them by default through player cloning. Only the server validates/reserves casts and advances fractional cooldown progress.

The client receives a compact play payload on login, respawn, dimension change, casts, completions, meaningful state changes, and every ten ticks only while progress is active. There is no client-to-server gameplay payload.

If charges are disabled while multiple charges recover, reconciliation keeps the longest remaining instance for each spell. Disabling Casting Reserve stops instances from blocking or reporting reserve without discarding their recovery. Disabling load immediately returns Casting Recovery to its non-load rate.

## Commands

All commands require permission level 2.

```text
/temponottime info
/temponottime reserve
/temponottime cooldowns
/temponottime charges <namespace:spell>
/temponottime clear
/temponottime reload
```

Player-query commands must be run by a player. `reload` is also usable from the dedicated-server console.

## Public API

`com.cappleapple.temponottime.api.TempoNotTimeApi` exposes Casting Reserve, charges, active instances, and Casting Recovery queries. NeoForge events under `api.event` allow other mods to adjust:

- Casting Reserve;
- per-cast Casting Draw;
- maximum charges;
- cast reservation (cancellable).

The server remains authoritative after event adjustments and defensively clamps invalid values.

## Compatibility notes

- Built-in and addon spells are derived from their actual `AbstractSpell` mana cost and Iron's effective cooldown; no per-spell compatibility table is used.
- Effective Max Mana and Mana Regeneration values automatically include equipment, curios, effects, upgrade modifiers, commands, and compatible third-party modifiers.
- Spellbook and sword player casts are managed. Scroll and mob casts retain Iron's own source-specific behavior.
- The current integration target is Iron's 1.21.1-3.16.2. Exact source hooks and rationale are documented in `docs/IRONS_INTEGRATION.md`.

## Building

From a PowerShell prompt with Java 21 available:

```powershell
./gradlew.bat test build
```

Release and sources JARs are written to `build/libs/`. Development launches are available through `runClient` and `runServer`.

## License

Tempo Not Time is available under the MIT License. See `LICENSE` for the complete terms.
