# Tempo Not Time

Tempo Not Time is an addon for Iron's Spells 'n Spellbooks on NeoForge 1.21.1. It takes Iron's mana system and changes the spend side into a cooldown economy. You still use Iron's calculated spell costs, cooldowns, equipment attributes, and addon content as the balance source, but instead of spending mana and waiting for it to regen, you're managing cooldown load, charges, and reserve reservation.

Technical mod ID: `temponottime`
Java namespace: `com.cappleapple.temponottime`

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.200 or newer 21.1.x
- Iron's Spells 'n Spellbooks 1.21.1-3.16.2 or newer compatible 1.21.1 3.x release
- Iron's own required runtime dependencies

Needs to be on the dedicated server and every connecting client.

## Resource mapping

```text
Iron's Max Mana       -> Casting Reserve
Iron's Mana Regen     -> Casting Regeneration
Iron's Spell Mana Cost-> Casting Draw
Iron's Cooldown       -> Recharge Duration
Cooldown Reduction    -> Cooldown Reduction
```

Iron's attributes stay intact internally so addons and other mods that read them don't break. Tempo Not Time reads the fully modified player values, gives them new gameplay meaning, bypasses the current-mana gating, prevents mana from actually spending or regenerating, and swaps out the mana HUD presentation while enabled.

While mana-free casting is active, Iron-facing compatibility queries report maximum Casting Reserve as maximum mana and available Casting Reserve as current mana. This keeps third-party HUDs and GUI customization mods accurate, including temporary Instant Mana overcharge, instead of leaving them at `0`. These values are a read-only projection: Tempo's cast validation, charges, Casting Draw, reserve occupancy, and recovery never read the projected mana number.

One edge case: when Casting Reserve is disabled but charges are still enabled, the converted Max Mana and native reserve attribute show up as Charge Capacity since they still factor into how many charges you get.

## Casting mechanics

The three mechanics (A, B, C) are independent. You can enable any subset, and the master switch drops everything back to normal Iron's behavior if you want.

### A: Cooldown load scaling

Every spell that's recovering counts as an active cooldown instance. More instances means slower recovery for all of them:

```text
load = max(0, activeCooldowns - freeCooldowns)
loadMultiplier = max(minimumRecoveryMultiplier,
                     1 / (1 + penaltyPerAdditionalCooldown * load))
```

`count_per_charge = false` makes the count unique recovering spells instead of total instances. Load never blocks a cast though, it just changes how fast things recover.

### B: Spell charges

By default, each numbered charge has a geometric Casting Reserve unlock threshold. The first charge requires one Casting Draw of reserve, and the threshold doubles for each subsequent charge:

```text
reserve threshold for charge N = castingDraw * 2^(N - 1)
```

This is not a cumulative price: a spell with 20 Casting Draw has charge thresholds of 20, 40, 80, and 160 Casting Reserve. A player at those thresholds gets one, two, three, and four charges respectively. Likewise, 300 Casting Reserve gives three charges for a spell with 60 Casting Draw because its thresholds are 60, 120, and 240; the fourth needs 480. This scaling affects only how many charges are unlocked. Firing any charge still occupies exactly one normal Casting Draw and creates one normal recharge instance. Sequential per-spell recovery is the default, while parallel recovery remains available through the server config.

The server config exposes this threshold as a restricted math expression. Its generated comments include the original linear equation, the cumulative-doubling alternative, and the default doubling-threshold equation so pack authors can switch between all three or write their own safe curve.

Spells using Iron's recast meter consume one Tempo charge and reserve one Casting Draw for the initial activation. Follow-up blasts consume Iron's displayed recast count and do not create additional Tempo recharge instances.

If equipment or effects bump the maximum up, new slots start available immediately. If the maximum drops, existing recovery debt sticks around and available charges clamp to zero until enough instances finish.

### C: Casting Reserve reservation

Each active recharge instance occupies its spell's Casting Draw against the player's total reserve:

```text
usedReserve + prospectiveDraw <= maximumReserve
```

Three recovering casts at 60 Casting Draw occupy 180 reserve. That reserve only frees up when each instance actually completes. With B+C together, each spent charge reserves its own slot independently.

