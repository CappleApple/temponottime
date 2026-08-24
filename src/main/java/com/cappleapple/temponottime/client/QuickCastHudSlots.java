package com.cappleapple.temponottime.client;

import com.cappleapple.temponottime.config.ClientConfig;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.player.ClientRenderCache;
import io.redspace.ironsspellbooks.player.KeyMappings;
import net.minecraft.world.phys.Vec2;

import java.util.ArrayList;
import java.util.List;

public final class QuickCastHudSlots {
    private QuickCastHudSlots() {
    }

    public static int visibleSpellCount(SpellSelectionManager spellSelection) {
        if (!ClientConfig.ONLY_SHOW_BOUND_QUICK_CAST_SLOTS.get()) {
            return spellSelection.getSpellCount();
        }
        return visibleSelections(spellSelection).size();
    }

    public static List<SpellSelectionManager.SelectionOption> visibleSelections(
            SpellSelectionManager spellSelection) {
        List<SpellSelectionManager.SelectionOption> allSpells = spellSelection.getAllSpells();
        if (!ClientConfig.ONLY_SHOW_BOUND_QUICK_CAST_SLOTS.get()) {
            return allSpells;
        }

        List<SpellSelectionManager.SelectionOption> visibleSpells = new ArrayList<>();
        int quickCastCount = KeyMappings.QUICK_CAST_MAPPINGS.size();
        for (int slot = 0; slot < allSpells.size() && slot < quickCastCount; slot++) {
            if (!KeyMappings.QUICK_CAST_MAPPINGS.get(slot).isUnbound()) {
                visibleSpells.add(allSpells.get(slot));
            }
        }
        return visibleSpells;
    }

    public static int visibleSelectionIndex(SpellSelectionManager spellSelection) {
        int selectedSlot = spellSelection.getGlobalSelectionIndex();
        if (!ClientConfig.ONLY_SHOW_BOUND_QUICK_CAST_SLOTS.get()) {
            return selectedSlot;
        }

        int visibleIndex = 0;
        int quickCastCount = KeyMappings.QUICK_CAST_MAPPINGS.size();
        for (int slot = 0; slot < selectedSlot && slot < quickCastCount; slot++) {
            if (!KeyMappings.QUICK_CAST_MAPPINGS.get(slot).isUnbound()) {
                visibleIndex++;
            }
        }
        return selectedSlot >= 0
                && selectedSlot < quickCastCount
                && !KeyMappings.QUICK_CAST_MAPPINGS.get(selectedSlot).isUnbound()
                ? visibleIndex
                : -1;
    }

    public static void generateRelativeLocations(SpellSelectionManager spellSelection,
                                                 int horizontalSpacing, int verticalSpacing) {
        if (!ClientConfig.ONLY_SHOW_BOUND_QUICK_CAST_SLOTS.get()) {
            ClientRenderCache.generateRelativeLocations(spellSelection, horizontalSpacing, verticalSpacing);
            return;
        }

        List<Vec2> locations = ClientRenderCache.relativeSpellBarSlotLocations;
        locations.clear();
        int spellCount = visibleSelections(spellSelection).size();
        if (spellCount == 0) {
            return;
        }

        int[] rowCounts = ClientRenderCache.getRowCounts(spellCount);
        int totalHeight = 0;
        for (int rowCount : rowCounts) {
            if (rowCount > 0) {
                totalHeight += horizontalSpacing;
            }
        }

        for (int row = 0; row < rowCounts.length; row++) {
            int rowCount = rowCounts[row];
            int rowWidth = horizontalSpacing * rowCount;
            for (int column = 0; column < rowCount; column++) {
                Vec2 location = new Vec2(-rowWidth / 2.0F + column * horizontalSpacing,
                        row * horizontalSpacing - totalHeight / 2.0F);
                location.add(-verticalSpacing / 2.0F);
                locations.add(location);
            }
        }
    }
}
