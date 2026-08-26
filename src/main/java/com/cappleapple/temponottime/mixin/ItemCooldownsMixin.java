package com.cappleapple.temponottime.mixin;

import com.cappleapple.temponottime.compat.SimplySwordsManaCompatibility;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemCooldowns.class)
public abstract class ItemCooldownsMixin {
    @Inject(method = "addCooldown", at = @At("HEAD"))
    private void temponottime$captureSimplySwordsCooldown(Item item, int durationTicks, CallbackInfo callback) {
        SimplySwordsManaCompatibility.captureEffectiveCooldown(item, durationTicks);
    }
}