Iron's Instant Mana effect also restores the same numerical amount of available Casting Reserve it would have restored as mana. Stacking doses can refill occupied reserve, but reserve above the normal max is capped to the most recent single dose and gets consumed by later casts before any new recharge debt is created.

### All mechanics disabled

Master switch on, A/B/C off, spells cost no mana but you get the standard one-cast-per-cooldown behavior back. `general.enabled = false` clears all replacement state and restores Iron's normal validation, mana, cooldown, and HUD paths entirely.

## Recharge normalization and Casting Recovery

Before any charge recovery starts, each spell's runtime base recharge goes through a configurable asymmetric `tanh` curve. Short and long sides have independent strengths, the normal duration stays fixed, ordering is preserved, and extreme values smooth into the configured normal band instead of hitting a hard clamp. Iron's final effective cooldown ratio (including cooldown reduction and cooldown events) applies on top of the normalized base.

```text
scale = normalizationSpread / sideStrength
normalized = normal + scale * tanh((original - normal) / scale)
```

A negligible positive tie-breaking slope gets added after the curve so durations beyond floating-point `tanh` saturation still stay ordered. Setting a side strength to zero skips normalization for that side entirely.

Fractional progress advances each server tick using Casting Recovery:

```text
castingRecovery = castingRegenerationMultiplier * loadMultiplier
```

The converted Casting Regeneration component treats Iron's Mana Regeneration baseline of `1.0` as normal speed, applies the configured conversion multiplier to the deviation from baseline, bounds it to a positive safe value and the configured max, then applies the load floor. Durations and progress are finite, positive, and persist.

## Configuration

Server gameplay config lives in `config/temponottime-server.toml`. Client presentation is in `config/temponottime-client.toml`. NeoForge generates both with comments.

Key server settings:

- `general.enabled`: master replacement switch.
- `general.convert_max_mana_to_casting_reserve`: add effective Iron's Max Mana to the native Casting Reserve attribute.
- `general.convert_mana_regeneration_to_casting_regeneration`: apply effective Iron's Mana Regeneration to Casting Regeneration.
- `disable_mana_consumption`: bypass mana requirements, suppress spend and regen.
- `death_cooldown_behavior`: `PRESERVE` or `CLEAR`.
- `general.creative_bypasses_casting_reserve`, `general.creative_bypasses_charges`: independent creative-mode exceptions.
- `casting_reserve.enabled`: enable reserve gating.
- `casting_reserve.max_mana_to_casting_reserve_multiplier`, `casting_reserve.mana_cost_to_casting_draw_multiplier`: global conversion balance.
- `casting_reserve.minimum_casting_reserve`, `casting_reserve.maximum_casting_reserve`: effective reserve bounds.
- `casting_reserve.allow_overreserve_single_cast`: allow one oversized draw only when no reserve is currently occupied.
- `casting_reserve.zero_mana_spell_casting_draw`: finite fallback for zero/negative/invalid draws.
- `charges.enabled`, `minimum_charges`, `maximum_charges`, `recovery_mode` (`SEQUENTIAL` by default, with `PARALLEL` available).
- `charges.casting_reserve_requirement_formula`: reserve threshold for numbered charge slots. Examples: original linear `casting_draw * charge`; cumulative doubling `casting_draw * (2 ^ charge - 1)`; default doubling threshold `casting_draw * 2 ^ (charge - 1)`.
- `cooldown_load.enabled`, `free_cooldowns`, `penalty_per_additional_cooldown`, `minimum_recovery_multiplier`, `count_per_charge`.
- `casting_recovery.casting_regeneration_to_recovery_multiplier`, `casting_recovery.maximum_total_recovery_multiplier`.
- `recharge_normalization.enabled`, `normal_recharge_seconds`, `short_recharge_strength`, `long_recharge_strength`, `normalization_spread`.
- `diagnostics.debug_logging`: state-transition logging only, never per-tick.

