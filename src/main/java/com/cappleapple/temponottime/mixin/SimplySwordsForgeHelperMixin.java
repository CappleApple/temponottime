package com.cappleapple.temponottime.mixin;

import com.cappleapple.temponottime.compat.SimplySwordsManaCompatibility;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.sweenus.simplyswords.neoforge.ForgeHelperMethods", remap = false)
public abstract class SimplySwordsForgeHelperMixin {
    @Inject(method = "hasMana", at = @At("HEAD"), cancellable = true, require = 0)
    private static void temponottime$checkCastingReserve(LivingEntity entity, float amount,
                                                         CallbackInfoReturnable<Boolean> callback) {
        if (entity instanceof ServerPlayer player && SimplySwordsManaCompatibility.handles(entity)) {
            callback.setReturnValue(SimplySwordsManaCompatibility.canAfford(player, amount));
        }
    }

    @Inject(method = "spendMana", at = @At("HEAD"), cancellable = true, require = 0)
    private static void temponottime$createRechargeDebt(LivingEntity entity, float amount, CallbackInfo callback) {
        if (entity instanceof ServerPlayer player && SimplySwordsManaCompatibility.handles(entity)) {
            SimplySwordsManaCompatibility.spend(player, amount);
            callback.cancel();
        }
    }
}
