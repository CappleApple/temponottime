package com.cappleapple.temponottime;

import com.cappleapple.temponottime.casting.CooldownManager;
import com.cappleapple.temponottime.command.TempoCommands;
import com.cappleapple.temponottime.config.ClientConfig;
import com.cappleapple.temponottime.config.ServerConfig;
import com.cappleapple.temponottime.network.TempoNetwork;
import com.cappleapple.temponottime.registry.TempoRegistries;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(TempoNotTime.MOD_ID)
public final class TempoNotTime {
    public static final String MOD_ID = "temponottime";
    public static final String DISPLAY_NAME = "Tempo Not Time";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TempoNotTime(IEventBus modBus, ModContainer container) {
        TempoRegistries.register(modBus);
        modBus.addListener(TempoRegistries::addPlayerAttributes);
        modBus.addListener(TempoNetwork::register);

        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC, MOD_ID + "-server.toml");
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, MOD_ID + "-client.toml");

        NeoForge.EVENT_BUS.register(CooldownManager.INSTANCE);
        NeoForge.EVENT_BUS.register(TempoCommands.class);
        LOGGER.info("Initializing {}", DISPLAY_NAME);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