Client config handles per-slot charge numbers and the off-by-default `hud.only_show_bound_quick_cast_slots` spell-bar filter. The filter compacts the HUD to slots whose matching Iron's quick cast keybinding is bound. The Casting Reserve bar reads Iron's mana-bar display settings (anchor, bar offsets, numerical toggle, text offsets) directly from `irons_spellbooks-client.toml`.

The native synchronized attribute:

```text
temponottime:casting_reserve
```

Its own modified value gets added to the converted effective Max Mana contribution, then the configured Casting Reserve bounds apply. No third-party modifiers are copied, deleted, or rewritten.

## Per-spell overrides

Server creates `config/temponottime-spell-overrides.json`. Spells not in the file just use calculated defaults. Reload with `/temponottime reload`.

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

Invalid files keep the last valid override set and log a clear error rather than corrupting active state.

## HUD and feedback

- Iron's mana bar becomes the Casting Reserve meter while server-side mana suppression is active. Display mode, anchor, offsets, and text settings all read from Iron's client config. Bar and plain `available/max` display show full at zero occupied reserve and drain as reserve gets occupied. Iron's contextual mode hides the full bar unless a magic item is held.
- The optional quick-cast HUD filter hides unbound spell slots and compacts the remaining bound slots without changing their quick-cast indices.
- Spell-bar slots show the available charge count on the icon, but only when more than one charge is ready.
- Iron's normal cooldown shading tracks the next returning charge's progress, even while other charges are still usable.
- Scroll tooltips show the normalized effective recharge duration including Iron's cooldown-reduction ratio, using the server's synchronized normalization settings.
- Mana-cost text becomes Casting Draw. Max Mana and Mana Regeneration labels become Casting Reserve and Casting Regeneration. Recharge speed is called Casting Recovery.
- Casting Reserve, charge, and reservation failures use localized action-bar feedback, throttled to once per second.

## Persistence and synchronization

Cooldown debt and temporary Instant Mana reserve credit live in a versioned NeoForge player data attachment. They survive logout, reconnect, and dimension travel. Death preserves them by default through player cloning. Only the server validates casts, reserves, and advances fractional cooldown progress.

Client gets a compact play payload on login, respawn, dimension change, casts, completions, meaningful state changes, and every ten ticks while progress is active. No client-to-server gameplay payload.

If you disable charges while multiple are recovering, reconciliation keeps the longest remaining instance per spell. Disabling Casting Reserve stops instances from blocking or reporting reserve without discarding their recovery. Disabling load immediately returns Casting Recovery to its non-load rate.

## Commands

All require permission level 2.

```text
/temponottime info
/temponottime reserve
/temponottime cooldowns
/temponottime charges <namespace:spell>
/temponottime clear
/temponottime reload
```

Player-query commands have to be run by a player. `reload` works from the dedicated-server console too.

## Public API

`com.cappleapple.temponottime.api.TempoNotTimeApi` exposes queries for Casting Reserve, charges, active instances, and Casting Recovery. NeoForge events under `api.event` let other mods adjust:

- Casting Reserve
- per-cast Casting Draw
- maximum charges
- cast reservation (cancellable)

Server stays authoritative after event adjustments and clamps invalid values defensively.

## Compatibility notes

- Built-in and addon spells derive from their actual `AbstractSpell` mana cost and Iron's effective cooldown, no per-spell compatibility table.
- Effective Max Mana and Mana Regeneration automatically include equipment, curios, effects, upgrade modifiers, commands, and compatible third-party modifiers.
- Iron's server `MagicData` current-mana query and client `ClientMagicData`/local-player Max Mana queries expose the synchronized reserve projection for compatibility. Server integrations can query exact reserve values directly through `TempoNotTimeApi`.
- Spellbook and sword player casts are managed. Scroll and mob casts keep Iron's own source-specific behavior.
- Current integration target is Iron's 1.21.1-3.16.2. Exact source hooks and rationale are in `docs/IRONS_INTEGRATION.md`.

## Building

From a PowerShell prompt with Java 21:

```powershell
./gradlew.bat test build
```

Release and sources JARs land in `build/libs/`. `runClient` and `runServer` for dev launches.

## License

Tempo Not Time is published under the MIT License. See `LICENSE` for the full terms.
