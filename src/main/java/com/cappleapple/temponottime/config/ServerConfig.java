package com.cappleapple.temponottime.config;

import com.cappleapple.temponottime.casting.ChargeRequirementFormula;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class ServerConfig {
    public enum RecoveryMode {
        PARALLEL,
        SEQUENTIAL
    }

    public enum DeathCooldownBehavior {
        PRESERVE,
        CLEAR
    }

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue CONVERT_MAX_MANA;
    public static final ModConfigSpec.BooleanValue CONVERT_MANA_REGEN;
    public static final ModConfigSpec.BooleanValue DISABLE_MANA_CONSUMPTION;
    public static final ModConfigSpec.EnumValue<DeathCooldownBehavior> DEATH_BEHAVIOR;
    public static final ModConfigSpec.BooleanValue CREATIVE_BYPASSES_CAPACITY;
    public static final ModConfigSpec.BooleanValue CREATIVE_BYPASSES_CHARGES;

    public static final ModConfigSpec.BooleanValue CAPACITY_ENABLED;
    public static final ModConfigSpec.DoubleValue MAX_MANA_TO_CAPACITY_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue MANA_COST_TO_CAPACITY_COST_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue MINIMUM_CAPACITY;
    public static final ModConfigSpec.DoubleValue MAXIMUM_CAPACITY;
    public static final ModConfigSpec.BooleanValue ALLOW_OVERCAPACITY_SINGLE_CAST;
    public static final ModConfigSpec.DoubleValue ZERO_MANA_SPELL_CAPACITY_COST;

    public static final ModConfigSpec.BooleanValue CHARGES_ENABLED;
    public static final ModConfigSpec.ConfigValue<String> CHARGE_REQUIREMENT_FORMULA;
    public static final ModConfigSpec.IntValue MINIMUM_CHARGES;
    public static final ModConfigSpec.IntValue MAXIMUM_CHARGES;
    public static final ModConfigSpec.EnumValue<RecoveryMode> RECOVERY_MODE;

    public static final ModConfigSpec.BooleanValue LOAD_ENABLED;
    public static final ModConfigSpec.IntValue FREE_COOLDOWNS;
    public static final ModConfigSpec.DoubleValue PENALTY_PER_ADDITIONAL_COOLDOWN;
    public static final ModConfigSpec.DoubleValue MINIMUM_RECOVERY_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue COUNT_PER_CHARGE;

    public static final ModConfigSpec.DoubleValue MANA_REGEN_TO_RECOVERY_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue MAXIMUM_TOTAL_RECOVERY_MULTIPLIER;

    public static final ModConfigSpec.BooleanValue RECHARGE_NORMALIZATION_ENABLED;
    public static final ModConfigSpec.DoubleValue NORMAL_RECHARGE_SECONDS;
    public static final ModConfigSpec.DoubleValue SHORT_RECHARGE_STRENGTH;
    public static final ModConfigSpec.DoubleValue LONG_RECHARGE_STRENGTH;
    public static final ModConfigSpec.DoubleValue NORMALIZATION_SPREAD;

    public static final ModConfigSpec.BooleanValue DEBUG_LOGGING;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("general");
        ENABLED = builder.comment("Enable Tempo Not Time feature true/false.")
                .define("enabled", true);
        CONVERT_MAX_MANA = builder.comment("Enable Max Mana to Casting Reserve conversion feature true/false.")
                .define("convert_max_mana_to_casting_reserve", true);
        CONVERT_MANA_REGEN = builder.comment("Enable Mana Regeneration to Casting Regeneration conversion feature true/false.")
                .define("convert_mana_regeneration_to_casting_regeneration", true);
        DISABLE_MANA_CONSUMPTION = builder.comment("Enable mana-free casting feature true/false.")
                .define("disable_mana_consumption", true);
        DEATH_BEHAVIOR = builder.comment("Whether recharge debt survives player death.")
                .defineEnum("death_cooldown_behavior", DeathCooldownBehavior.PRESERVE);
        CREATIVE_BYPASSES_CAPACITY = builder.comment("Enable creative Casting Reserve bypass feature true/false.")
                .define("creative_bypasses_casting_reserve", false);
        CREATIVE_BYPASSES_CHARGES = builder.comment("Enable creative charge bypass feature true/false.")
                .define("creative_bypasses_charges", false);
        builder.pop();

        builder.push("casting_reserve");
        CAPACITY_ENABLED = builder.comment("Enable casting reserve feature true/false.")
                .define("enabled", true);
        MAX_MANA_TO_CAPACITY_MULTIPLIER = builder.comment("Multiplier applied to effective Iron's Max Mana when calculating Casting Reserve.")
                .defineInRange("max_mana_to_casting_reserve_multiplier", 1.0, 0.0, 1000.0);
        MANA_COST_TO_CAPACITY_COST_MULTIPLIER = builder.comment("Multiplier applied to effective spell mana cost when calculating Casting Draw.")
                .defineInRange("mana_cost_to_casting_draw_multiplier", 1.0, 0.0, 1000.0);
        MINIMUM_CAPACITY = builder.comment("Lower clamp for effective Casting Reserve.")
                .defineInRange("minimum_casting_reserve", 0.0, 0.0, 1_000_000.0);
        MAXIMUM_CAPACITY = builder.comment("Upper clamp for effective Casting Reserve.")
                .defineInRange("maximum_casting_reserve", 100_000.0, 0.0, 1_000_000.0);
        ALLOW_OVERCAPACITY_SINGLE_CAST = builder.comment("Enable single-cast Casting Reserve overdraw feature true/false.")
                .define("allow_overreserve_single_cast", false);
        ZERO_MANA_SPELL_CAPACITY_COST = builder.comment("Safe Casting Draw used for zero, negative, or invalid spell mana costs.")
                .defineInRange("zero_mana_spell_casting_draw", 1.0, 0.0001, 1_000_000.0);
        builder.pop();

        builder.push("charges");
        CHARGES_ENABLED = builder.comment("Enable charges feature true/false.")
                .define("enabled", true);
        CHARGE_REQUIREMENT_FORMULA = builder.comment(
                        "Formula returning the total Casting Reserve required to unlock charge number 'charge'.",
                        "Available variables: casting_draw, casting_reserve, charge. Operators: +, -, *, /, %, ^, and parentheses.",
                        "Available functions: pow, min, max, abs, sqrt, floor, ceil, log, log2.",
                        "The result must be finite, positive, and increase for every subsequent charge.",
                        "Original linear behavior: casting_draw * charge",
                        "Cumulative doubling behavior: casting_draw * (2 ^ charge - 1)",
                        "Default doubling-threshold behavior: casting_draw * 2 ^ (charge - 1)")
                .define("casting_reserve_requirement_formula", ChargeRequirementFormula.DEFAULT_EXPRESSION,
                        ChargeRequirementFormula::isValidConfigValue);
        MINIMUM_CHARGES = builder.comment("Global lower charge clamp.")
                .defineInRange("minimum_charges", 1, 1, 10_000);
        MAXIMUM_CHARGES = builder.comment("Global upper charge clamp.")
                .defineInRange("maximum_charges", 10, 1, 10_000);
        RECOVERY_MODE = builder.comment("SEQUENTIAL recovers one spent charge per spell at a time and is the default; PARALLEL recovers every spent charge at once.")
                .defineEnum("recovery_mode", RecoveryMode.SEQUENTIAL);
        builder.pop();

        builder.push("cooldown_load");
        LOAD_ENABLED = builder.comment("Enable cooldown load feature true/false.")
                .define("enabled", false);
        FREE_COOLDOWNS = builder.comment("Number of active cooldowns before load penalties begin.")
                .defineInRange("free_cooldowns", 1, 0, 10_000);
        PENALTY_PER_ADDITIONAL_COOLDOWN = builder.comment("Penalty in 1 / (1 + penalty * additional cooldowns).")
                .defineInRange("penalty_per_additional_cooldown", 0.20, 0.0, 1000.0);
        MINIMUM_RECOVERY_MULTIPLIER = builder.comment("Floor preventing load from freezing Casting Recovery.")
                .defineInRange("minimum_recovery_multiplier", 0.25, 0.0001, 1.0);
        COUNT_PER_CHARGE = builder.comment("Enable per-charge cooldown load counting true/false.")
                .define("count_per_charge", true);
        builder.pop();

        builder.push("casting_recovery");
        MANA_REGEN_TO_RECOVERY_MULTIPLIER = builder.comment("Multiplier for converted Casting Regeneration above or below Iron's baseline value of 1.0.")
                .defineInRange("casting_regeneration_to_recovery_multiplier", 1.0, 0.0, 1000.0);
        MAXIMUM_TOTAL_RECOVERY_MULTIPLIER = builder.comment("Maximum Casting Recovery speed after Casting Regeneration conversion and before cooldown load.")
                .defineInRange("maximum_total_recovery_multiplier", 4.0, 0.05, 1000.0);
        builder.pop();

        builder.push("recharge_normalization");
        RECHARGE_NORMALIZATION_ENABLED = builder.comment("Enable recharge normalization feature true/false.")
                .define("enabled", true);
        NORMAL_RECHARGE_SECONDS = builder.comment("Recharge duration considered normal and used as the center of the curve.")
                .defineInRange("normal_recharge_seconds", 10.0, 0.05, 86_400.0);
        SHORT_RECHARGE_STRENGTH = builder.comment("How aggressively shorter recharges are compressed toward normal. Zero leaves them unchanged.")
                .defineInRange("short_recharge_strength", 0.8, 0.0, 1000.0);
        LONG_RECHARGE_STRENGTH = builder.comment("How aggressively longer recharges are compressed toward normal. Zero leaves them unchanged.")
                .defineInRange("long_recharge_strength", 0.5, 0.0, 1000.0);
        NORMALIZATION_SPREAD = builder.comment("Breadth of the relatively unmodified area around normal recharge.")
                .defineInRange("normalization_spread", 8.0, 0.001, 86_400.0);
        builder.pop();

        builder.push("diagnostics");
        DEBUG_LOGGING = builder.comment("Enable diagnostic logging feature true/false.")
                .define("debug_logging", false);
        builder.pop();

        SPEC = builder.build();
    }

    private ServerConfig() {
    }

    public static boolean enabled() {
        try {
            return ENABLED.get();
        } catch (IllegalStateException ignoredDuringBootstrap) {
            return false;
        }
    }
}
