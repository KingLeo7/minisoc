package minisoc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Global configuration manager for MiniSOC.
 * Loads tunable detection thresholds and cooldowns from minisoc.properties.
 */
public class Config {

    private static final String CONFIG_FILE = "minisoc.properties";
    private static final Properties props = new Properties();

    static {
        load();
    }

    public static synchronized void load() {
        Path p = Paths.get(CONFIG_FILE);
        if (Files.exists(p)) {
            try (InputStream in = Files.newInputStream(p)) {
                props.load(in);
            } catch (IOException ignored) {}
        }
    }

    public static synchronized void save() {
        Path p = Paths.get(CONFIG_FILE);
        try (OutputStream out = Files.newOutputStream(p)) {
            props.store(out, "MiniSOC Detection Thresholds & Settings");
        } catch (IOException ignored) {}
    }

    public static synchronized int getInt(String key, int defaultValue) {
        String val = props.getProperty(key);
        if (val != null) {
            try {
                return Integer.parseInt(val.trim());
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    public static synchronized long getLong(String key, long defaultValue) {
        String val = props.getProperty(key);
        if (val != null) {
            try {
                return Long.parseLong(val.trim());
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    public static synchronized String getString(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public static synchronized void setProperty(String key, String value) {
        props.setProperty(key, value);
    }
}

