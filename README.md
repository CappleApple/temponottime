# Tempo Not Time

Tempo Not Time is an addon for Iron's Spells 'n Spellbooks that turns mana into a cooldown-focused casting system.

Choose independent spell charges and cooldowns, or keep the shared **Casting Reserve** system. Iron's normal spell costs, cooldowns, gear bonuses, and addon content still matter; Tempo just gives those numbers a different job.

Built for Minecraft 1.21.1 / NeoForge.

## Casting modes

Set `general.casting_mode` in `config/temponottime-server.toml` and restart the server or world:

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

- Max Mana determines charge counts through the existing charge formula when `general.convert_max_mana_to_casting_reserve` is enabled.
- Mana Regeneration speeds recharge when `general.convert_mana_regeneration_to_casting_regeneration` is enabled. Cooldown Reduction still affects each spell's duration.
- Mana spending, shared reserve limits, and cooldown load are bypassed even if their individual settings say otherwise. Casting one spell does not slow or block another.
- `charges.enabled`, charge limits, sequential/parallel recovery, recharge normalization, and applicable per-spell overrides still work. Disabling charges gives each spell one use followed by its cooldown.
- Mana potions and other applications of Iron's Instant Mana effect advance currently recovering spell charges. Each spell receives `restored mana / that cast's mana cost` of a full charge's recharge time. For example, restoring 20 mana advances a 40-mana, 10-second charge by 5 seconds.
- Sequential recovery spends the dose on the oldest recovering charge, carrying unused recovery into that spell's next charge. Parallel recovery applies the dose to every recovering charge. Excess recovery is discarded; active recasts and unfinished casts are unaffected until their cooldown starts.

Iron's original attribute identities and names remain unchanged in both modes. The existing conversion settings change their gameplay role without relabeling them. The mod's own `temponottime:casting_reserve` attribute retains its name and can also contribute to charge scaling.

In shared-reserve mode, a spell with 60 Casting Draw occupies 60 of your Casting Reserve until that cast recovers. With 300 reserve, other spells can use the remaining 240. Instant Mana restores available reserve in this mode.

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

In shared-reserve mode, recovering too many things at once can slow your overall recovery rate. `SPELL_COOLDOWNS` bypasses this penalty.

You can configure how many active cooldowns are free, how strongly extra cooldowns affect recovery, the minimum recovery speed, and whether multiple spent charges count separately.

Cooldown load never hard-locks casting. It only changes how quickly active cooldowns recover.

## Recharge normalization

Very short and very long cooldowns can optionally be pulled toward a configurable middle range. This is a soft curve rather than a hard clamp, so fast spells remain fast and slow spells remain slow without extreme outliers dominating a build.

Short and long cooldown normalization can be tuned separately.

## HUD

Tempo reuses Iron's existing spell and mana UI instead of replacing it with a separate HUD.

While the mod is active:

- the mana bar displays available Casting Reserve in mana-free `CASTING_RESERVE` mode and is hidden in `SPELL_COOLDOWNS` mode;
- Max Mana, Mana Regeneration, and Mana Cost keep their original labels;
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

The public API exposes Casting Reserve, Casting Draw, spell charges, recovery state, and cast-reservation hooks for integrations that need to participate directly in the system.

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

Tempo Not Time is available under the [MIT License](LICENSE).
