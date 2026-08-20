package cloud.thehsi.ReactionRoles.Utils;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ConfigUtils {

    private ConfigUtils() {
        // Utility class
    }

    public static Properties loadOrCreateConfig(Path path, Logger logger) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not create plugin config directory.", ex);
        }

        Properties config = new Properties();

        if (!Files.exists(path)) {
            config.setProperty("guild-id", "123456789012345678");
            config.setProperty("channel-id", "123456789012345678");
            config.setProperty(
                    "message-content",
                    "React below to choose your role!\\n\\n👍 — Member"
            );
            config.setProperty("role.👍", "111111111111111111");

            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                config.store(writer, "ReactionRolePlugin configuration");
                if (logger != null) {
                    logger.info("Created default configuration at {}.", path);
                }
            } catch (IOException ex) {
                throw new IllegalStateException("Could not create " + path, ex);
            }
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            config.load(reader);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read " + path, ex);
        }

        return config;
    }

    public static long getRequiredLong(Properties config, String key) {
        String value = config.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing configuration key: " + key);
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalStateException(
                    "Configuration key '" + key + "' is not a valid Discord ID: " + value,
                    ex
            );
        }
    }
}