package com.cappleapple.temponottime.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue SHOW_CHARGES;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("hud");
        SHOW_CHARGES = builder.comment("Enable spell-icon charge count display feature true/false.")
                .define("show_charges", true);
        builder.pop();
        SPEC = builder.build();
    }

    private ClientConfig() {
    }
}
