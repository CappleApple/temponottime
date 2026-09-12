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
- Iron's mana/spell overlays and tooltip helpers for the HUD terminology Tempo replaces.

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

This disables ordinary mana regeneration when the pack has chosen mana-free casting and substitutes Casting Reserve for Iron's mid-cast mana-floor check.

### `MagicDataMixin`

Some integrations ask Iron's public `MagicData.getMana()` for an affordability value. While Tempo is active, that read can expose available Casting Reserve instead. This is a compatibility view only; Tempo's server-side reserve calculation remains the source used to authorize casts.

### `InstantManaEffectMixin`

Iron's Instant Mana effect becomes an immediate Casting Reserve recovery effect while Tempo is active.

### `AttributeMixin`

Only the display names for Iron's Max Mana and Mana Regen attributes are changed, allowing normal equipment tooltips to use Casting Reserve / Casting Regeneration terminology without replacing the attributes themselves.

### Client display hooks

The client hooks reuse Iron's own HUD layout rather than drawing a second spell interface:

- the mana overlay displays available Casting Reserve;
- the spell bar displays Tempo charge counts and recovery state;
- mana-cost tooltip text becomes Casting Draw; and
- cooldown tooltip values use the synchronized normalization settings.

## Cooldown calculation

Iron's `getSpellCooldown` supplies the spell's normal base cooldown. Tempo can normalize that base, then preserves the ratio introduced by Iron's effective cooldown calculation and cooldown events.

That is important for compatibility: equipment and addons that modify Iron's cooldown still modify the final Tempo recharge instead of being silently discarded.

## State ownership

Tempo stores its own recharge instances, charge/reserve occupancy, normalization state, and display snapshot. It does not rewrite Iron's saved mana capability, copy third-party modifiers into a second attribute system, or modify spell items in the registry.

When debugging an integration, the useful distinction is:

- **Iron's answers what the spell is and what it normally costs/does.**
- **Tempo answers whether the player can spend a charge and how that cast recovers.**
