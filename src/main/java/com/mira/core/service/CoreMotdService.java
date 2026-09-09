package com.mira.core.service;

import com.mira.core.api.MaintenanceService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.CachedServerIcon;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public final class CoreMotdService {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final JavaPlugin plugin;
    private final MaintenanceService maintenance;
    private CachedServerIcon icon;

    public CoreMotdService(JavaPlugin plugin, MaintenanceService maintenance) {
        this.plugin = plugin;
        this.maintenance = maintenance;
        reload();
    }

    public void reload() {
        icon = loadIcon();
    }

    public void apply(ServerListPingEvent event) {
        List<String> lines = maintenance.enabled()
                ? plugin.getConfig().getStringList("motd.maintenance.lines")
                : plugin.getConfig().getStringList("motd.normal.lines");
        if (lines.isEmpty()) {
            lines = List.of(maintenance.enabled()
                    ? "&5&lMira &8- &cMaintenance"
                    : "&5&lMira");
        }

        String reason = maintenance.reason().orElse(
                plugin.getConfig().getString("maintenance.default-reason", "Server maintenance"));
        String end = maintenance.scheduledEnd()
                .map(value -> value.toString())
                .orElse("");

        String rendered = lines.stream()
                .limit(2)
                .map(line -> placeholders(line, reason, end))
                .map(this::legacy)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");

        event.setMotd(rendered);

        if (plugin.getConfig().getBoolean("motd.max-players.modify", false)) {
            event.setMaxPlayers(Math.max(0,
                    plugin.getConfig().getInt("motd.max-players.value", Bukkit.getMaxPlayers())));
        }

        if (icon != null) {
            try {
                event.setServerIcon(icon);
            } catch (RuntimeException exception) {
                plugin.getLogger().fine("Could not apply configured server icon: " + exception.getMessage());
            }
        }
    }

    public List<Component> joinMessages() {
        // The dedicated join-message service owns player joins. Never emit the legacy
        // MOTD join/welcome line at the same time, even if an older preserved config
        // still has motd.join.enabled=true.
        if (plugin.getConfig().getBoolean("join-message.enabled", true)) return List.of();
        if (!plugin.getConfig().getBoolean("motd.join.enabled", false)) return List.of();
        return plugin.getConfig().getStringList("motd.join.messages").stream()
                .map(this::component)
                .toList();
    }

    private CachedServerIcon loadIcon() {
        if (!plugin.getConfig().getBoolean("motd.icon.enabled", false)) return null;

        String name = plugin.getConfig().getString("motd.icon.file", "server-icon.png");
        File file = new File(plugin.getDataFolder(), name == null || name.isBlank() ? "server-icon.png" : name);
        if (!file.isFile()) {
            plugin.getLogger().warning("MiraCore MOTD icon enabled but file was not found: " + file.getPath());
            return null;
        }

        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                plugin.getLogger().warning("MiraCore could not decode MOTD icon: " + file.getPath());
                return null;
            }
            return Bukkit.getServer().loadServerIcon(image);
        } catch (Exception exception) {
            plugin.getLogger().warning("MiraCore could not load MOTD icon: " + exception.getMessage());
            return null;
        }
    }

    private String placeholders(String raw, String reason, String end) {
        return (raw == null ? "" : raw)
                .replace("%reason%", reason == null ? "" : reason)
                .replace("%end%", end == null ? "" : end);
    }

    private String legacy(String raw) {
        return LegacyComponentSerializer.legacySection().serialize(component(raw));
    }

    private Component component(String raw) {
        return LEGACY.deserialize(raw == null ? "" : raw);
    }
}
