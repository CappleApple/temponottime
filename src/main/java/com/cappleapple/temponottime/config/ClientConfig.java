package com.cappleapple.temponottime.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue SHOW_CHARGES;
    public static final ModConfigSpec.BooleanValue ONLY_SHOW_BOUND_QUICK_CAST_SLOTS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("hud");
        SHOW_CHARGES = builder.comment("Enable spell-icon charge count display feature true/false.")
                .define("show_charges", true);
        ONLY_SHOW_BOUND_QUICK_CAST_SLOTS = builder
                .comment("Only show spell-bar slots whose matching Iron's quick cast keybinding is bound.")
                .define("only_show_bound_quick_cast_slots", false);
        builder.pop();
        SPEC = builder.build();
    }

    private ClientConfig() {
    }
}
