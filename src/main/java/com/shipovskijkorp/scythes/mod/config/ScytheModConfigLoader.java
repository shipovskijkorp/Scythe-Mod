package com.shipovskijkorp.scythes.mod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.shipovskijkorp.scythes.mod.ScytheMod;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ScytheModConfigLoader {

    public static ScytheModConfig CONFIG;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static ScytheModConfig getConfig() {
        if (CONFIG == null) {
            CONFIG = new ScytheModConfig();
            sanitize(CONFIG);
        }
        return CONFIG;
    }

    public static void load() {
        Path configPath = null;

        try {
            configPath = FabricLoader.getInstance()
                    .getConfigDir()
                    .resolve("scythes.json");

            if (!Files.exists(configPath)) {
                CONFIG = new ScytheModConfig();
                sanitize(CONFIG);
                write(configPath, CONFIG);
                ScytheMod.LOGGER.info("Created default config: {}", configPath.getFileName());
                return;
            }

            String rawJson = Files.readString(configPath, StandardCharsets.UTF_8);
            JsonObject rawObject = null;
            try {
                rawObject = GSON.fromJson(rawJson, JsonObject.class);
            } catch (Exception ignored) {
                // The typed parse below will throw and fall into the outer catch with a useful log.
            }

            CONFIG = GSON.fromJson(rawJson, ScytheModConfig.class);
            if (CONFIG == null) {
                CONFIG = new ScytheModConfig();
            }

            migrate(CONFIG, rawObject);
            sanitize(CONFIG);
            write(configPath, CONFIG);

        } catch (Exception e) {
            CONFIG = new ScytheModConfig();
            sanitize(CONFIG);
            ScytheMod.LOGGER.error("Failed to load config (using defaults){}",
                    configPath != null ? (": " + configPath.getFileName()) : "",
                    e);
        }
    }

    private static void write(Path configPath, ScytheModConfig config) throws Exception {
        Files.createDirectories(configPath.getParent());
        Files.writeString(configPath, GSON.toJson(config), StandardCharsets.UTF_8);
    }

    private static void migrate(ScytheModConfig config, JsonObject rawObject) {
        if (rawObject == null) return;

        if (rawObject.has("bloodHarvestDurabilityCost") && !rawObject.has("scytheAbilityDurabilityCost")) {
            try {
                config.scytheAbilityDurabilityCost = rawObject.get("bloodHarvestDurabilityCost").getAsInt();
            } catch (Exception ignored) {
                // sanitize() will keep the default if the legacy value is invalid.
            }
        }
    }

    private static void sanitize(ScytheModConfig config) {
        config.bloodBleedingChance = clamp(config.bloodBleedingChance, 0.0, 1.0);
        config.bloodBleedingBaseDurationTicks = clampMin(config.bloodBleedingBaseDurationTicks, 1);
        config.bloodBleedingExtendTicks = clampMin(config.bloodBleedingExtendTicks, 1);

        config.bleedingTickRate = clampMin(config.bleedingTickRate, 1);
        config.bleedingDamagePerSecond = clampMin(config.bleedingDamagePerSecond, 0.0);
        config.bloodVampirismChance = clamp(config.bloodVampirismChance, 0.0, 1.0);
        config.bloodVampirismHealFraction = clampMin(config.bloodVampirismHealFraction, 0.0);
        config.bloodVampirismCooldownTicks = clampMin(config.bloodVampirismCooldownTicks, 0);

        config.bloodHarvestRadius = clampMin(config.bloodHarvestRadius, 0.0);
        config.bloodHarvestCooldownTicks = clampMin(config.bloodHarvestCooldownTicks, 0);
        config.bloodHarvestKillWindowTicks = clampMin(config.bloodHarvestKillWindowTicks, 1);
        config.scytheAbilityDurabilityCost = clampMin(config.scytheAbilityDurabilityCost, 0);
        config.bloodHarvestSlownessTicks = clampMin(config.bloodHarvestSlownessTicks, 1);
        config.bloodHarvestBlindnessTicks = clampMin(config.bloodHarvestBlindnessTicks, 1);
        config.bloodHarvestWeaknessTicks = clampMin(config.bloodHarvestWeaknessTicks, 1);
        config.bloodHarvestGlowingTicks = clampMin(config.bloodHarvestGlowingTicks, 1);
        config.bloodHarvestSlownessAmplifier = clampMin(config.bloodHarvestSlownessAmplifier, 0);
        config.bloodHarvestWeaknessAmplifier = clampMin(config.bloodHarvestWeaknessAmplifier, 0);
        config.bloodHarvestSuccessBuffTicks = clampMin(config.bloodHarvestSuccessBuffTicks, 1);
        config.bloodHarvestSuccessSpeedAmplifier = clampMin(config.bloodHarvestSuccessSpeedAmplifier, 0);
        config.bloodHarvestSuccessStrengthAmplifier = clampMin(config.bloodHarvestSuccessStrengthAmplifier, 0);
        config.bloodHarvestSuccessRegenAmplifier = clampMin(config.bloodHarvestSuccessRegenAmplifier, 0);
        config.bloodHarvestFailureDebuffTicks = clampMin(config.bloodHarvestFailureDebuffTicks, 1);
        config.bloodHarvestFailureSlownessAmplifier = clampMin(config.bloodHarvestFailureSlownessAmplifier, 0);
        config.bloodHarvestFailureWeaknessAmplifier = clampMin(config.bloodHarvestFailureWeaknessAmplifier, 0);

        config.plagueAuraRadius = clampMin(config.plagueAuraRadius, 0.0);
        config.plagueAuraTickRate = clampMin(config.plagueAuraTickRate, 1);
        config.plagueAuraEffectTicks = clampMin(config.plagueAuraEffectTicks, 1);
        config.plagueAuraRefreshThresholdTicks = clampMin(config.plagueAuraRefreshThresholdTicks, 1);
        config.plagueActiveRadius = clampMin(config.plagueActiveRadius, 0.0);
        config.plagueActiveTicks = clampMin(config.plagueActiveTicks, 1);
        config.plagueActiveTickRate = clampMin(config.plagueActiveTickRate, 1);
        config.plagueActiveDamage = clampMin(config.plagueActiveDamage, 0.0);
        config.plagueActiveDebuffTicks = clampMin(config.plagueActiveDebuffTicks, 1);
        config.plagueActiveSlownessAmplifier = clampMin(config.plagueActiveSlownessAmplifier, 0);
        config.plagueActiveWeaknessAmplifier = clampMin(config.plagueActiveWeaknessAmplifier, 0);
        config.plagueActiveCooldownTicks = clampMin(config.plagueActiveCooldownTicks, 0);
        config.plagueMissingHealthBaseDamage = clampMin(config.plagueMissingHealthBaseDamage, 0.0);
        config.plagueMissingHealthMultiplierCap = clampMin(config.plagueMissingHealthMultiplierCap, 1.0);

        config.witheringAuraRadius = clampMin(config.witheringAuraRadius, 0.0);
        config.witheringAuraTickRate = clampMin(config.witheringAuraTickRate, 1);
        config.witheringAuraWitherTicks = clampMin(config.witheringAuraWitherTicks, 1);
        config.witheringAuraSlownessTicks = clampMin(config.witheringAuraSlownessTicks, 1);
        config.witheringAuraRefreshThresholdTicks = clampMin(config.witheringAuraRefreshThresholdTicks, 1);
        config.witheringActiveRadius = clampMin(config.witheringActiveRadius, 0.0);
        config.witheringActiveTicks = clampMin(config.witheringActiveTicks, 1);
        config.witheringActiveTickRate = clampMin(config.witheringActiveTickRate, 1);
        config.witheringActiveDamage = clampMin(config.witheringActiveDamage, 0.0);
        config.witheringActiveCooldownTicks = clampMin(config.witheringActiveCooldownTicks, 0);
        config.witheringDebuffTicks = clampMin(config.witheringDebuffTicks, 1);
        config.witheringDebuffSlownessAmplifier = clampMin(config.witheringDebuffSlownessAmplifier, 0);
        config.witheringDebuffWitherAmplifier = clampMin(config.witheringDebuffWitherAmplifier, 0);
        config.bloodyEssenceVillagerDropChance = clamp(config.bloodyEssenceVillagerDropChance, 0.0, 1.0);
        config.bloodyEssencePlayerDropChance = clamp(config.bloodyEssencePlayerDropChance, 0.0, 1.0);
        config.witheringArmorIgnoreFraction = clamp(config.witheringArmorIgnoreFraction, 0.0, 1.0);
        config.witheringArmorIgnoreBaseDamage = clampMin(config.witheringArmorIgnoreBaseDamage, 0.0);
    }

    private static int clampMin(int value, int min) {
        return value < min ? min : value;
    }

    private static double clampMin(double value, double min) {
        return value < min ? min : value;
    }

    private static double clamp(double value, double min, double max) {
        return Math.min(max, Math.max(min, value));
    }
}
