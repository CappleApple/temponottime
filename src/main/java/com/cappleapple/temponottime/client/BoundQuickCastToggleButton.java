package com.cappleapple.temponottime.client;

import com.cappleapple.temponottime.TempoNotTime;
import com.cappleapple.temponottime.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class BoundQuickCastToggleButton extends AbstractButton {
    public static final int SIZE = 14;
    private static final int TEXTURE_SIZE = 256;
    private static final ResourceLocation ENABLED_TEXTURE =
            TempoNotTime.id("textures/gui/quick_cast_bound_button.png");
    private static final ResourceLocation DISABLED_TEXTURE =
            TempoNotTime.id("textures/gui/quick_cast_bound_button_disabled.png");

    public BoundQuickCastToggleButton(int x, int y) {
        super(x, y, SIZE, SIZE, stateMessage());
        refreshMessage();
    }

    @Override
    public void onPress() {
        ClientConfig.ONLY_SHOW_BOUND_QUICK_CAST_SLOTS.set(!isEnabled());
        ClientConfig.ONLY_SHOW_BOUND_QUICK_CAST_SLOTS.save();
        refreshMessage();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation texture = isEnabled() ? ENABLED_TEXTURE : DISABLED_TEXTURE;
        graphics.blit(texture, getX(), getY(), width, height, 0.0F, 0.0F,
                TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    private void refreshMessage() {
        Component message = stateMessage();
        setMessage(message);
        setTooltip(Tooltip.create(message));
    }

    private static boolean isEnabled() {
        return ClientConfig.ONLY_SHOW_BOUND_QUICK_CAST_SLOTS.get();
    }

    private static Component stateMessage() {
        return Component.translatable("gui.temponottime.only_show_bound_quick_cast_slots",
                Component.translatable(isEnabled() ? "options.on" : "options.off"));
    }
}
