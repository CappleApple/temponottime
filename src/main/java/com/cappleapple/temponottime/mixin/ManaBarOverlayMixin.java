package com.cappleapple.temponottime.mixin;

import com.cappleapple.temponottime.network.ClientCooldownState;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.config.ClientConfigs;
import io.redspace.ironsspellbooks.gui.overlays.ManaBarOverlay;
import io.redspace.ironsspellbooks.item.CastingItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ManaBarOverlay.class, remap = false)
public abstract class ManaBarOverlayMixin {
    private static final int DEFAULT_IMAGE_WIDTH = 98;
    private static final int XP_IMAGE_WIDTH = 188;
    private static final int IMAGE_HEIGHT = 21;

    @Shadow
    private static int getBarX(ManaBarOverlay.Anchor anchor, int screenWidth) {
        throw new AssertionError();
    }

    @Shadow
    private static int getBarY(ManaBarOverlay.Anchor anchor, int screenHeight, Gui gui) {
        throw new AssertionError();
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void temponottime$renderCapacityBar(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo callback) {
        if (!ClientCooldownState.manaDisabled()) {
            return;
        }
        callback.cancel();

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || minecraft.player.isSpectator()) {
            return;
        }

        var snapshot = ClientCooldownState.snapshot();
        double maximum = Math.max(0.0, snapshot.maximumCastingReserve());
        double available = Math.max(0.0, maximum + snapshot.castingReserveCredit() - snapshot.usedCastingReserve());
        double availableFraction = maximum > 0.0 ? Math.clamp(available / maximum, 0.0, 1.0) : available > 0.0 ? 1.0 : 0.0;
        if (!temponottime$shouldShowCapacityBar(minecraft.player, available, maximum)) {
            return;
        }

        ManaBarOverlay.Anchor anchor = ClientConfigs.MANA_BAR_ANCHOR.get();
        if (anchor == ManaBarOverlay.Anchor.XP && minecraft.player.getJumpRidingScale() > 0) {
            return;
        }
        int barX = getBarX(anchor, graphics.guiWidth()) + ClientConfigs.MANA_BAR_X_OFFSET.get();
        int barY = getBarY(anchor, graphics.guiHeight(), minecraft.gui) - ClientConfigs.MANA_BAR_Y_OFFSET.get();
        int imageWidth = anchor == ManaBarOverlay.Anchor.XP ? XP_IMAGE_WIDTH : DEFAULT_IMAGE_WIDTH;
        int spriteX = anchor == ManaBarOverlay.Anchor.XP ? 68 : 0;
        int spriteY = anchor == ManaBarOverlay.Anchor.XP ? 40 : 0;

        graphics.blit(ManaBarOverlay.TEXTURE, barX, barY, spriteX, spriteY, imageWidth, IMAGE_HEIGHT, 256, 256);
        graphics.blit(ManaBarOverlay.TEXTURE, barX, barY, spriteX, spriteY + IMAGE_HEIGHT,
                (int) (imageWidth * availableFraction), IMAGE_HEIGHT);

        if (ClientConfigs.MANA_BAR_TEXT_VISIBLE.get()) {
            int availableText = (int) Math.round(available);
            int capacity = (int) Math.round(maximum);
            String text = availableText + "/" + capacity;
            int textX = barX + imageWidth / 2 - minecraft.font.width(text) / 2
                    + ClientConfigs.MANA_TEXT_X_OFFSET.get();
            int textY = barY + (anchor == ManaBarOverlay.Anchor.XP ? 3 : 11)
                    + ClientConfigs.MANA_TEXT_Y_OFFSET.get();
            graphics.drawString(minecraft.font, text, textX, textY, ChatFormatting.AQUA.getColor());
        }
    }

    private static boolean temponottime$shouldShowCapacityBar(LocalPlayer player, double available, double maximum) {
        ManaBarOverlay.Display display = ClientConfigs.MANA_BAR_DISPLAY.get();
        if (display == ManaBarOverlay.Display.Never) {
            return false;
        }
        if (display == ManaBarOverlay.Display.Always) {
            return true;
        }
        return available + 1.0e-7 < maximum || player.isHolding(stack -> stack.getItem() instanceof CastingItem
                || ISpellContainer.isSpellContainer(stack) && !ISpellContainer.get(stack).mustEquip());
    }
}
