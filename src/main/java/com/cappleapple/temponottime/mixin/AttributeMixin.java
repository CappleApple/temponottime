package com.cappleapple.temponottime.mixin;

import com.cappleapple.temponottime.config.ServerConfig;
import com.cappleapple.temponottime.network.ClientCooldownState;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Attribute.class)
public abstract class AttributeMixin {
    @Inject(method = "getDescriptionId", at = @At("RETURN"), cancellable = true)
    private void temponottime$localizeChargeCapacity(CallbackInfoReturnable<String> callback) {
        boolean cooldownMode = FMLEnvironment.dist == Dist.CLIENT
                ? ClientCooldownState.enabled() && ClientCooldownState.snapshot().spellCooldownsOnly()
                : ServerConfig.spellCooldownsOnly();
        if (cooldownMode && (Object) this == AttributeRegistry.MAX_MANA.value()) {
            callback.setReturnValue("attribute.temponottime.charge_capacity");
        }
    }
}
