package com.cappleapple.temponottime.mixin;

import com.cappleapple.temponottime.casting.CooldownManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.effect.InstantManaEffect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = InstantManaEffect.class, remap = false)
public abstract class InstantManaEffectMixin {
    @Inject(method = "applyInstantenousEffect", at = @At("TAIL"))
    private void temponottime$rechargeCastingReserve(Entity source, Entity indirectSource,
                                                     LivingEntity livingEntity, int amplifier,
                                                     double health, CallbackInfo callback) {
        if (!(livingEntity instanceof ServerPlayer player)) return;
        int levels = Math.max(1, amplifier + 1);
        int maximumMana = (int) player.getAttributeValue(AttributeRegistry.MAX_MANA);
        int recoveredMana = (int) (levels * 25 + maximumMana * levels * 0.05F);
        CooldownManager.INSTANCE.rechargeCastingReserve(player, recoveredMana);
    }
}
