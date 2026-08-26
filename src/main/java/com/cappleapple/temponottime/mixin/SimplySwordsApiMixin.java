package com.cappleapple.temponottime.mixin;

import com.cappleapple.temponottime.compat.SimplySwordsManaCompatibility;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.sweenus.simplyswords.api.SimplySwordsAPI", remap = false)
public abstract class SimplySwordsApiMixin {
    @Inject(method = "setWeaponCooldown", at = @At("HEAD"), require = 0)
    private static void temponottime$beginCooldownCapture(LivingEntity actor, ItemStack stack,
                                                          int baseCooldownTicks, CallbackInfo callback) {
        SimplySwordsManaCompatibility.beginCooldownCapture(actor, stack);
    }

    @Inject(method = "setWeaponCooldown", at = @At("RETURN"), require = 0)
    private static void temponottime$endCooldownCapture(LivingEntity actor, ItemStack stack,
                                                        int baseCooldownTicks, CallbackInfo callback) {
        SimplySwordsManaCompatibility.endCooldownCapture();
    }
}
