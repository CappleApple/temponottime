# Iron's Spells 'n Spellbooks integration

This file documents the parts of Iron's Spells 'n Spellbooks that Tempo Not Time depends on. It is mainly here for maintainers and addon authors who need to understand where Tempo joins Iron's normal casting flow.

Current integration target: **Iron's Spells 'n Spellbooks 1.21.1-3.16.2**.

## What Iron's still owns

Tempo does not replace spell definitions or the actual spell engine. Iron's remains responsible for:

- spell definitions, cast time, recasts, and spell-specific validation;
- the original mana-cost calculation;
- cooldown reduction and other spell attributes;
- spell selection and quick-cast slots;
- equipment/addon attribute modifiers; and
- the actual spell effects.

Tempo takes the resulting cost/cooldown information and applies its own reserve, charge, and recovery rules around it.

## Main integration points

The important Iron's classes are:

- `AttributeRegistry` for Max Mana, Mana Regen, Cooldown Reduction, and related spell attributes;
- `MagicData` for player magic state and casting/recast information;
- `AbstractSpell` for cast validation, mana cost, and base cooldowns;
- `MagicManager` for effective cooldowns and mana regeneration;
- `SpellSelectionManager` / `ClientMagicData` for client spell selection and display state; and
- Iron's mana/spell overlays and tooltip helpers for charge, reserve, and cooldown presentation.

Tempo uses Iron's public spell events where they are sufficient and narrowly scoped mixins where there is no event at the required point.

## Public events and NeoForge hooks

`SpellPreCastEvent` is where Tempo checks charges and available Casting Reserve after Iron's own spell validation.

`SpellOnCastEvent` commits the successful cast and prevents the normal mana payment while mana-free casting is enabled.

`SpellCooldownAddedEvent.Post` provides the final effective cooldown after Iron's own modifiers. Tempo uses that duration for the matching recovery instance.

Iron's active recast entry is used to distinguish a follow-up recast from a new activation, so a multi-stage spell does not spend additional Tempo charges for every follow-up blast.

NeoForge player lifecycle events handle persistence and synchronization, while a data attachment stores Tempo's recharge state.

## Why mixins are still needed

A few decisions happen before or after Iron's public events, so event handlers alone cannot implement the mechanic cleanly.

### `AbstractSpellMixin`

This hook handles the narrow parts of spell validation that need to see Tempo state:

- allowing a valid zero-mana player to reach Tempo's reserve check;
- allowing another stored charge even when Iron's normal cooldown object is active;
- creating the pending reservation immediately after Iron begins a cast; and
- preventing Iron's normal mana subtraction after a successful Tempo-managed cast.

The rest of `AbstractSpell` continues through Iron's normal code.

### `MagicManagerMixin`

This disables ordinary mana regeneration while mana-free casting is enabled and bypasses Iron's mid-cast mana-floor check for managed cast sources.

### `MagicDataMixin`

Some integrations ask Iron's public `MagicData.getMana()` for an affordability value. During mana-free casting, that read exposes available Casting Reserve in shared-reserve mode and full native Max Mana in `SPELL_COOLDOWNS` mode. This is a compatibility view only; Tempo's server-side charge and reserve gates authorize casts.

### `InstantManaEffectMixin`

Redirects the effect's `MagicData.setMana` call and measures Iron's actual mana addition before clamping. In shared-reserve mode it preserves the native write and grants Casting Reserve credit. Credit can refill occupied reserve and can exceed the ordinary maximum by at most the latest single dose; later Casting Draw consumes that credit first.

In `SPELL_COOLDOWNS` mode it skips the native write and advances each spell's active recharge queue. Progress is proportional to the restored mana divided by the raw mana cost captured at cast commit, independent of reserve multipliers or potion credit. Zero/invalid costs use `casting_reserve.zero_mana_spell_casting_draw` as a positive fallback. Sequential recovery carries unused mana into the next active charge of the same spell; parallel recovery applies the full dose to every active charge. Waiting recasts, unfinished casts, and Simply Swords item timers are excluded. A dose never creates future credit.

Both potion applications and `applyEffectTick` reach this hook. Disabling Tempo preserves the original write without adding credit or advancing cooldowns.

### `AttributeMixin`

