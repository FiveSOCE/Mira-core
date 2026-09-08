package com.mira.core.service;

import com.mira.core.MiraCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.PermissionNode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;
import java.util.Locale;

public final class CorePlayerJoinService implements Listener {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final MiraCorePlugin plugin;
    private final LuckPerms luckPerms;

    public CorePlayerJoinService(MiraCorePlugin plugin) {
        this.plugin = plugin;
        LuckPerms resolved = null;
        if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            try { resolved = LuckPermsProvider.get(); }
            catch (IllegalStateException ignored) { }
        }
        this.luckPerms = resolved;
    }

    public void syncStartingGroupAccess() {
        if (luckPerms == null) {
            plugin.getLogger().warning("LuckPerms is unavailable; starting-rank starter permissions were not synchronized.");
            return;
        }

        String groupName = plugin.getConfig().getString("starter.starting-group", "default");
        if (groupName == null || groupName.isBlank()) groupName = "default";

        try {
            luckPerms.getGroupManager().loadAllGroups().join();
            Group group = luckPerms.getGroupManager().getGroup(groupName);
            if (group == null) {
                plugin.getLogger().warning("Starter starting-group '" + groupName + "' does not exist in LuckPerms.");
                return;
            }

            List<String> permissions = plugin.getConfig().getStringList("starter.starting-group-permissions");
            if (permissions.isEmpty()) permissions = List.of("Mirakits.starter", "miracore.guides");

            boolean changed = false;
            for (String raw : permissions) {
                String permission = raw == null ? "" : raw.trim();
                if (permission.isBlank()) continue;
                var result = group.data().add(PermissionNode.builder(permission).value(true).build());
                if (result.wasSuccessful()) changed = true;
            }

            if (changed) luckPerms.getGroupManager().saveGroup(group).join();
            plugin.getLogger().info("Starting group '" + groupName + "' has starter access: " + permissions);
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Could not synchronize starter permissions to LuckPerms: " + ex.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("join-message.enabled", true)) return;

        // MiraCore is the sole join-message owner.
        event.joinMessage(null);

        Player player = event.getPlayer();
        Component line = LEGACY.deserialize(normalizeLegacy(render(player)));
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) Bukkit.broadcast(line);
        });
    }

    private String render(Player player) {
        String format = plugin.getConfig().getString("join-message.format", "%rank% &f%player% Has Joined.");
        if (format == null || format.isBlank()) format = "%rank% &f%player% Has Joined.";

        return format
                .replace("%rank%", rank(player))
                .replace("%player%", player.getName())
                .replace("%username%", player.getName());
    }

    private String rank(Player player) {
        if (luckPerms == null) return "&7[Player]";

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            try { user = luckPerms.getUserManager().loadUser(player.getUniqueId()).join(); }
            catch (RuntimeException ignored) { return "&7[Player]"; }
        }

        String prefix = user.getCachedData().getMetaData().getPrefix();
        if (prefix != null && !prefix.isBlank()) return prefix.trim();

        String primary = user.getPrimaryGroup();
        if (primary == null || primary.isBlank()) primary = "Player";
        return "&7[" + pretty(primary) + "]";
    }

    private String pretty(String value) {
        String[] parts = value.replace('_', ' ').replace('-', ' ').trim().split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return out.isEmpty() ? "Player" : out.toString();
    }

    private String normalizeLegacy(String value) {
        return value == null ? "" : value.replace('§', '&');
    }
}
