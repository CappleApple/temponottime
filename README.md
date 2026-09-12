# Tempo Not Time

Tempo Not Time is an addon for Iron's Spells 'n Spellbooks that turns mana into a cooldown-focused casting system.

Instead of spending mana and waiting for a bar to refill, spells occupy part of your **Casting Reserve** while they recover. Iron's normal spell costs, cooldowns, gear bonuses, and addon content still matter; Tempo just gives those numbers a different job.

Built for Minecraft 1.21.1 / NeoForge.

## The basic idea

Iron's existing stats are reused rather than replaced with a second parallel set:

```text
Max Mana         -> Casting Reserve
Mana Regen       -> Casting Regeneration
Spell Mana Cost  -> Casting Draw
Spell Cooldown   -> Recharge Duration
```

If a spell has 60 Casting Draw and you have 300 Casting Reserve, casting it temporarily occupies 60 reserve until that cast recovers. Other spells can use whatever reserve is still free.

This makes spellcasting feel closer to managing a loadout of recovering abilities than managing a mana potion bar.

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

## Cooldown load

Recovering too many things at once can slow your overall recovery rate.

You can configure how many active cooldowns are free, how strongly extra cooldowns affect recovery, the minimum recovery speed, and whether multiple spent charges count separately.

Cooldown load never hard-locks casting. It only changes how quickly active cooldowns recover.

## Recharge normalization

Very short and very long cooldowns can optionally be pulled toward a configurable middle range. This is a soft curve rather than a hard clamp, so fast spells remain fast and slow spells remain slow without extreme outliers dominating a build.

Short and long cooldown normalization can be tuned separately.

## HUD

Tempo reuses Iron's existing spell and mana UI instead of replacing it with a separate HUD.

While the mod is active:

- the mana bar displays available Casting Reserve;
- mana cost text becomes Casting Draw;
- mana regeneration is presented as Casting Regeneration;
- spell slots show remaining charges when a spell has more than one;
- cooldown shading tracks the next returning charge; and
- scroll tooltips show the normalized recharge duration.

There is also an optional setting to hide unbound quick-cast slots without changing their actual quick-cast indices. The toggle is available from the inscription table.

## Configuration

Server gameplay settings are stored in:

```text
config/temponottime-server.toml
```

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

Scrolls and mob casting keep Iron's normal behavior.

Simply Swords is optional. When installed, its Iron's-compatible weapon mana costs can use Casting Reserve and its effective item cooldown can become Tempo recharge debt. See [the Simply Swords notes](docs/SIMPLY_SWORDS_INTEGRATION.md) for the exact behavior.

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

The public API exposes Casting Reserve, Casting Draw, spell charges, recovery state, and cast-reservation hooks for integrations that need to participate directly in the system.

## Building

```powershell
.\gradlew.bat test build
```

Built jars are written to `build/libs/`.

## License

Tempo Not Time is available under the MIT License.
