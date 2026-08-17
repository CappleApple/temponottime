package com.cappleapple.temponottime.mixin;

import com.cappleapple.temponottime.casting.CooldownManager;
import com.cappleapple.temponottime.config.ServerConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.PlayerCooldowns;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractSpell.class, remap = false)
public abstract class AbstractSpellMixin {
    @Redirect(method = "canBeCastedBy", at = @At(value = "INVOKE", target = "Lio/redspace/ironsspellbooks/api/magic/MagicData;getMana()F"))
    private float temponottime$ignoreManaRequirement(MagicData magicData, int spellLevel, CastSource castSource,
                                                    MagicData ignoredMagicData, Player player) {
        return ServerConfig.enabled() && ServerConfig.DISABLE_MANA_CONSUMPTION.get() && castSource.consumesMana()
                ? Float.MAX_VALUE
                : magicData.getMana();
    }

    @Redirect(method = "canBeCastedBy", at = @At(value = "INVOKE", target = "Lio/redspace/ironsspellbooks/capabilities/magic/PlayerCooldowns;isOnCooldown(Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;)Z"))
    private boolean temponottime$useTempoCooldownGate(PlayerCooldowns cooldowns, AbstractSpell spell, int spellLevel,
                                                     CastSource castSource, MagicData magicData, Player player) {
        return ServerConfig.enabled() && CooldownManager.manages(castSource) ? false : cooldowns.isOnCooldown(spell);
    }

    @Inject(method = "attemptInitiateCast", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/api/magic/MagicData;initiateCast(Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;IILio/redspace/ironsspellbooks/api/spells/CastSource;Ljava/lang/String;)V",
            shift = At.Shift.AFTER))
    private void temponottime$recordSuccessfulInitiation(ItemStack stack, int spellLevel, Level level, Player player,
                                                        CastSource castSource, boolean triggerCooldown, String castingEquipmentSlot,
                                                        CallbackInfoReturnable<Boolean> callback) {
        CooldownManager.INSTANCE.beginSuccessfulCast((ServerPlayer) player, (AbstractSpell) (Object) this, spellLevel, castSource);
    }

    @Redirect(method = "castSpell", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/api/magic/MagicData;setMana(F)V"))
    private void temponottime$skipManaSpend(MagicData magicData, float mana,
                                            Level level, int spellLevel, ServerPlayer player,
                                            CastSource castSource, boolean triggerCooldown) {
        if (!(ServerConfig.enabled() && ServerConfig.DISABLE_MANA_CONSUMPTION.get() && castSource.consumesMana())) {
            magicData.setMana(mana);
        }
    }
}
