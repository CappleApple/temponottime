package com.cappleapple.temponottime.api.event;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;

public final class CastingDrawEvent extends Event {
    private final Player player;
    private final AbstractSpell spell;
    private final int spellLevel;
    private final double originalDraw;
    private double draw;

    public CastingDrawEvent(Player player, AbstractSpell spell, int spellLevel, double draw) {
        this.player = player;
        this.spell = spell;
        this.spellLevel = spellLevel;
        this.originalDraw = draw;
        this.draw = draw;
    }

    public Player getPlayer() { return player; }
    public AbstractSpell getSpell() { return spell; }
    public int getSpellLevel() { return spellLevel; }
    public double getOriginalDraw() { return originalDraw; }
    public double getDraw() { return draw; }
    public void setDraw(double draw) { this.draw = draw; }
}
