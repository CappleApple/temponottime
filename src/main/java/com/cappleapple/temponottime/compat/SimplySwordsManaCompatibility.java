package com.cappleapple.temponottime.compat;

import com.cappleapple.temponottime.casting.CooldownManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SimplySwordsManaCompatibility {
    public static final String COOLDOWN_PREFIX = "temponottime:simply_swords/";

    private static final ThreadLocal<Item> SPENDING_ITEM = new ThreadLocal<>();
    private static final ThreadLocal<CooldownCapture> COOLDOWN_CAPTURE = new ThreadLocal<>();
    private static final Map<UUID, Map<Item, CapturedCooldown>> EFFECTIVE_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Map<Item, PendingRetime>> PENDING_RETIMES = new HashMap<>();

    private SimplySwordsManaCompatibility() {
    }

    public static boolean handles(LivingEntity entity) {
        return entity instanceof ServerPlayer && CooldownManager.INSTANCE.manaCompatibilityActive();
    }

    public static boolean canAfford(ServerPlayer player, float manaCost) {
        return CooldownManager.INSTANCE.canSpendExternalMana(player, manaCost);
    }

    public static void beginManaSpend(ItemStack stack) {
        SPENDING_ITEM.set(stack == null || stack.isEmpty() ? null : stack.getItem());
    }

    public static void endManaSpend() {
        SPENDING_ITEM.remove();
    }

    public static void spend(ServerPlayer player, float manaCost) {
        Item item = SPENDING_ITEM.get();
        int effectiveCooldown = 1;
        long gameTime = player.serverLevel().getGameTime();
        boolean hasCurrentCooldown = false;
        if (item != null) {
            Map<Item, CapturedCooldown> cooldowns = EFFECTIVE_COOLDOWNS.get(player.getUUID());
            if (cooldowns != null) {
                CapturedCooldown captured = cooldowns.get(item);
                if (captured != null && captured.gameTime() == gameTime) {
                    effectiveCooldown = captured.durationTicks();
                    hasCurrentCooldown = true;
                }
                cooldowns.remove(item);
                if (cooldowns.isEmpty()) {
                    EFFECTIVE_COOLDOWNS.remove(player.getUUID());
                }
            }
        }
        long instanceId = CooldownManager.INSTANCE.commitExternalManaUse(player, item, manaCost, effectiveCooldown);
        if (item != null && !hasCurrentCooldown && instanceId >= 0L) {
            PENDING_RETIMES.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>())
                    .put(item, new PendingRetime(instanceId, gameTime));
        }
    }

    public static void beginCooldownCapture(LivingEntity actor, ItemStack stack) {
        if (actor instanceof ServerPlayer player && handles(actor) && stack != null && !stack.isEmpty()) {
            COOLDOWN_CAPTURE.set(new CooldownCapture(player, stack.getItem()));
        } else {
            COOLDOWN_CAPTURE.remove();
        }
    }

    public static void captureEffectiveCooldown(Item item, int durationTicks) {
        CooldownCapture capture = COOLDOWN_CAPTURE.get();
        if (capture == null || capture.item() != item || durationTicks <= 0) {
            return;
        }
        ServerPlayer player = capture.player();
        long gameTime = player.serverLevel().getGameTime();
        Map<Item, PendingRetime> pendingByItem = PENDING_RETIMES.get(player.getUUID());
        PendingRetime pending = pendingByItem == null ? null : pendingByItem.remove(item);
        if (pendingByItem != null && pendingByItem.isEmpty()) {
            PENDING_RETIMES.remove(player.getUUID());
        }
        if (pending != null && pending.gameTime() == gameTime) {
            CooldownManager.INSTANCE.retimeExternalManaUse(player, item, pending.instanceId(), durationTicks);
            return;
        }
        EFFECTIVE_COOLDOWNS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>())
                .put(item, new CapturedCooldown(Math.max(1, durationTicks), gameTime));
    }

    public static void endCooldownCapture() {
        COOLDOWN_CAPTURE.remove();
    }

    public static void clear(ServerPlayer player) {
        EFFECTIVE_COOLDOWNS.remove(player.getUUID());
        PENDING_RETIMES.remove(player.getUUID());
    }

    public static boolean isExternalCooldown(String id) {
        return id.startsWith(COOLDOWN_PREFIX);
    }

    private record CooldownCapture(ServerPlayer player, Item item) {
    }

    private record CapturedCooldown(int durationTicks, long gameTime) {
    }

    private record PendingRetime(long instanceId, long gameTime) {
    }
}
