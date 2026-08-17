package com.cappleapple.temponottime.mixin;

import com.cappleapple.temponottime.config.ServerConfig;
import com.cappleapple.temponottime.network.ClientCooldownState;
import com.cappleapple.temponottime.registry.TempoRegistries;
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
    private void temponottime$translateConvertedAttributes(CallbackInfoReturnable<String> callback) {
        boolean client = FMLEnvironment.dist == Dist.CLIENT;
        boolean enabled = client ? ClientCooldownState.enabled() : ServerConfig.enabled();
        if (!enabled) return;
        boolean convertsMaxMana = client ? ClientCooldownState.snapshot().convertsMaxMana() : ServerConfig.CONVERT_MAX_MANA.get();
        boolean convertsManaRegen = client ? ClientCooldownState.snapshot().convertsManaRegen() : ServerConfig.CONVERT_MANA_REGEN.get();
        boolean reserveEnabled = client ? ClientCooldownState.snapshot().castingReserveEnabled() : ServerConfig.CAPACITY_ENABLED.get();
        boolean chargesEnabled = client ? ClientCooldownState.snapshot().chargesEnabled() : ServerConfig.CHARGES_ENABLED.get();
        String capacityDescription = !reserveEnabled && chargesEnabled
                ? "attribute.temponottime.charge_capacity" : "attribute.temponottime.casting_reserve";
        Object self = this;
        if (convertsMaxMana && self == AttributeRegistry.MAX_MANA.value()) {
            callback.setReturnValue(capacityDescription);
        } else if (self == TempoRegistries.CASTING_RESERVE.value()) {
            callback.setReturnValue(capacityDescription);
        } else if (convertsManaRegen && self == AttributeRegistry.MANA_REGEN.value()) {
            callback.setReturnValue("attribute.temponottime.casting_regeneration");
        }
    }
}
