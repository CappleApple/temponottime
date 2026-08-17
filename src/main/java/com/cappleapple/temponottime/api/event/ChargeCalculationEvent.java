package com.cappleapple.temponottime.api.event;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;

public final class ChargeCalculationEvent extends Event {
    private final Player player;
    private final AbstractSpell spell;
    private final int spellLevel;
    private final int originalCharges;
    private int charges;

    public ChargeCalculationEvent(Player player, AbstractSpell spell, int spellLevel, int charges) {
        this.player = player;
        this.spell = spell;
        this.spellLevel = spellLevel;
        this.originalCharges = charges;
        this.charges = charges;
    }

    public Player getPlayer() { return player; }
    public AbstractSpell getSpell() { return spell; }
    public int getSpellLevel() { return spellLevel; }
    public int getOriginalCharges() { return originalCharges; }
    public int getCharges() { return charges; }
    public void setCharges(int charges) { this.charges = charges; }
}
