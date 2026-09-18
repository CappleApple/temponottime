package com.cappleapple.temponottime.casting;

import com.cappleapple.temponottime.compat.SimplySwordsManaCompatibility;
import com.cappleapple.temponottime.config.ServerConfig.RecoveryMode;
import com.cappleapple.temponottime.data.CooldownInstance;
import com.cappleapple.temponottime.data.PlayerCooldownData;

import java.util.Comparator;

/** Converts one instant-mana dose into independent progress for each spell's spent charges. */
public final class InstantManaRecovery {
    private InstantManaRecovery() {
    }

    public static boolean apply(PlayerCooldownData data, double restoredMana, RecoveryMode mode) {
        if (!Double.isFinite(restoredMana) || restoredMana <= 0.0) return false;
        boolean changed = false;
        for (var entry : data.cooldowns().entrySet()) {
            // Simply Swords owns its native item timers; these instances only track reserve debt.
            if (SimplySwordsManaCompatibility.isExternalCooldown(entry.getKey())) continue;
            var instances = entry.getValue();
            double budget = restoredMana;
            for (CooldownInstance instance : instances.stream()
                    .sorted(Comparator.comparingLong(CooldownInstance::id)).toList()) {
                // Recasts and unfinished continuous casts have not started their recharge yet.
                if (instance.waitingForIronCooldown()) continue;
                double available = mode == RecoveryMode.PARALLEL ? restoredMana : budget;
                if (available <= 0.0) break;
                double needed = instance.remainingFraction() * instance.recoveryManaCost();
                double spent = Math.min(available, needed);
                double progress = spent >= needed ? instance.remainingTicks()
                        : (spent / instance.recoveryManaCost()) * instance.durationTicks();
                if (instance.advance(progress)) instances.remove(instance);
                changed |= progress > 0.0;
                budget = Math.max(0.0, budget - spent);
            }
        }
        if (changed) {
            data.removeCompleted();
            data.markDirty();
        }
        return changed;
    }
}
