package dev.jsco.obsidian;

import com.google.common.base.Throwables;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class ObsidianConfig {

    private static File CONFIG_FILE;
    public static YamlConfiguration config;
    static int version;

    public static void init() {
        CONFIG_FILE = new File("obsidian.yml");
        config = new YamlConfiguration();

        try {
            if (!CONFIG_FILE.exists()) {
                CONFIG_FILE.createNewFile();
                config.load(CONFIG_FILE);
            } else {
                config.load(CONFIG_FILE);
            }
        } catch (IOException ignored) {
        } catch (InvalidConfigurationException ex) {
            ObsidianLogger.LOGGER.log(Level.SEVERE, "Failed to load obsidian config, please check syntax for errors.", ex);
        }

        config.options().copyDefaults(true);
        readConfig(ObsidianConfig.class, null);
    }

    public static void readConfig(Class<?> clazz, Object instance) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (Modifier.isPrivate(method.getModifiers())) {
                if (method.getParameterTypes().length == 0 && method.getReturnType() == Void.TYPE) {
                    try {
                        method.setAccessible(true);
                        method.invoke(instance);
                    } catch (InvocationTargetException ex) {
                        throw Throwables.propagate(ex.getCause());
                    } catch (Exception ex) {
                        ObsidianLogger.LOGGER.log(Level.SEVERE, "Error invoking " + method, ex);
                    }
                }
            }
        }

        try {
            config.save(CONFIG_FILE);
        } catch (IOException ex) {
            ObsidianLogger.LOGGER.log(Level.SEVERE, "Could not save " + CONFIG_FILE, ex);
        }
    }

    private static void set(String path, Object val) {
        config.set(path, val);
    }

    private static int getInt(String path, int def) {
        config.addDefault(path, def);
        return config.getInt(path, config.getInt(path));
    }

    public static List<Long> failedRconAttempts = new ArrayList<>();
    public static int maxRconAttempts;
    private static void maxRconAttempts() {
        maxRconAttempts = getInt("settings.max-rcon-attempts", 3);
    }

    public static int rconTimeWindow;
    private static void rconTimeWindow() {
        rconTimeWindow = getInt("settings.rcon-time-window-minutes", 5);
    }

}
