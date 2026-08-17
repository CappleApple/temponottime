package com.cappleapple.temponottime.mixin;

import com.cappleapple.temponottime.config.ClientConfig;
import com.cappleapple.temponottime.network.ClientCooldownState;
import io.redspace.ironsspellbooks.config.ClientConfigs;
import io.redspace.ironsspellbooks.gui.overlays.SpellBarOverlay;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.player.ClientRenderCache;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SpellBarOverlay.class, remap = false)
public abstract class SpellBarOverlayMixin {
    @Shadow
    private static float alpha;

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/gui/overlays/SpellBarOverlay;flushTranslucency()V",
            shift = At.Shift.AFTER))
    private void temponottime$drawAvailableCharges(GuiGraphics graphics, DeltaTracker deltaTracker,
                                                    CallbackInfo callback) {
        if (!ClientCooldownState.enabled() || !ClientConfig.SHOW_CHARGES.get()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        var spellSelection = ClientMagicData.getSpellSelectionManager();
        var locations = ClientRenderCache.relativeSpellBarSlotLocations;
        int spellCount = Math.min(spellSelection.getSpellCount(), locations.size());
        if (minecraft.player == null || spellCount == 0) {
            return;
        }

        int centerX;
        int centerY;
        SpellBarOverlay.Anchor anchor = ClientConfigs.SPELL_BAR_ANCHOR.get();
        if (anchor == SpellBarOverlay.Anchor.Hotbar) {
            centerX = graphics.guiWidth() / 2 - Math.max(110, graphics.guiWidth() / 4);
            centerY = graphics.guiHeight() - Math.max(55, graphics.guiHeight() / 8);
        } else {
            centerX = switch (anchor) {
                case TopLeft, BottomLeft -> 0;
                case TopRight, BottomRight -> graphics.guiWidth();
                case Hotbar -> throw new IllegalStateException("Handled above");
            };
            centerY = switch (anchor) {
                case TopLeft, TopRight -> 0;
                case BottomLeft, BottomRight -> graphics.guiHeight();
                case Hotbar -> throw new IllegalStateException("Handled above");
            };
        }
        centerX += ClientConfigs.SPELL_BAR_X_OFFSET.get();
        centerY += ClientConfigs.SPELL_BAR_Y_OFFSET.get();
        centerX -= (locations.size() / 3) * 5;

        int opacity = Math.round(Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F);
        int color = opacity << 24 | 0xFFFFFF;
        for (int index = 0; index < spellCount; index++) {
            var spell = spellSelection.getSpellData(index).getSpell();
            var state = ClientCooldownState.spells().get(spell.getSpellId());
            if (state == null || state.availableCharges() <= 1) {
                continue;
            }
            int slotX = centerX + (int) locations.get(index).x;
            int slotY = centerY + (int) locations.get(index).y;
            graphics.drawString(minecraft.font, Integer.toString(state.availableCharges()),
                    slotX + 2, slotY + 2, color, true);
        }
    }
}
