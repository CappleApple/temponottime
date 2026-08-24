package com.cappleapple.temponottime.mixin;

import com.cappleapple.temponottime.casting.ManaCompatibilityValues;
import com.cappleapple.temponottime.network.ClientCooldownState;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityClientMixin {
    @Inject(method = "getAttributeValue(Lnet/minecraft/core/Holder;)D", at = @At("HEAD"), cancellable = true)
    private void temponottime$reportMaximumCastingReserve(Holder<Attribute> attribute,
                                                          CallbackInfoReturnable<Double> callback) {
        if (!ClientCooldownState.manaDisabled()
                || attribute.value() != AttributeRegistry.MAX_MANA.value()
                || (Object) this != Minecraft.getInstance().player) {
            return;
        }

        var snapshot = ClientCooldownState.snapshot();
        callback.setReturnValue(ManaCompatibilityValues.snapshot(
                snapshot.maximumCastingReserve(), snapshot.usedCastingReserve(),
                snapshot.castingReserveCredit()).maximum());
    }
}
