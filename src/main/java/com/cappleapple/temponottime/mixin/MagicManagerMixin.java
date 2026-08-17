package com.cappleapple.temponottime.mixin;

import com.cappleapple.temponottime.casting.CooldownManager;
import com.cappleapple.temponottime.config.ServerConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MagicManager.class, remap = false)
public abstract class MagicManagerMixin {
    @Inject(method = "regenPlayerMana", at = @At("HEAD"), cancellable = true)
    private void temponottime$disableManaRegeneration(ServerPlayer player, MagicData magicData,
                                                       CallbackInfoReturnable<Boolean> callback) {
        if (ServerConfig.enabled() && ServerConfig.DISABLE_MANA_CONSUMPTION.get()) {
            callback.setReturnValue(false);
        }
    }

    @Redirect(method = "lambda$tick$0", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/api/magic/MagicData;getMana()F"))
    private float temponottime$ignoreContinuousCastManaFloor(MagicData magicData) {
        return ServerConfig.enabled()
                && ServerConfig.DISABLE_MANA_CONSUMPTION.get()
                && CooldownManager.manages(magicData.getCastSource())
                ? Float.MAX_VALUE
                : magicData.getMana();
    }
}
