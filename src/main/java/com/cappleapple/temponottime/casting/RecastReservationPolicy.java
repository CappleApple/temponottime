package com.cappleapple.temponottime.casting;

/** Distinguishes an initial spell activation from blasts supplied by Iron's active recast meter. */
public final class RecastReservationPolicy {
    private RecastReservationPolicy() {
    }

    public static boolean consumesTempoUse(boolean activeIronRecast) {
        return !activeIronRecast;
    }
}
