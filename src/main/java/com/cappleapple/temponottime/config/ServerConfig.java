package com.cappleapple.temponottime.config;

import com.cappleapple.temponottime.casting.ChargeRequirementFormula;
import com.cappleapple.temponottime.casting.TimingNormalization;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class ServerConfig {
    public enum CastingMode {
        CASTING_RESERVE,
        SPELL_COOLDOWNS
    }

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
    public static final ModConfigSpec.EnumValue<CastingMode> CASTING_MODE;
    public static final ModConfigSpec.BooleanValue CONVERT_MAX_MANA;
    public static final ModConfigSpec.BooleanValue CONVERT_MANA_REGEN;
    public static final ModConfigSpec.BooleanValue DISABLE_MANA_CONSUMPTION;
    public static final ModConfigSpec.EnumValue<DeathCooldownBehavior> DEATH_BEHAVIOR;
    public static final ModConfigSpec.BooleanValue CREATIVE_BYPASSES_CAPACITY;
    public static final ModConfigSpec.BooleanValue CREATIVE_BYPASSES_CHARGES;

    public static final ModConfigSpec.BooleanValue CAPACITY_ENABLED;
    public static final ModConfigSpec.BooleanValue PRORATED_MANA_REGEN;
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

    public static final ModConfigSpec.BooleanValue CHARGE_CAST_DELAY_ENABLED;
    public static final ModConfigSpec.DoubleValue MINIMUM_CHARGE_CAST_DELAY;
    public static final ModConfigSpec.DoubleValue MAXIMUM_CHARGE_CAST_DELAY;
    public static final ModConfigSpec.DoubleValue CHARGE_CAST_DELAY_PERCENTAGE;
    public static final ModConfigSpec.DoubleValue CHARGE_CAST_DELAY_FLAT_BASE;

    public static final ModConfigSpec.BooleanValue LOAD_ENABLED;
    public static final ModConfigSpec.BooleanValue SHARED_COOLDOWN_LOAD;
    public static final ModConfigSpec.IntValue FREE_COOLDOWNS;
    public static final ModConfigSpec.DoubleValue PENALTY_PER_ADDITIONAL_COOLDOWN;
    public static final ModConfigSpec.DoubleValue MINIMUM_RECOVERY_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue COUNT_PER_CHARGE;

    public static final ModConfigSpec.DoubleValue MANA_REGEN_TO_RECOVERY_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue MAXIMUM_TOTAL_RECOVERY_MULTIPLIER;

    public static final ModConfigSpec.BooleanValue RECHARGE_NORMALIZATION_ENABLED;
    public static final ModConfigSpec.DoubleValue RECHARGE_FLAT_MODIFIER;
    public static final ModConfigSpec.DoubleValue NORMAL_RECHARGE_SECONDS;
    public static final ModConfigSpec.DoubleValue SHORT_RECHARGE_STRENGTH;
    public static final ModConfigSpec.DoubleValue LONG_RECHARGE_STRENGTH;
    public static final ModConfigSpec.DoubleValue NORMALIZATION_SPREAD;

    public static final ModConfigSpec.BooleanValue CAST_TIME_NORMALIZATION_ENABLED;
    public static final ModConfigSpec.DoubleValue CAST_TIME_FLAT_MODIFIER;
    public static final ModConfigSpec.BooleanValue AFFECT_CUSTOM_CAST_TIMES;
    public static final ModConfigSpec.BooleanValue AFFECT_NO_CAST_TIME_SPELLS;
    public static final ModConfigSpec.DoubleValue NORMAL_CAST_SECONDS;
    public static final ModConfigSpec.DoubleValue SHORT_CAST_STRENGTH;
    public static final ModConfigSpec.DoubleValue LONG_CAST_STRENGTH;
    public static final ModConfigSpec.DoubleValue CAST_NORMALIZATION_SPREAD;

    public static final ModConfigSpec.BooleanValue DEBUG_LOGGING;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("general");
        ENABLED = builder.comment("Enable Tempo Not Time feature true/false.")
                .define("enabled", true);
        CASTING_MODE = builder.comment(
                        "CASTING_RESERVE keeps the shared reserve system.",
                        "SPELL_COOLDOWNS uses independent spell charges and recharge timers, hides the mana bar,",
                        "and disables mana spending and shared reserve limits regardless of their toggles.",
                        "Max Mana and Mana Regeneration conversion toggles still control charge scaling and recovery speed.",
                        "Instant Mana advances each spell's recharge by restored mana / that cast's mana cost.")
                .defineEnum("casting_mode", CastingMode.CASTING_RESERVE);
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
        PRORATED_MANA_REGEN = builder.comment(
                        "Return occupied Casting Reserve in proportion to charge cooldown progress every 10 ticks.",
                        "Only applies in CASTING_RESERVE mode. False keeps the full cost occupied until recharge completes.",
                        "Pending casts and charges that have not started recovering keep their full cost.")
                .define("prorated_mana_regen", true);
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

        builder.push("delay_between_charge_casts");
        CHARGE_CAST_DELAY_ENABLED = builder.comment("Require a short delay before casting the same spell again after a successful cast.",
                        "Other spells and native follow-up recasts remain available. The timer pauses while that spell is still casting.")
                .define("enabled", true);
        MINIMUM_CHARGE_CAST_DELAY = builder.comment("Minimum delay in seconds.")
                .defineInRange("min_delay_seconds", 0.1, 0.0, 86_400.0);
        MAXIMUM_CHARGE_CAST_DELAY = builder.comment("Maximum delay in seconds. If below the minimum, the minimum takes precedence.")
                .defineInRange("max_delay_seconds", 10.0, 0.0, 86_400.0);
        CHARGE_CAST_DELAY_PERCENTAGE = builder.comment("Percentage of the actual cast duration added to the delay; 10 means 10%.")
                .defineInRange("delay_modifier_percentage", 10.0, 0.0, 10_000.0);
        CHARGE_CAST_DELAY_FLAT_BASE = builder.comment("Signed seconds added before the cast-time percentage and min/max clamp.",
                        "Delay = clamp(flat_base_modifier + cast_seconds * delay_modifier_percentage / 100, min, max), rounded up to a tick.")
                .defineInRange("flat_base_modifier", 0.5, -86_400.0, 86_400.0);
        builder.pop();

        builder.push("cooldown_load");
        LOAD_ENABLED = builder.comment("Enable cooldown load feature true/false.")
                .define("enabled", false);
        SHARED_COOLDOWN_LOAD = builder.comment(
                        "False applies load independently to each spell, counting that spell's spent/pending charges.",
                        "True uses one shared cooldown load for all spells. Applies in both casting modes when load is enabled.")
                .define("shared_cooldown_load", false);
        FREE_COOLDOWNS = builder.comment("Number of active cooldowns before load penalties begin.")
                .defineInRange("free_cooldowns", 1, 0, 10_000);
        PENALTY_PER_ADDITIONAL_COOLDOWN = builder.comment("Penalty in 1 / (1 + penalty * additional cooldowns).")
                .defineInRange("penalty_per_additional_cooldown", 0.20, 0.0, 1000.0);
        MINIMUM_RECOVERY_MULTIPLIER = builder.comment("Floor preventing load from freezing Casting Recovery.")
                .defineInRange("minimum_recovery_multiplier", 0.25, 0.0001, 1.0);
        COUNT_PER_CHARGE = builder.comment("For shared load only: count each spent charge (true) or each distinct spell (false). Per-spell load always counts charges.")
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
        RECHARGE_FLAT_MODIFIER = builder.comment(
                        "Seconds added to base cooldowns before normalization and cooldown modifiers; negative values shorten them.",
                        "Applied even when the normalization curve is disabled. Resulting durations have a one-tick minimum.")
                .defineInRange("flat_modifer", 0.0, -86_400.0, 86_400.0);
        NORMAL_RECHARGE_SECONDS = builder.comment("Recharge duration considered normal and used as the center of the curve.")
                .defineInRange("normal_recharge_seconds", 10.0, 0.05, 86_400.0);
        SHORT_RECHARGE_STRENGTH = builder.comment("How aggressively shorter recharges are compressed toward normal. Zero leaves them unchanged.")
                .defineInRange("short_recharge_strength", 0.8, 0.0, 1000.0);
        LONG_RECHARGE_STRENGTH = builder.comment("How aggressively longer recharges are compressed toward normal. Zero leaves them unchanged.")
                .defineInRange("long_recharge_strength", 0.5, 0.0, 1000.0);
        NORMALIZATION_SPREAD = builder.comment("Breadth of the relatively unmodified area around normal recharge.")
                .defineInRange("normalization_spread", 8.0, 0.001, 86_400.0);
        builder.pop();

        builder.push("cast_time_normalization");
        CAST_TIME_NORMALIZATION_ENABLED = builder.comment(
                        "Enable the cast-time normalization curve for player spells.",
                        "For continuous spells, cast time is channel duration. Mob casting is unchanged.")
                .define("enabled", false);
        CAST_TIME_FLAT_MODIFIER = builder.comment(
                        "Seconds added to eligible base cast times before normalization and cast-speed modifiers.",
                        "Applied even when the curve is disabled. Negative values shorten casts; minimum one tick.")
                .defineInRange("flat_modifer", 0.0, -86_400.0, 86_400.0);
        AFFECT_CUSTOM_CAST_TIMES = builder.comment(
                        "Apply cast-time adjustments to spells overriding getCastTime or getEffectiveCastTime, including inherited overrides.",
                        "False preserves custom and animation-bound timing. Applies to the flat modifier and curve.")
                .define("affect_custom_cast_times", false);
        AFFECT_NO_CAST_TIME_SPELLS = builder.comment(
                        "Allow the flat modifier and curve to give zero-duration spells a cast delay. False keeps them instant.",
                        "Uses a zero base and no cast-speed multiplier. Custom zero-duration spells also require affect_custom_cast_times.")
                .define("affect_no_cast_time_spells", false);
        NORMAL_CAST_SECONDS = builder.comment("Cast time at the center of the normalization curve, in seconds.")
                .defineInRange("normal_cast_seconds", 1.0, 0.05, 86_400.0);
        SHORT_CAST_STRENGTH = builder.comment("Compression strength for shorter casts. Zero leaves them unchanged.")
                .defineInRange("short_cast_strength", 0.8, 0.0, 1000.0);
        LONG_CAST_STRENGTH = builder.comment("Compression strength for longer casts. Zero leaves them unchanged.")
                .defineInRange("long_cast_strength", 0.5, 0.0, 1000.0);
        CAST_NORMALIZATION_SPREAD = builder.comment("Breadth in seconds of the relatively unmodified area around normal cast time.")
                .defineInRange("normalization_spread", 0.8, 0.001, 86_400.0);
        builder.pop();

        builder.push("diagnostics");
        DEBUG_LOGGING = builder.comment("Enable diagnostic logging feature true/false.")
                .define("debug_logging", false);
        builder.pop();

        SPEC = builder.build();
    }

    private ServerConfig() {
    }

    public static TimingNormalization rechargeNormalization() {
        return new TimingNormalization(RECHARGE_NORMALIZATION_ENABLED.get(), RECHARGE_FLAT_MODIFIER.get(),
                NORMAL_RECHARGE_SECONDS.get(), SHORT_RECHARGE_STRENGTH.get(), LONG_RECHARGE_STRENGTH.get(), NORMALIZATION_SPREAD.get());
    }

    public static TimingNormalization castTimeNormalization() {
        return new TimingNormalization(CAST_TIME_NORMALIZATION_ENABLED.get(), CAST_TIME_FLAT_MODIFIER.get(),
                NORMAL_CAST_SECONDS.get(), SHORT_CAST_STRENGTH.get(), LONG_CAST_STRENGTH.get(), CAST_NORMALIZATION_SPREAD.get(),
                AFFECT_CUSTOM_CAST_TIMES.get(), AFFECT_NO_CAST_TIME_SPELLS.get());
    }

    public static boolean spellCooldownsOnly() {
        return enabled() && CASTING_MODE.get() == CastingMode.SPELL_COOLDOWNS;
    }

    public static boolean manaDisabled() {
        return enabled() && (spellCooldownsOnly() || DISABLE_MANA_CONSUMPTION.get());
    }

    public static boolean proratedManaRegen() {
        return enabled() && !spellCooldownsOnly() && CAPACITY_ENABLED.get() && PRORATED_MANA_REGEN.get();
    }

    public static boolean capacityEnabled() {
        return !spellCooldownsOnly() && CAPACITY_ENABLED.get();
    }

    public static boolean enabled() {
        try {
            return ENABLED.get();
        } catch (IllegalStateException ignoredDuringBootstrap) {
            return false;
        }
    }
}
