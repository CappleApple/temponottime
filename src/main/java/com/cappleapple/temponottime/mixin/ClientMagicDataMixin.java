package com.cappleapple.temponottime.mixin;

import com.cappleapple.temponottime.casting.ManaCompatibilityValues;
import com.cappleapple.temponottime.network.ClientCooldownState;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClientMagicData.class, remap = false)
public abstract class ClientMagicDataMixin {
    @Inject(method = "getPlayerMana", at = @At("HEAD"), cancellable = true)
    private static void temponottime$reportAvailableCastingReserve(CallbackInfoReturnable<Integer> callback) {
        if (!ClientCooldownState.manaDisabled()) {
            return;
        }
        var snapshot = ClientCooldownState.snapshot();
        callback.setReturnValue(ManaCompatibilityValues.snapshot(
                snapshot.maximumCastingReserve(), snapshot.usedCastingReserve(),
                snapshot.castingReserveCredit()).currentAsInt());
    }

    @Inject(method = "getCooldownPercent", at = @At("HEAD"), cancellable = true)
    private static void temponottime$showChargeRecovery(AbstractSpell spell,
                                                         CallbackInfoReturnable<Float> callback) {
        if (!ClientCooldownState.enabled()) {
            return;
        }
        var state = ClientCooldownState.spells().get(spell.getSpellId());
        if (state != null) {
            callback.setReturnValue(state.activeUses() > 0 ? state.nextRemainingFraction() : 0.0F);
        }
    }
}
