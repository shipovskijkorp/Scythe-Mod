package com.example.bloodyscythe.config;

import com.example.bloodyscythe.BleedingMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class BloodyScytheConfigLoader {

    public static BloodyScytheConfig CONFIG;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void load() {
        File configFile = null;

        try {
            configFile = FabricLoader.getInstance()
                    .getConfigDir()
                    .resolve("bloodyscythe.json")
                    .toFile();

            if (!configFile.exists()) {
                CONFIG = new BloodyScytheConfig();
                try (FileWriter writer = new FileWriter(configFile)) {
                    GSON.toJson(CONFIG, writer);
                }
                BleedingMod.LOGGER.info("Created default config: {}", configFile.getName());
                return;
            }

            try (FileReader reader = new FileReader(configFile)) {
                CONFIG = GSON.fromJson(reader, BloodyScytheConfig.class);
            }

            if (CONFIG == null) CONFIG = new BloodyScytheConfig();

        } catch (Exception e) {
            CONFIG = new BloodyScytheConfig();
            BleedingMod.LOGGER.error("Failed to load config (using defaults){}", configFile != null ? (": " + configFile.getName()) : "", e);
        }
    }
}
