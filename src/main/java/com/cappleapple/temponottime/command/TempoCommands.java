package com.cappleapple.temponottime.command;

import com.cappleapple.temponottime.casting.CooldownManager;
import com.cappleapple.temponottime.config.SpellOverrideManager;
import com.cappleapple.temponottime.data.CooldownInstance;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Comparator;

public final class TempoCommands {
    private TempoCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("temponottime")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("info").executes(context -> info(context.getSource())))
                .then(Commands.literal("reserve").executes(context -> reserve(context.getSource())))
                .then(Commands.literal("cooldowns").executes(context -> cooldowns(context.getSource())))
                .then(Commands.literal("clear").executes(context -> clear(context.getSource())))
                .then(Commands.literal("reload").executes(context -> reload(context.getSource())))
                .then(Commands.literal("charges")
                        .then(Commands.argument("spell", StringArgumentType.string())
                                .executes(context -> charges(context.getSource(), StringArgumentType.getString(context, "spell"))))));
    }

    private static int info(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CooldownManager manager = CooldownManager.INSTANCE;
        source.sendSuccess(() -> Component.translatable("command.temponottime.info",
                format(manager.maximumCastingReserve(player)), format(manager.usedCastingReserve(player)), format(manager.freeCastingReserve(player)),
                manager.activeCooldownCount(player), format(manager.recoveryMultiplier(player))), false);
        return manager.activeCooldownCount(player);
    }

    private static int reserve(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CooldownManager manager = CooldownManager.INSTANCE;
        source.sendSuccess(() -> Component.translatable("command.temponottime.reserve",
                format(manager.usedCastingReserve(player)), format(manager.maximumCastingReserve(player)), format(manager.freeCastingReserve(player))), false);
        return (int) Math.floor(manager.freeCastingReserve(player));
    }

    private static int cooldowns(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        var instances = CooldownManager.INSTANCE.data(player).allInstances().stream()
                .sorted(Comparator.comparing(CooldownInstance::spellId).thenComparingLong(CooldownInstance::id)).toList();
        source.sendSuccess(() -> Component.translatable("command.temponottime.cooldowns.header", instances.size()), false);
        for (CooldownInstance instance : instances) {
            source.sendSuccess(() -> Component.translatable("command.temponottime.cooldowns.entry", instance.spellId(),
                    format(instance.remainingTicks() / 20.0), format(instance.castingDraw())), false);
        }
        return instances.size();
    }

    private static int clear(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CooldownManager.INSTANCE.clear(player);
        source.sendSuccess(() -> Component.translatable("command.temponottime.clear"), true);
        return 1;
    }

    private static int reload(CommandSourceStack source) {
        SpellOverrideManager.reload();
        source.sendSuccess(() -> Component.translatable("command.temponottime.reload", SpellOverrideManager.size()), true);
        return SpellOverrideManager.size();
    }

    private static int charges(CommandSourceStack source, String spellId) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ResourceLocation resourceLocation = ResourceLocation.tryParse(spellId);
        var spell = resourceLocation == null ? SpellRegistry.none() : SpellRegistry.getSpell(resourceLocation);
        if (spell == SpellRegistry.none()) {
            source.sendFailure(Component.translatable("command.temponottime.unknown_spell", spellId));
            return 0;
        }
        int level = CooldownManager.INSTANCE.data(player).forSpell(spellId).stream().findFirst().map(CooldownInstance::spellLevel).orElse(1);
        int maximum = CooldownManager.INSTANCE.maxCharges(player, spell, level);
        int available = CooldownManager.INSTANCE.availableCharges(player, spell, level);
        source.sendSuccess(() -> Component.translatable("command.temponottime.charges", spellId, available, maximum), false);
        return available;
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
