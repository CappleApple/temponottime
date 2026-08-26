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
@Mixin(targets = "net.sweenus.simplyswords.util.WeaponManaCost", remap = false)
public abstract class SimplySwordsWeaponManaCostMixin {
    @Inject(method = "spend", at = @At("HEAD"), require = 0)
    private static void temponottime$beginManaSpend(LivingEntity user, ItemStack stack, CallbackInfo callback) {
        SimplySwordsManaCompatibility.beginManaSpend(stack);
    }

    @Inject(method = "spend", at = @At("RETURN"), require = 0)
    private static void temponottime$endManaSpend(LivingEntity user, ItemStack stack, CallbackInfo callback) {
        SimplySwordsManaCompatibility.endManaSpend();
    }
}
