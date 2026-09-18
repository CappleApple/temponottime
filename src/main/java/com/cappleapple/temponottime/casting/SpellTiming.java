package com.cappleapple.temponottime.casting;

import com.cappleapple.temponottime.config.ServerConfig;
import com.cappleapple.temponottime.network.ClientCooldownState;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;

public final class SpellTiming {
    private static final ClassValue<Boolean> CUSTOM_CAST_TIME = new ClassValue<>() {
        @Override protected Boolean computeValue(Class<?> type) {
            try {
                return type.getMethod("getCastTime", int.class).getDeclaringClass() != AbstractSpell.class
                        || type.getMethod("getEffectiveCastTime", int.class, LivingEntity.class).getDeclaringClass() != AbstractSpell.class;
            } catch (NoSuchMethodException exception) {
                // Unknown timing implementations stay untouched unless explicitly opted in.
                return true;
            }
        }
    };

    private SpellTiming() {
    }

    public static int rechargeBaseTicks(int originalTicks, Player player) {
        if (player == null) return originalTicks;
        TimingNormalization settings;
        if (player.level().isClientSide) {
            if (!ClientCooldownState.enabled()) return originalTicks;
            settings = ClientCooldownState.snapshot().rechargeNormalization();
        } else {
            if (!ServerConfig.enabled()) return originalTicks;
            settings = ServerConfig.rechargeNormalization();
        }
        return settings.flatModifier() == 0.0 ? originalTicks : settings.adjustedBaseTicks(originalTicks);
    }

    public static int effectiveCastTicks(AbstractSpell spell, int level, Player player, int originalEffectiveTicks) {
        if (player == null) return originalEffectiveTicks;
        TimingNormalization settings;
        if (player.level().isClientSide) {
            if (!ClientCooldownState.enabled()) return originalEffectiveTicks;
            settings = ClientCooldownState.snapshot().castTimeNormalization();
        } else {
            if (!ServerConfig.enabled()) return originalEffectiveTicks;
            settings = ServerConfig.castTimeNormalization();
        }
        return effectiveCastTicks(spell, level, originalEffectiveTicks, settings);
    }
    public static int effectiveCastTicks(AbstractSpell spell, int level, int originalEffectiveTicks, TimingNormalization settings) {
        if (!settings.affectCustomCastTimes() && CUSTOM_CAST_TIME.get(spell.getClass())) return originalEffectiveTicks;
        return settings.castTicks(spell.getCastTime(level), originalEffectiveTicks);
    }
}
