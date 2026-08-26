# Tempo Not Time

Tempo Not Time is an addon for Iron's Spells 'n Spellbooks on NeoForge 1.21.1 that replaces mana spending with a cooldown-focused casting system.

Iron's existing spell costs, cooldowns, equipment bonuses, and addon content still determine balance, but instead of spending mana and waiting for it to regenerate, you manage **Casting Reserve**, spell charges, and recharge time.

## Requirements

* Minecraft 1.21.1
* NeoForge 21.1.200 or newer compatible 21.1.x version
* Iron's Spells 'n Spellbooks 1.21.1-3.16.2 or newer compatible 1.21.1 3.x version
* Iron's required dependencies
* Simply Swords is optional; its Iron's Spells mana-cost system is integrated when present

Tempo Not Time must be installed on both the server and connecting clients.

## Resource Mapping

```text
Iron's Max Mana             -> Casting Reserve
Iron's Mana Regen           -> Casting Regeneration
Iron's Spell Mana Cost      -> Casting Draw
Iron's Cooldown             -> Recharge Duration
Cooldown Reduction          -> Cooldown Reduction
Simply Swords Mana Cost     -> Casting Draw
Simply Swords Item Cooldown -> Recharge Duration
```

Iron's original attributes remain compatible with equipment, addons, effects, and other mods.

When Tempo Not Time is enabled, mana is no longer spent normally. Instead, those values are used by the new casting system.

## Casting Mechanics

Tempo Not Time has three main mechanics. Each can be enabled or disabled independently.

### Cooldown Load

Having several spells recovering at once can slow down overall recharge speed.

You can configure:

* How many cooldowns are free before penalties begin
* How strongly additional cooldowns slow recovery
* The minimum recovery speed
* Whether multiple spent charges count separately

Cooldown load never prevents casting. It only affects recharge speed.

### Spell Charges

Spells can gain multiple charges based on the player's Casting Reserve compared to the spell's Casting Draw.

By default, charge requirements double with each additional charge.

For a spell with **20 Casting Draw**:

```text
1 charge  -> 20 Casting Reserve
2 charges -> 40 Casting Reserve
3 charges -> 80 Casting Reserve
4 charges -> 160 Casting Reserve
```

Each cast still occupies only one normal Casting Draw while that charge recovers.

Charges recover sequentially by default, with parallel recovery available through configuration.

Iron's recast spells consume one Tempo charge when initially cast. Their built-in follow-up casts continue using Iron's normal recast system.

### Casting Reserve

Every recovering cast temporarily occupies part of your Casting Reserve.

For example:

```text
Maximum Casting Reserve: 300

Recovering spell: 60 Casting Draw
Recovering spell: 60 Casting Draw
Recovering spell: 60 Casting Draw

Available Casting Reserve: 120
```

That reserve becomes available again as each cast finishes recovering.

This means powerful spells naturally take up more of your available casting capacity.

Iron's Instant Mana effect instead restores available Casting Reserve while Tempo Not Time is active.

## Casting Recovery

**Casting Regeneration** controls how quickly spells recover.

Iron's Mana Regeneration bonuses are converted into Casting Regeneration, so existing equipment and effects continue contributing to your build.

Cooldown Load can then further modify your final **Casting Recovery** speed.

## Recharge Normalization

Tempo Not Time can normalize unusually short or unusually long spell cooldowns.

Instead of forcing every spell into a hard minimum or maximum, recharge durations are smoothly pulled toward a configurable normal range.

This keeps fast spells fast and slow spells slow while reducing extreme cooldown differences.

Short and long cooldowns can be normalized independently.

## Configuration

Server gameplay settings are stored in:

```text
config/temponottime-server.toml
```

Client HUD settings are stored in:

```text
config/temponottime-client.toml
```

The off-by-default `hud.only_show_bound_quick_cast_slots` setting can also be toggled from the
inscription table. Its icon is colored while On and grayscale while Off.

Major server options include:

* Enable or disable Tempo Not Time
* Convert Max Mana into Casting Reserve
* Convert Mana Regeneration into Casting Regeneration
* Enable or disable mana consumption
* Preserve or clear cooldowns on death
* Creative-mode bypasses
* Casting Reserve limits
* Casting Draw scaling
* Spell charges
* Sequential or parallel charge recovery
* Charge scaling formula
* Cooldown Load
* Casting Recovery scaling
* Recharge normalization
* Debug logging

The charge requirement formula is configurable, allowing pack authors to use linear, doubling, cumulative, or custom scaling.

## Per-Spell Overrides

Tempo Not Time creates:

```text
config/temponottime-spell-overrides.json
```

This lets individual spells override the normal calculated behavior.

Example:

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

Spells not listed continue using their automatically calculated values.

Reload overrides with:

```text
/temponottime reload
```

## HUD

While Tempo Not Time is active:

* Iron's mana bar becomes the **Casting Reserve** bar
* Mana Cost becomes **Casting Draw**
* Max Mana becomes **Casting Reserve**
* Mana Regeneration becomes **Casting Regeneration**
* Recharge speed is called **Casting Recovery**
* Spell slots display remaining charges when more than one is available
* Normal cooldown shading shows progress toward the next returning charge
* Scroll tooltips display the normalized recharge duration
* Casting failures provide action-bar feedback

An optional HUD setting can hide unbound quick-cast spell slots and compact the remaining slots
without changing their quick-cast indices. The inscription table exposes it as a persisted icon
toggle with `Only show bound quick cast slots: On/Off` hover text.

The Casting Reserve bar continues using Iron's existing HUD positioning and display settings.

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

`reload` can also be used from the dedicated-server console.

## Compatibility

Tempo Not Time uses Iron's actual spell costs, cooldowns, attributes, equipment modifiers, effects, and addon spells instead of maintaining separate compatibility values.

Spellbook and sword casts use Tempo's casting system.

Scrolls and mob casting retain Iron's normal behavior.

Current integration target:

```text
Iron's Spells 'n Spellbooks 1.21.1-3.16.2
```

Simply Swords' configured weapon mana costs use the same Casting Draw conversion and authoritative
reserve gate. Successful weapon abilities create recharge debt from their final effective item
cooldown, including charge-on-release abilities, without making Simply Swords a required dependency.
The exact optional hooks are documented in `docs/SIMPLY_SWORDS_INTEGRATION.md`.

## Public API

Tempo Not Time provides an API for mods that need access to:

* Casting Reserve
* Casting Draw
* Spell charges
* Active recharge instances
* Casting Recovery
* Cast reservation

## Building

Requires Java 21.

```powershell
./gradlew.bat test build
```

Built JARs are placed in:

```text
build/libs/
```

## License

Tempo Not Time is published under the MIT License. See `LICENSE` for details.
