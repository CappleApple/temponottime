package com.cappleapple.temponottime.mixin;

import com.cappleapple.temponottime.client.QuickCastHudSlots;
import com.cappleapple.temponottime.config.ClientConfig;
import com.cappleapple.temponottime.network.ClientCooldownState;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = SpellBarOverlay.class, remap = false)
public abstract class SpellBarOverlayMixin {
    @Shadow
    private static float alpha;

    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/api/magic/SpellSelectionManager;getSpellCount()I"))
    private int temponottime$getVisibleSpellCount(SpellSelectionManager spellSelection) {
        return QuickCastHudSlots.visibleSpellCount(spellSelection);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/player/ClientRenderCache;generateRelativeLocations(Lio/redspace/ironsspellbooks/api/magic/SpellSelectionManager;II)V"))
    private void temponottime$generateVisibleSlotLocations(SpellSelectionManager spellSelection,
                                                          int horizontalSpacing, int verticalSpacing) {
        QuickCastHudSlots.generateRelativeLocations(spellSelection, horizontalSpacing, verticalSpacing);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/api/magic/SpellSelectionManager;getAllSpells()Ljava/util/List;"))
    private List<SpellSelectionManager.SelectionOption> temponottime$getVisibleSpells(
            SpellSelectionManager spellSelection) {
        return QuickCastHudSlots.visibleSelections(spellSelection);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/api/magic/SpellSelectionManager;getGlobalSelectionIndex()I"))
    private int temponottime$getVisibleSelectionIndex(SpellSelectionManager spellSelection) {
        return QuickCastHudSlots.visibleSelectionIndex(spellSelection);
    }

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
        var visibleSpells = QuickCastHudSlots.visibleSelections(spellSelection);
        var locations = ClientRenderCache.relativeSpellBarSlotLocations;
        int spellCount = Math.min(visibleSpells.size(), locations.size());
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
            var spell = visibleSpells.get(index).spellData.getSpell();
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
