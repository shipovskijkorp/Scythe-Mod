package com.shipovskijkorp.scythes.mod.config;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

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
        File configFile = null;

        try {
            configFile = FabricLoader.getInstance()
                    .getConfigDir()
                    .resolve("scythes.json")
                    .toFile();

            if (!configFile.exists()) {
                CONFIG = new ScytheModConfig();
                sanitize(CONFIG);
                try (FileWriter writer = new FileWriter(configFile)) {
                    GSON.toJson(CONFIG, writer);
                }
                ScytheMod.LOGGER.info("Created default config: {}", configFile.getName());
                return;
            }

            try (FileReader reader = new FileReader(configFile)) {
                CONFIG = GSON.fromJson(reader, ScytheModConfig.class);
            }

            if (CONFIG == null) {
                CONFIG = new ScytheModConfig();
            }
            sanitize(CONFIG);

        } catch (Exception e) {
            CONFIG = new ScytheModConfig();
            sanitize(CONFIG);
            ScytheMod.LOGGER.error("Failed to load config (using defaults){}",
                    configFile != null ? (": " + configFile.getName()) : "",
                    e);
        }
    }

    private static void sanitize(ScytheModConfig config) {
        config.bloodBleedingChance = clamp(config.bloodBleedingChance, 0.0, 1.0);
        config.bloodBleedingBaseDurationTicks = clampMin(config.bloodBleedingBaseDurationTicks, 1);
        config.bloodBleedingExtendTicks = clampMin(config.bloodBleedingExtendTicks, 1);

        config.bleedingTickRate = clampMin(config.bleedingTickRate, 1);
        config.bleedingDamagePerSecond = clampMin(config.bleedingDamagePerSecond, 0.0);

        config.bloodHarvestRadius = clampMin(config.bloodHarvestRadius, 0.0);
        config.bloodHarvestCooldownTicks = clampMin(config.bloodHarvestCooldownTicks, 0);
        config.bloodHarvestKillWindowTicks = clampMin(config.bloodHarvestKillWindowTicks, 1);
        config.bloodHarvestDurabilityCost = clampMin(config.bloodHarvestDurabilityCost, 0);
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
