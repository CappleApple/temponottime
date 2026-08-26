package com.cappleapple.temponottime.mixin;

import com.cappleapple.temponottime.client.BoundQuickCastToggleButton;
import io.redspace.ironsspellbooks.gui.inscription_table.InscriptionTableMenu;
import io.redspace.ironsspellbooks.gui.inscription_table.InscriptionTableScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = InscriptionTableScreen.class, remap = false)
public abstract class InscriptionTableScreenMixin extends AbstractContainerScreen<InscriptionTableMenu> {
    protected InscriptionTableScreenMixin(InscriptionTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void temponottime$addBoundQuickCastToggle(CallbackInfo callback) {
        addRenderableWidget(new BoundQuickCastToggleButton(leftPos + 43, topPos + 17));
    }
}
