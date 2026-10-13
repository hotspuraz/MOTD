package evo.motd;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

/** Parsed view of config.yml. Immutable except for the maintenance flag, which {@link #setMaintenance} persists. */
public final class Settings {

    public record Motd(String line1, String line2) {}

    public enum CountMode { REAL, FIXED, OFFSET, ONLINE_PLUS }

    public enum RotationMode { FIXED, RANDOM, SEQUENTIAL }

    private final Path file;
    private final YamlConfigurationLoader loader;
    private ConfigurationNode root;

    public RotationMode rotationMode;
    public int rotationIntervalSeconds;
    public List<Motd> motds;
    public CountMode onlineMode;
    public int onlineValue;
    public CountMode maxMode;
    public int maxValue;
    public List<String> hover;
    public boolean hideHover;

    public boolean maintenance;
    public Motd maintenanceMotd;
    public String maintenanceVersion;
    public String bypassPermission;
    public List<String> allowedPlayers;
    public String kickMessage;
    public List<String> maintenanceHover;

    public Settings(Path dataDir) throws IOException {
        Files.createDirectories(dataDir);
        this.file = dataDir.resolve("config.yml");
        if (Files.notExists(file)) {
            try (InputStream in = Settings.class.getResourceAsStream("/config.yml")) {
                Files.copy(in, file);
            }
        }
        this.loader = YamlConfigurationLoader.builder().path(file).nodeStyle(NodeStyle.BLOCK).build();
        reload();
    }

    public void reload() throws IOException {
        root = loader.load();
        rotationMode = parseEnum(RotationMode.class, root.node("rotation", "mode").getString("random"), RotationMode.RANDOM);
        rotationIntervalSeconds = Math.max(1, root.node("rotation", "interval-seconds").getInt(30));

        motds = new ArrayList<>();
        for (ConfigurationNode n : root.node("motds").childrenList()) {
            motds.add(new Motd(n.node("line1").getString(""), n.node("line2").getString("")));
        }
        if (motds.isEmpty()) motds.add(new Motd("A Velocity Server", ""));

        onlineMode = parseEnum(CountMode.class, root.node("players", "online", "mode").getString("real"), CountMode.REAL);
        onlineValue = root.node("players", "online", "value").getInt();
        maxMode = parseEnum(CountMode.class, root.node("players", "max", "mode").getString("real"), CountMode.REAL);
        maxValue = root.node("players", "max", "value").getInt();
        hover = strings(root.node("players", "hover"));
        hideHover = root.node("players", "hide-hover").getBoolean(false);

        maintenance = root.node("maintenance", "enabled").getBoolean(false);
        maintenanceMotd = new Motd(root.node("maintenance", "motd", "line1").getString(""),
                root.node("maintenance", "motd", "line2").getString(""));
        maintenanceVersion = root.node("maintenance", "version-text").getString("<red>Maintenance");
        bypassPermission = root.node("maintenance", "bypass-permission").getString("motd.maintenance.bypass");
        allowedPlayers = strings(root.node("maintenance", "allowed-players"));
        kickMessage = root.node("maintenance", "kick-message").getString("<red>Maintenance");
        maintenanceHover = strings(root.node("maintenance", "hover"));
    }

    public void setMaintenance(boolean enabled) throws IOException {
        root.node("maintenance", "enabled").set(enabled);
        loader.save(root);
        this.maintenance = enabled;
    }

    public String message(String key) {
        return root.node("messages", key).getString("");
    }

    private static List<String> strings(ConfigurationNode node) {
        List<String> out = new ArrayList<>();
        for (ConfigurationNode n : node.childrenList()) out.add(n.getString(""));
        return out;
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, E fallback) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
