package tfar.mobcatcher.config;

import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class ServerConfig {

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("MobCatcher.properties");

    public static List<String> entityBlacklist = List.of(
            "corpse:corpse", "minecraft:wither", "minecraft:ender_dragon"
    );
    public static double launcherVelocityMultiplier = 1.5;
    public static double dispenserVelocity = 1.1;
    public static double dispenserInaccuracy = 4.0;

    public static void init() {
        File configFile = CONFIG_PATH.toFile();
        Properties props = new Properties();

        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
                String blacklistRaw = props.getProperty("entityBlacklist",
                        "corpse:corpse,minecraft:wither,minecraft:ender_dragon");
                entityBlacklist = Arrays.asList(blacklistRaw.split(","));

                launcherVelocityMultiplier = Double.parseDouble(
                        props.getProperty("launcherVelocityMultiplier", "1.5"));
                dispenserVelocity = Double.parseDouble(
                        props.getProperty("dispenserVelocity", "1.1"));
                dispenserInaccuracy = Double.parseDouble(
                        props.getProperty("dispenserInaccuracy", "4.0"));
            } catch (IOException | NumberFormatException e) {
                System.err.println("[MobCatcher] Failed to load config, using defaults: " + e.getMessage());
            }
        } else {
            // Write defaults
            props.setProperty("# entityBlacklist", "Comma-separated list of entity IDs to blacklist from capture");
            props.setProperty("entityBlacklist", "corpse:corpse,minecraft:wither,minecraft:ender_dragon");
            props.setProperty("# launcherVelocityMultiplier", "Velocity multiplier of NetEntity shot by NetLauncher (0-10)");
            props.setProperty("launcherVelocityMultiplier", "1.5");
            props.setProperty("# dispenserVelocity", "Velocity of NetEntity shot by Dispenser (0-255)");
            props.setProperty("dispenserVelocity", "1.1");
            props.setProperty("# dispenserInaccuracy", "Inaccuracy of NetEntity shot by Dispenser (0-10)");
            props.setProperty("dispenserInaccuracy", "4.0");
            try (FileOutputStream fos = new FileOutputStream(configFile)) {
                props.store(fos, "MobCatcher Configuration");
            } catch (IOException e) {
                System.err.println("[MobCatcher] Failed to write default config: " + e.getMessage());
            }
        }
    }
}
