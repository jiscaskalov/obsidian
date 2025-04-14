package dev.jsco.obsidian;

import org.bukkit.Bukkit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ObsidianLogger extends Logger {

    public static final ObsidianLogger LOGGER = new ObsidianLogger();

    private ObsidianLogger() {
        super("obsidian", null);
        setParent(Bukkit.getLogger());
        setLevel(Level.ALL);
    }

}
