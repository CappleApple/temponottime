package com.cappleapple.temponottime.config;

import com.cappleapple.temponottime.TempoNotTime;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class SpellOverrideManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FMLPaths.CONFIGDIR.get().resolve(TempoNotTime.MOD_ID + "-spell-overrides.json");
    private static volatile Map<ResourceLocation, SpellOverride> overrides = Map.of();

    private SpellOverrideManager() {
    }

    public static SpellOverride get(String spellId) {
        ResourceLocation id = ResourceLocation.tryParse(spellId);
        return id == null ? SpellOverride.DEFAULT : overrides.getOrDefault(id, SpellOverride.DEFAULT);
    }

    public static int size() {
        return overrides.size();
    }

    public static synchronized void reload() {
        try {
            if (Files.notExists(PATH)) {
                Files.createDirectories(PATH.getParent());
                Files.writeString(PATH, GSON.toJson(new JsonObject()) + System.lineSeparator());
            }
            Map<ResourceLocation, SpellOverride> loaded = new HashMap<>();
            try (Reader reader = Files.newBufferedReader(PATH)) {
                JsonElement rootElement = JsonParser.parseReader(reader);
                if (!rootElement.isJsonObject()) {
                    throw new IllegalArgumentException("Override root must be a JSON object");
                }
                for (Map.Entry<String, JsonElement> entry : rootElement.getAsJsonObject().entrySet()) {
                    ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
                    if (id == null || !entry.getValue().isJsonObject()) {
                        TempoNotTime.LOGGER.warn("Ignoring malformed spell override entry '{}'", entry.getKey());
                        continue;
                    }
                    SpellOverride parsed = parse(entry.getValue().getAsJsonObject());
                    loaded.put(id, parsed);
                }
            }
            overrides = Map.copyOf(loaded);
            TempoNotTime.LOGGER.info("Loaded {} Tempo Not Time spell override(s) from {}", overrides.size(), PATH);
        } catch (IOException | RuntimeException exception) {
            TempoNotTime.LOGGER.error("Failed to load {}; retaining the last valid override set", PATH, exception);
        }
    }

    private static SpellOverride parse(JsonObject object) {
        Double cost = optionalFiniteDouble(object, "casting_draw", 0.0, 1_000_000.0);
        Integer charges = optionalInt(object, "max_charges", 1, 10_000);
        Double cooldown = optionalFiniteDouble(object, "cooldown_multiplier", 0.0001, 1000.0);
        Boolean chargesAllowed = optionalBoolean(object, "charges_allowed");
        Boolean reserves = optionalBoolean(object, "casting_reserve_applies");
        Boolean load = optionalBoolean(object, "load_scaling_applies");
        return new SpellOverride(cost, charges, cooldown == null ? 1.0 : cooldown, chargesAllowed, reserves, load);
    }

    private static Double optionalFiniteDouble(JsonObject object, String key, double minimum, double maximum) {
        if (!object.has(key)) return null;
        double value = object.get(key).getAsDouble();
        if (!Double.isFinite(value) || value < minimum || value > maximum) {
            throw new IllegalArgumentException(key + " is outside its valid range");
        }
        return value;
    }

    private static Integer optionalInt(JsonObject object, String key, int minimum, int maximum) {
        if (!object.has(key)) return null;
        int value = object.get(key).getAsInt();
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(key + " is outside its valid range");
        }
        return value;
    }

    private static Boolean optionalBoolean(JsonObject object, String key) {
        return object.has(key) ? object.get(key).getAsBoolean() : null;
    }
}
