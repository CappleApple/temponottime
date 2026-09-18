# Simply Swords integration

Tempo Not Time has optional compatibility for Simply Swords' Iron's Spells mana-cost path. Simply Swords is not a required dependency; these hooks are only used when its classes are present.

The bridge was written against the `Architectury-1.21` source around commit `9da5070bbe49f4790a11826c4c4ab243ab05b2bd`.

## What changes

In `CASTING_RESERVE` mode, when Tempo is enabled and normal mana consumption is disabled:

- Simply Swords weapon mana costs are treated as **Casting Draw**.
- Affordability checks use the player's available Casting Reserve.
- A successful weapon ability does not subtract Iron's backing mana.
- The final effective item cooldown becomes a Tempo recovery instance.
- That recovery participates in Casting Reserve occupancy, cooldown load, Casting Recovery, normalization, persistence, and synchronization.

In `SPELL_COOLDOWNS` mode, mana spending and shared reserve gates are bypassed regardless of `disable_mana_consumption`. Simply Swords retains its native item cooldowns. Instant Mana only accelerates Iron's spell recharge queues.

When Tempo is disabled, or shared-reserve mode leaves mana consumption enabled, Simply Swords keeps its normal behavior.

## How the bridge works

Simply Swords uses `WeaponManaCost` to check and spend weapon mana, with its NeoForge helper reading and writing Iron's `MagicData`. Its public cooldown helper then applies the final item cooldown after the normal reduction rules.

Tempo hooks those points rather than maintaining a separate table of weapon values. This means a weapon's configured mana cost, base cooldown, and final cooldown remain the inputs. Tempo adds `recharge_normalization.flat_modifer` to the captured base, normalizes it, then preserves the effective/base ratio. These adjustments affect Tempo's reserve recovery debt; Simply Swords still owns its native item timer.

Charge-on-release abilities are supported as well. Tempo correlates the mana spend and the effective cooldown even when those two operations happen in the opposite order during the same server tick.

## Recovery entries

Simply Swords recovery entries use internal IDs below:

```text
temponottime:simply_swords/
```

In shared-reserve mode, they count toward reserve usage and cooldown load. Per-spell load groups them by weapon item ID; shared load combines them with spell debt. They are not exposed as fake Iron's spells. They therefore do not appear in spell charge maps or quick-cast HUD slots.

## Mixins

The optional bridge is split across a few small hooks:

- `SimplySwordsWeaponManaCostMixin` associates a weapon ability with the mana call that follows it.
- `SimplySwordsForgeHelperMixin` replaces Simply Swords' Iron-based affordability/spend helpers while Tempo owns mana usage.
- `SimplySwordsApiMixin` and `ItemCooldownsMixin` capture the effective item cooldown after Simply Swords has applied its own reduction rules.

The goal is to reuse Simply Swords' existing balance values and only substitute Tempo's resource/recovery model around them.
