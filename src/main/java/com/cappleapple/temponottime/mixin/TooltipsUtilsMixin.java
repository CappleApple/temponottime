package com.cappleapple.temponottime.mixin;

import com.cappleapple.temponottime.casting.ChargeCooldownPenalty;
import com.cappleapple.temponottime.casting.RechargeNormalizer;
import com.cappleapple.temponottime.network.ClientCooldownState;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.util.TooltipsUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TooltipsUtils.class, remap = false)
public abstract class TooltipsUtilsMixin {
    @Redirect(method = "formatScrollTooltip", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/capabilities/magic/MagicManager;getEffectiveSpellCooldown(Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;Lnet/minecraft/world/entity/player/Player;Lio/redspace/ironsspellbooks/api/spells/CastSource;)I"))
    private static int temponottime$showNormalizedScrollCooldown(AbstractSpell spell, Player player,
                                                                CastSource castSource, ItemStack stack, Player tooltipPlayer) {
        var spellData = ISpellContainer.get(stack).getSpellAtIndex(0);
        int level = spell.getLevelFor(spellData.getLevel(), player);
        return temponottime$previewCooldown(spell, player, castSource, level);
    }

    @Redirect(method = "formatActiveSpellTooltip", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/capabilities/magic/MagicManager;getEffectiveSpellCooldown(Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;Lnet/minecraft/world/entity/player/Player;Lio/redspace/ironsspellbooks/api/spells/CastSource;)I"))
    private static int temponottime$showChargeCooldown(AbstractSpell spell, Player player, CastSource castSource,
                                                       ItemStack stack, SpellData spellData,
                                                       CastSource tooltipSource, LocalPlayer tooltipPlayer) {
        if (!ClientCooldownState.enabled() || !ClientCooldownState.snapshot().spellCooldownsOnly()) {
            return MagicManager.getEffectiveSpellCooldown(spell, player, castSource);
        }
        return temponottime$previewCooldown(spell, player, castSource, spell.getLevelFor(spellData.getLevel(), player));
    }

    @Unique
    private static int temponottime$previewCooldown(AbstractSpell spell, Player player, CastSource castSource, int level) {
        int effectiveTicks = MagicManager.getEffectiveSpellCooldown(spell, player, castSource);
        var snapshot = ClientCooldownState.snapshot();
        if (!snapshot.enabled()) return effectiveTicks;
        double normalized = RechargeNormalizer.normalizeEffectiveTicks(spell.getSpellCooldown(), effectiveTicks,
                snapshot.rechargeNormalizationEnabled(), snapshot.normalRechargeSeconds(),
                snapshot.shortRechargeStrength(), snapshot.longRechargeStrength(), snapshot.normalizationSpread());
        if (snapshot.spellCooldownsOnly()) {
            normalized *= ChargeCooldownPenalty.multiplier(player.getAttributeValue(AttributeRegistry.MAX_MANA),
                    spell.getManaCost(level));
        }
        return (int) Math.clamp(Math.round(normalized), 1L, Integer.MAX_VALUE);
    }

    @Inject(method = "getManaCostComponent", at = @At("RETURN"), cancellable = true)
    private static void temponottime$localizeChargeCost(CastType castType, int manaCost,
                                                        CallbackInfoReturnable<MutableComponent> callback) {
        if (!ClientCooldownState.enabled() || !ClientCooldownState.snapshot().spellCooldownsOnly()) return;
        boolean continuous = castType == CastType.CONTINUOUS;
        int displayedCost = continuous ? manaCost * (20 / MagicManager.CONTINUOUS_CAST_TICK_INTERVAL) : manaCost;
        callback.setReturnValue(Component.translatable(continuous
                ? "tooltip.temponottime.charge_cost_per_second" : "tooltip.temponottime.charge_cost", displayedCost));
    }
}
