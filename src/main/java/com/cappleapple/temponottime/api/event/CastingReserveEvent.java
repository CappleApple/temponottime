package com.cappleapple.temponottime.api.event;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;

public final class CastingReserveEvent extends Event {
    private final Player player;
    private final double originalReserve;
    private double reserve;

    public CastingReserveEvent(Player player, double reserve) {
        this.player = player;
        this.originalReserve = reserve;
        this.reserve = reserve;
    }

    public Player getPlayer() { return player; }
    public double getOriginalReserve() { return originalReserve; }
    public double getReserve() { return reserve; }
    public void setReserve(double reserve) { this.reserve = reserve; }
}
