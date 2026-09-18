package com.cappleapple.temponottime.mixin;

import com.cappleapple.temponottime.casting.CooldownManager;
import com.cappleapple.temponottime.config.ServerConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.effect.InstantManaEffect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = InstantManaEffect.class, remap = false)
public abstract class InstantManaEffectMixin {
    @Redirect(method = "applyInstantenousEffect", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/api/magic/MagicData;setMana(F)V"))
    private void temponottime$restoreMana(MagicData magicData, float targetMana,
                                         Entity source, Entity indirectSource, LivingEntity entity,
                                         int amplifier, double health) {
        // Capture Iron's actual addition before its mana setter clamps it to the maximum.
        double restoredMana = targetMana - magicData.getMana();
        if (!(entity instanceof ServerPlayer player) || !ServerConfig.enabled()) {
            magicData.setMana(targetMana);
            return;
        }
        if (!ServerConfig.spellCooldownsOnly()) magicData.setMana(targetMana);
        CooldownManager.INSTANCE.restoreInstantMana(player, restoredMana);
    }
}