In `SPELL_COOLDOWNS` mode, `Attribute.getDescriptionId` returns `attribute.temponottime.charge_capacity` for Iron's Max Mana attribute. This covers normal attribute localization, including equipment modifier tooltips. The server uses its configured mode and the client uses the synchronized mode. Other attributes and other modes retain their original description IDs.

### Client display hooks

The client hooks reuse Iron's own HUD layout rather than drawing a second spell interface:

- the mana overlay displays available Casting Reserve in mana-free shared-reserve mode and is hidden in `SPELL_COOLDOWNS` mode;
- the spell bar displays Tempo charge counts and recovery state;
- `TooltipsUtilsMixin` labels spell mana costs as Charge Cost in `SPELL_COOLDOWNS`, preserving Iron's continuous-cast per-second conversion;
- other modes retain the original mana-cost components; and
- cooldown tooltip values use the synchronized normalization settings, with the current capacity shortfall penalty included for scroll and active-spell previews in `SPELL_COOLDOWNS`. Previews use the spell's displayed mana cost; server-only cast-event cost changes can differ.

Third-party HUDs receive full native Max Mana in `SPELL_COOLDOWNS` mode but may still choose to draw their own mana bar. Only the shared-reserve mode projects Casting Reserve into client Max Mana queries.

## Cooldown calculation

Iron's `getSpellCooldown` supplies the spell's normal base cooldown. Tempo can normalize that base, then preserves the ratio introduced by Iron's effective cooldown calculation and cooldown events.

That is important for compatibility: equipment and addons that modify Iron's cooldown still modify the final Tempo recharge instead of being silently discarded.

In `SPELL_COOLDOWNS`, a successful cast captures the multiplier `1 + clamp((manaCost - maxMana) / manaCost, 0, 1)`. Zero-cost spells have no penalty. These are Iron's effective Max Mana and the successful cast event's mana cost, independent of reserve-cost multipliers and reserve credit. The penalty is applied after normalization and per-spell cooldown scaling, and retained when Iron's final cooldown event activates the charge. Equipment changes after the cast do not change that charge's captured multiplier. Existing saved charges without a multiplier keep their previous durations.

## Prorated reserve recovery

With `casting_reserve.prorated_mana_regen = true` (the default), player ticks sample each charge's actual cooldown progress every 10 ticks in `CASTING_RESERVE` mode. Occupied reserve becomes `reservedCost * (1 - sampledProgressFraction)`. Pending and waiting casts keep their full reservation; queued sequential charges release nothing until their own progress advances.

This changes authoritative affordability and the existing mana/HUD compatibility snapshot together. It does not add potion credit or write Iron's backing mana. A cast paid partly by potion credit only releases its remaining reserved cost, and removing a completed charge releases only the occupancy left over. Disabling the option restores the full-cost-until-completion rule. If native mana spending is explicitly enabled, its backing mana and ordinary regeneration remain separate from this reserve calculation.

## State ownership

Tempo stores its own recharge instances, charge/reserve occupancy, normalization state, and display snapshot. It does not rewrite Iron's saved mana capability, copy third-party modifiers into a second attribute system, or modify spell items in the registry.

When debugging an integration, the useful distinction is:

- **Iron's answers what the spell is and what it normally costs/does.**
- **Tempo answers whether the player can spend a charge and how that cast recovers.**

## Mode and saved-state behavior

`general.casting_mode` defaults to `CASTING_RESERVE`. `SPELL_COOLDOWNS` forces mana-free casting and bypasses reserve and load gates while honoring the charge and attribute-conversion toggles. Scrolls and mobs remain outside Tempo's managed spellbook/sword cast sources.

Cooldown data version 4 additionally stores the captured `cooldown_penalty_multiplier` and sampled `reserve_recovered_fraction`. Older saves default to no penalty and sample existing progress on the next ten-tick interval.

As in version 3, cooldown data stores `recovery_mana_cost` separately from occupied reserve. Existing version-2 instances fall back to their saved Casting Draw, with a minimum cost of 1; their original raw mana cost cannot be recovered. New casts preserve raw mana cost even if reserve credit covers their entire cost. Charge progress continues to use the existing save, logout, dimension-change, and death policies.

The server synchronizes the effective mode and mana/reserve flags. Network protocol `1.3` requires matching clients and server. Third-party HUDs may still choose to draw their own full mana bar; Tempo directly hides Iron's native overlay.
