package com.cappleapple.temponottime.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import org.jetbrains.annotations.Nullable;

public final class PlayerCooldownDataSerializer implements IAttachmentSerializer<CompoundTag, PlayerCooldownData> {
    @Override
    public PlayerCooldownData read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
        PlayerCooldownData data = new PlayerCooldownData();
        data.load(tag);
        return data;
    }

    @Override
    public @Nullable CompoundTag write(PlayerCooldownData attachment, HolderLookup.Provider provider) {
        return attachment.save();
    }
}
