package com.cappleapple.temponottime.casting;

public record CastDecision(boolean allowed, Failure failure) {
    public enum Failure {
        NONE,
        NO_CHARGES,
        NO_CAPACITY,
        EVENT_CANCELED
    }

    public static CastDecision allow() {
        return new CastDecision(true, Failure.NONE);
    }

    public static CastDecision deny(Failure failure) {
        return new CastDecision(false, failure);
    }
}
