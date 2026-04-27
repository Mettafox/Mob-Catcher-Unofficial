package tfar.mobcatcher.config;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class ServerConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("MobCatcher");
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
        if (Files.exists(CONFIG_PATH)) {
            load();
        } else {
            writeDefaults();
        }
    }

    private static void load() {
        var props = new Properties();
        try (var in = Files.newInputStream(CONFIG_PATH)) {
            props.load(in);
            entityBlacklist = Arrays.asList(
                    props.getProperty("entityBlacklist", "corpse:corpse,minecraft:wither,minecraft:ender_dragon").split(","));
            launcherVelocityMultiplier = Double.parseDouble(props.getProperty("launcherVelocityMultiplier", "1.5"));
            dispenserVelocity = Double.parseDouble(props.getProperty("dispenserVelocity", "1.1"));
            dispenserInaccuracy = Double.parseDouble(props.getProperty("dispenserInaccuracy", "4.0"));
        } catch (IOException | NumberFormatException e) {
            LOGGER.error("Failed to load config, using defaults", e);
        }
    }

    private static void writeDefaults() {
        var props = new Properties();
        props.setProperty("entityBlacklist", "corpse:corpse,minecraft:wither,minecraft:ender_dragon");
        props.setProperty("launcherVelocityMultiplier", "1.5");
        props.setProperty("dispenserVelocity", "1.1");
        props.setProperty("dispenserInaccuracy", "4.0");
        try (var out = Files.newOutputStream(CONFIG_PATH)) {
            props.store(out,
                    """
                    MobCatcher Configuration
                    entityBlacklist            — Comma-separated list of entity IDs to blacklist from capture
                    launcherVelocityMultiplier — Velocity multiplier of net shot by NetLauncher (0-10)
                    dispenserVelocity          — Velocity of net shot by Dispenser (0-255)
                    dispenserInaccuracy        — Inaccuracy of net shot by Dispenser (0-10)""");
        } catch (IOException e) {
            LOGGER.error("Failed to write default config", e);
        }
    }
}
