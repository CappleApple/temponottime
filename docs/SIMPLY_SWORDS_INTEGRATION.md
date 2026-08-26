# Simply Swords mana integration

Tempo Not Time's optional bridge targets Simply Swords' `Architectury-1.21` source at commit
`9da5070bbe49f4790a11826c4c4ab243ab05b2bd`. There is no compile-time or runtime dependency on
Simply Swords; pseudo-mixins apply only when its classes are present.

## Inspected source paths

- `net.sweenus.simplyswords.util.WeaponManaCost` reads the configured per-item mana cost, checks
  affordability before an ability, and spends after an accepted activation or release.
- `net.sweenus.simplyswords.neoforge.ForgeHelperMethods` implements the Iron's Spells path through
  `MagicData.getMana()` and `MagicData.setMana()` and sends Iron's mana sync packet after spending.
- `net.sweenus.simplyswords.api.SimplySwordsAPI.setWeaponCooldown` applies the final Iron's cooldown
  reduction and writes the effective player item cooldown.
- `UniqueWeaponActiveAbility` spends immediately for ordinary abilities. Dawnquiver, Ember Ire,
  and Stormbringer opt into charge-on-release spending. The bridge correlates the mana spend and
  effective cooldown in either order when they occur in the same server tick.

## Bridge behavior

- The bridge is active only while Tempo Not Time is enabled and `disable_mana_consumption` is true.
  Otherwise Simply Swords' original mana path is untouched.
- `ForgeHelperMethods.hasMana` is answered by Tempo's authoritative reserve calculation. The
  configured weapon mana cost passes through `mana_cost_to_casting_draw_multiplier`, and occupied
  reserve, Instant Mana credit, overreserve policy, capacity disablement, and creative bypass all
  retain their normal Tempo semantics.
- `ForgeHelperMethods.spendMana` is canceled after a successful ability and replaced by one external
  recharge instance. Iron's backing mana is not mutated or treated as authoritative state.
- The effective player item cooldown is captured at `ItemCooldowns.addCooldown` while
  `SimplySwordsAPI.setWeaponCooldown` is active. That duration enters Tempo's recharge normalization,
  Casting Recovery, persistence, reserve occupancy, and cooldown-load systems.
- External weapon instances use an internal `temponottime:simply_swords/` key. They contribute to
  reserve and load synchronization but are excluded from the spell-state map so they cannot appear
  as fake Iron's spells or alter quick-cast charge rendering.

## Optional mixins

- `SimplySwordsWeaponManaCostMixin` correlates the spending weapon with Simply Swords' platform mana
  call.
- `SimplySwordsForgeHelperMixin` replaces only Simply Swords' Iron affordability and spend helpers.
- `SimplySwordsApiMixin` and `ItemCooldownsMixin` capture the final effective item cooldown without
  reimplementing Simply Swords' cooldown-reduction rules.
