# Iron's 1.21.1 integration

Tempo Not Time targets Iron's Spells 'n Spellbooks `1.21.1-3.16.2`. Integration was implemented against the tagged source rather than inferred class names.

## Inspected paths

- Attributes: `api.registry.AttributeRegistry` registers `MAX_MANA`, `MANA_REGEN`, `COOLDOWN_REDUCTION`, and the other synchronized spell attributes.
- Player magic data: `api.magic.MagicData` owns mana, casting state, `PlayerCooldowns`, recasts, serialization, and Iron's sync behavior.
- Validation/initiation: `api.spells.AbstractSpell.canBeCastedBy` checks mana and `PlayerCooldowns`; `attemptInitiateCast` posts `SpellPreCastEvent` before calling `MagicData.initiateCast`.
- Spend/cooldown: `AbstractSpell.castSpell` posts `SpellOnCastEvent`, subtracts its resulting mana cost, and asks `MagicManager` to add the cooldown.
- Effective duration: `AbstractSpell.getSpellCooldown` supplies the runtime base recharge. Tempo Not Time normalizes that base, then reapplies the ratio from `MagicManager.getEffectiveSpellCooldown` and the final cooldown event so Iron's native reduction and addon modifiers retain their normal effect.
- Continuous casting and regeneration: `MagicManager.tick` performs the mid-cast mana-floor check; `regenPlayerMana` mutates current mana.
- Selection and HUD: `api.magic.SpellSelectionManager`, `player.ClientMagicData`, `gui.overlays.ManaBarOverlay`, and Iron's spell-bar renderer provide selected spell and cooldown presentation.
- Tooltips: `util.TooltipsUtils.getSpellManaCostComponent` creates player-visible spell mana-cost text. Attribute modifier text uses each `Attribute` description ID.
- Instant Mana: `effect.InstantManaEffect.applyInstantenousEffect` calculates `25 * level + 5% * level` of effective Max Mana before applying Iron's normal mana clamp.
- Networking: Iron's payloads sync mana/casting/cooldown state. Tempo Not Time adds only its replacement state, uses that snapshot for Iron-facing HUD compatibility values, and does not accept a gameplay payload from clients.

## Public hooks used

- `SpellPreCastEvent` performs the authoritative charge/Casting Reserve decision while preserving unrelated Iron's validation.
- `SpellOnCastEvent` commits exactly one cooldown instance for the successful cast and sets event mana cost to zero.
- Iron's active `PlayerRecasts` entry distinguishes follow-up blasts from a new activation; those blasts bypass Tempo's charge/reserve validation and do not commit additional instances.
- `SpellCooldownAddedEvent.Post` supplies Iron's final effective cooldown and activates the matching waiting instance.
- NeoForge player tick/login/logout/clone/respawn/dimension events advance, persist, reconcile, and synchronize replacement state.
- `EntityAttributeModificationEvent` attaches `temponottime:casting_reserve` to players.
- NeoForge data attachments serialize versioned cooldown debt.
- NeoForge custom payload registration provides server-to-client display state.

## Mixins and why they are required

The public events do not cover every required decision or presentation point. All mixins target a small method and live in `com.cappleapple.temponottime.mixin`.

### `AbstractSpellMixin`

- Redirects only the current-mana read in `canBeCastedBy`. `SpellPreCastEvent` occurs after that check, so an event alone cannot allow a zero-mana player to reach reservation validation.
- Redirects only Iron's native `PlayerCooldowns.isOnCooldown` check for managed cast sources. The replacement system must allow additional stored charges while keeping all unrelated checks in the original method.
- Injects immediately after `MagicData.initiateCast` to create an atomic pending reservation. This closes the gap between the pre-cast validation event and later successful-cast event without trusting the client.
- Redirects only `MagicData.setMana` in `castSpell`. Setting `SpellOnCastEvent` mana cost to zero is cooperative, but another later event listener could otherwise restore a nonzero value; the redirect guarantees the advertised no-spend rule.

### `MagicManagerMixin`

- Cancels `regenPlayerMana` while mana gameplay is disabled. There is no public cancellable regeneration event, and repeatedly refilling/draining mana would be a more invasive hidden-resource hack.
- Redirects the one `MagicData.getMana` used by Iron's continuous-cast termination condition. Without this hook, a valid zero-mana continuous spell would stop early despite passing initial validation.

### `MagicDataMixin`

Reports available Casting Reserve from Iron's public server-side `MagicData.getMana` query while mana-free casting is active. A re-entry guard prevents compatibility listeners from recursively recalculating reserve. This is a read-only projection; Tempo's authoritative cast and recovery paths never consume the reported value.

### `InstantManaEffectMixin`

Runs after Iron's Instant Mana effect and grants the server-owned Casting Reserve credit by the same raw recovery amount. Credit can refill occupied reserve and can exceed the ordinary maximum by at most the latest single dose; later Casting Draw consumes that credit first.

### `AttributeMixin`

Replaces description IDs only for Iron's `MAX_MANA` and `MANA_REGEN` attribute objects while enabled. Modifier identity and math remain untouched, but generic equipment tooltips use Casting Reserve and Casting Regeneration terminology. No suitably scoped tooltip event exists for this generated attribute text.

### Client presentation mixins

- `ManaBarOverlayMixin` replaces Iron's mana values with available Casting Reserve while reusing Iron's texture and reading its display mode, anchor, bar offsets, numerical-text toggle, and text offsets directly from Iron's client config. Contextual visibility uses reserve fullness in place of mana fullness.
- `SpellBarOverlayMixin` renders authoritative available-charge counts on Iron's calculated spell icons and suppresses counts of zero or one.
- `TooltipsUtilsMixin` replaces Iron's spell mana-cost component with the localized Casting Draw component and redirects scroll cooldown formatting through the server-synchronized recharge-normalization curve.
- `ClientMagicDataMixin` supplies available Casting Reserve through Iron's public current-mana query and the next recovering charge's cooldown percentage to Iron's existing spell HUD, including between usable charges.
- `LivingEntityClientMixin` reports synchronized maximum Casting Reserve when a client HUD asks the local player for Iron's Max Mana attribute. Server attribute math remains untouched.

## State ownership

Iron's remains authoritative for spell definitions, cast time, spell-specific checks, selection, recasts, calculated mana cost, native cooldown reduction, and actual spell effects. Tempo Not Time owns only its versioned recharge instances, charge/reserve gates, normalized base recharge, converted Casting Recovery rate, persistence, and display snapshot.

The bridge never scans the item registry, rewrites Item instances, copies third-party attribute modifiers, or modifies Iron's saved mana capability format. Compatibility mana is derived from synchronized Tempo state and cannot authorize a cast.
