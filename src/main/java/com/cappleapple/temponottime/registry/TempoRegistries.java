package com.cappleapple.temponottime.registry;

import com.cappleapple.temponottime.TempoNotTime;
import com.cappleapple.temponottime.data.PlayerCooldownData;
import com.cappleapple.temponottime.data.PlayerCooldownDataSerializer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class TempoRegistries {
    private static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, TempoNotTime.MOD_ID);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, TempoNotTime.MOD_ID);

    public static final DeferredHolder<Attribute, Attribute> CASTING_RESERVE = ATTRIBUTES.register("casting_reserve",
            () -> new RangedAttribute("attribute.temponottime.casting_reserve", 0.0, 0.0, 1_000_000.0).setSyncable(true));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerCooldownData>> COOLDOWN_DATA = ATTACHMENTS.register("cooldown_data",
            () -> AttachmentType.builder(holder -> new PlayerCooldownData()).serialize(new PlayerCooldownDataSerializer()).build());

    private TempoRegistries() {
    }

    public static void register(IEventBus modBus) {
        ATTRIBUTES.register(modBus);
        ATTACHMENTS.register(modBus);
    }

    public static void addPlayerAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, CASTING_RESERVE);
    }
}
