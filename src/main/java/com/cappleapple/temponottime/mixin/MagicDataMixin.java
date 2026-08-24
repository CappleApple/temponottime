package com.cappleapple.temponottime.mixin;

import com.cappleapple.temponottime.casting.CooldownManager;
import com.cappleapple.temponottime.config.ServerConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MagicData.class, remap = false)
public abstract class MagicDataMixin {
    @Unique
    private static final ThreadLocal<Boolean> TEMPONOTTIME$REPORTING_MANA =
            ThreadLocal.withInitial(() -> false);

    @Shadow
    private ServerPlayer serverPlayer;

    @Inject(method = "getMana", at = @At("HEAD"), cancellable = true)
    private void temponottime$reportAvailableCastingReserve(CallbackInfoReturnable<Float> callback) {
        if (serverPlayer == null || TEMPONOTTIME$REPORTING_MANA.get()
                || !ServerConfig.enabled() || !ServerConfig.DISABLE_MANA_CONSUMPTION.get()) {
            return;
        }

        TEMPONOTTIME$REPORTING_MANA.set(true);
        try {
            callback.setReturnValue(CooldownManager.INSTANCE.manaCompatibility(serverPlayer).currentAsFloat());
        } finally {
            TEMPONOTTIME$REPORTING_MANA.set(false);
        }
    }
}
