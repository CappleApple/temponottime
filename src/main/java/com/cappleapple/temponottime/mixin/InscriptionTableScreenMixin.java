package com.cappleapple.temponottime.mixin;

import com.cappleapple.temponottime.client.BoundQuickCastToggleButton;
import com.cappleapple.temponottime.network.ClientCooldownState;
import com.cappleapple.temponottime.casting.SpellTiming;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.injection.Redirect;
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

    @Redirect(method = "renderLorePage", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;getEffectiveCastTime(ILnet/minecraft/world/entity/LivingEntity;)I"))
    private int temponottime$showBaseCastTime(AbstractSpell spell, int level, LivingEntity entity) {
        int original = spell.getEffectiveCastTime(level, entity);
        return ClientCooldownState.enabled()
                ? SpellTiming.effectiveCastTicks(spell, level, original, ClientCooldownState.snapshot().castTimeNormalization()) : original;
    }

    @Redirect(method = "renderLorePage", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;getSpellCooldown()I"))
    private int temponottime$showBaseCooldown(AbstractSpell spell) {
        int original = spell.getSpellCooldown();
        if (!ClientCooldownState.enabled()) return original;
        var settings = ClientCooldownState.snapshot().rechargeNormalization();
        if (!settings.enabled() && settings.flatModifier() == 0.0) return original;
        int base = settings.adjustedBaseTicks(original);
        return (int) Math.clamp(Math.round(settings.normalizeAdjustedTicks(base, base)), 1L, Integer.MAX_VALUE);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void temponottime$addBoundQuickCastToggle(CallbackInfo callback) {
        addRenderableWidget(new BoundQuickCastToggleButton(leftPos + 43, topPos + 17));
    }
}
