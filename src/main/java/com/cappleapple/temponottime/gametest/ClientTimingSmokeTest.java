package com.cappleapple.temponottime.gametest;

import com.cappleapple.temponottime.TempoNotTime;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.util.TooltipsUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** Development-run smoke check; the gametest package is excluded from release jars. */
@EventBusSubscriber(modid = TempoNotTime.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientTimingSmokeTest {
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Force tooltip transformation before entering a world, including all redirect signatures.
            TooltipsUtils.getCastTimeComponent(CastType.INSTANT, "0");
            TempoNotTime.LOGGER.info("Client timing tooltip mixin smoke check passed");
        });
    }
}
