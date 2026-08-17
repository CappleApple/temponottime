package com.cappleapple.temponottime.api.event;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public final class SpellCastReservationEvent extends Event implements ICancellableEvent {
    private final Player player;
    private final AbstractSpell spell;
    private final int spellLevel;
    private final double castingDraw;

    public SpellCastReservationEvent(Player player, AbstractSpell spell, int spellLevel, double castingDraw) {
        this.player = player;
        this.spell = spell;
        this.spellLevel = spellLevel;
        this.castingDraw = castingDraw;
    }

    public Player getPlayer() { return player; }
    public AbstractSpell getSpell() { return spell; }
    public int getSpellLevel() { return spellLevel; }
    public double getCastingDraw() { return castingDraw; }
}
