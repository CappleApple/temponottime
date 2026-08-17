package com.cappleapple.temponottime.mixin;

import com.cappleapple.temponottime.casting.RechargeNormalizer;
import com.cappleapple.temponottime.network.ClientCooldownState;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.util.TooltipsUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TooltipsUtils.class, remap = false)
public abstract class TooltipsUtilsMixin {
    @Redirect(method = "formatScrollTooltip", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/capabilities/magic/MagicManager;getEffectiveSpellCooldown(Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;Lnet/minecraft/world/entity/player/Player;Lio/redspace/ironsspellbooks/api/spells/CastSource;)I"))
    private static int temponottime$showNormalizedScrollCooldown(AbstractSpell spell, Player player,
                                                                CastSource castSource) {
        int effectiveTicks = MagicManager.getEffectiveSpellCooldown(spell, player, castSource);
        var snapshot = ClientCooldownState.snapshot();
        if (!snapshot.enabled()) return effectiveTicks;
        double normalized = RechargeNormalizer.normalizeEffectiveTicks(spell.getSpellCooldown(), effectiveTicks,
                snapshot.rechargeNormalizationEnabled(), snapshot.normalRechargeSeconds(),
                snapshot.shortRechargeStrength(), snapshot.longRechargeStrength(), snapshot.normalizationSpread());
        return (int) Math.max(1, Math.round(normalized));
    }

    @Inject(method = "getManaCostComponent", at = @At("RETURN"), cancellable = true)
    private static void temponottime$renameManaCost(CastType castType, int manaCost,
                                                   CallbackInfoReturnable<MutableComponent> callback) {
        if (!ClientCooldownState.manaDisabled()) return;
        callback.setReturnValue(Component.translatable("tooltip.temponottime.casting_draw", manaCost));
    }
}
