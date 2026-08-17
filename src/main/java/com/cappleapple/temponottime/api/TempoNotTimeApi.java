package com.cappleapple.temponottime.api;

import com.cappleapple.temponottime.casting.CooldownManager;
import com.cappleapple.temponottime.data.CooldownInstance;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public final class TempoNotTimeApi {
    private TempoNotTimeApi() {
    }

    public static double maximumCastingReserve(Player player) { return CooldownManager.INSTANCE.maximumCastingReserve(player); }
    public static double usedCastingReserve(Player player) { return CooldownManager.INSTANCE.usedCastingReserve(player); }
    public static double freeCastingReserve(Player player) { return CooldownManager.INSTANCE.freeCastingReserve(player); }
    public static int maximumCharges(Player player, AbstractSpell spell, int spellLevel) { return CooldownManager.INSTANCE.maxCharges(player, spell, spellLevel); }
    public static int availableCharges(Player player, AbstractSpell spell, int spellLevel) { return CooldownManager.INSTANCE.availableCharges(player, spell, spellLevel); }
    public static List<CooldownInstance> activeCooldowns(Player player) { return List.copyOf(CooldownManager.INSTANCE.data(player).allInstances()); }
    public static double castingRecoveryMultiplier(Player player) { return CooldownManager.INSTANCE.recoveryMultiplier(player); }
}
