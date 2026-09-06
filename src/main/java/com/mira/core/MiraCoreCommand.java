package com.mira.core;

import com.mira.core.api.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class MiraCoreCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of("status", "test", "reload", "why", "audit", "profiles", "maintenance", "updates", "essentials", "help");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter MAINTENANCE_TIME =
            DateTimeFormatter.ofPattern("dd MMM yyyy, h:mm a z").withZone(ZoneId.of("Australia/Brisbane"));
    private final MiraCorePlugin plugin;

    public MiraCoreCommand(MiraCorePlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("miracore.admin")) {
            plugin.messages().send(sender, Component.text("You do not have permission to use MiraCore admin commands.", NamedTextColor.RED));
            return true;
        }
        String sub = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "status" -> showStatus(sender);
            case "test" -> runTests(sender);
            case "reload" -> reload(sender);
            case "why" -> why(sender, args);
            case "audit" -> audit(sender, args);
            case "profiles" -> profiles(sender);
            case "maintenance" -> maintenance(sender, args);
            case "updates" -> updates(sender, args);
            case "essentials" -> essentials(sender, args);
            case "help" -> help(sender);
            default -> help(sender);
        };
    }

    private boolean showStatus(CommandSender sender) {
        Runtime runtime = Runtime.getRuntime();
        long used = (runtime.totalMemory() - runtime.freeMemory()) / 1024L / 1024L;
        long max = runtime.maxMemory() / 1024L / 1024L;
        plugin.messages().send(sender, Component.text("Mira Suite Dashboard", NamedTextColor.LIGHT_PURPLE));
        plugin.messages().send(sender, Component.text("Core v" + plugin.api().version() + " | Paper " + plugin.getServer().getMinecraftVersion(), NamedTextColor.GRAY));
        plugin.messages().send(sender, Component.text("Players " + Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers()
                + " | Cached profiles " + plugin.api().profiles().cached().size() + " | Memory " + used + "/" + max + " MB", NamedTextColor.AQUA));
        List<ModuleSnapshot> modules = plugin.api().modules().all();
        for (ModuleSnapshot module : modules) {
            Component line = Component.text(" • " + module.displayName() + " v" + module.version() + " ", NamedTextColor.GRAY)
                    .append(Component.text("[" + module.health().name() + "]", healthColor(module.health())));
            if (!module.detail().isBlank()) line = line.append(Component.text(" " + module.detail(), NamedTextColor.DARK_GRAY));
            plugin.messages().send(sender, line);
        }
        return true;
    }

    private boolean runTests(CommandSender sender) {
        DiagnosticReport report = plugin.api().runDiagnostics();
        plugin.messages().send(sender, Component.text("MiraCore self-test: " + report.passedCount() + "/" + report.checks().size() + " passed", report.passed() ? NamedTextColor.GREEN : NamedTextColor.RED));
        for (DiagnosticCheck check : report.checks()) {
            plugin.messages().send(sender, Component.text(check.passed() ? " ✔ " : " ✘ ", check.passed() ? NamedTextColor.GREEN : NamedTextColor.RED)
                    .append(Component.text(check.name(), NamedTextColor.WHITE))
                    .append(Component.text(" - " + check.detail(), NamedTextColor.GRAY)));
        }
        return true;
    }

    private boolean reload(CommandSender sender) {
        plugin.reloadCoreConfiguration();
        plugin.api().audit().record("MiraCore", "RELOAD", sender instanceof org.bukkit.entity.Player p ? p.getUniqueId() : null, sender.getName(), "MiraCore", "Reloaded MiraCore configuration.");
        plugin.messages().send(sender, Component.text("MiraCore configuration reloaded.", NamedTextColor.GREEN));
        return true;
    }

    private boolean why(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.messages().send(sender, Component.text("Usage: /miracore why <player> <permission>", NamedTextColor.RED));
            return true;
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(args[1]);
        CommandSender target = offline.getPlayer();
        if (target == null) {
            plugin.messages().send(sender, Component.text("That player must be online for live permission debugging.", NamedTextColor.RED));
            return true;
        }
        PermissionDebugService.Result result = plugin.api().permissionDebug().inspect(target, args[2]);
        plugin.messages().send(sender, Component.text(target.getName() + " -> " + result.permission() + " = " + result.granted(), result.granted() ? NamedTextColor.GREEN : NamedTextColor.RED));
        plugin.messages().send(sender, Component.text(result.explanation(), NamedTextColor.GRAY));
        for (String match : result.matchedAttachments()) plugin.messages().send(sender, Component.text(" • " + match, NamedTextColor.DARK_GRAY));
        return true;
    }

    private boolean audit(CommandSender sender, String[] args) {
        String query = args.length >= 2 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : null;
        List<AuditEntry> entries = query == null ? plugin.api().audit().recent(15) : plugin.api().audit().search(query, 15);
        plugin.messages().send(sender, Component.text("Mira Audit" + (query == null ? "" : " search: " + query), NamedTextColor.LIGHT_PURPLE));
        if (entries.isEmpty()) plugin.messages().send(sender, Component.text("No matching entries.", NamedTextColor.GRAY));
        for (AuditEntry entry : entries) {
            plugin.messages().send(sender, Component.text(TIME.format(entry.time()) + " [" + entry.source() + "/" + entry.action() + "] ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(entry.actorName() + " -> " + entry.target() + ": " + entry.message(), NamedTextColor.GRAY)));
        }
        return true;
    }

    private boolean profiles(CommandSender sender) {
        List<PlayerProfile> profiles = plugin.api().profiles().cached().stream().sorted(Comparator.comparing(PlayerProfile::lastSeen).reversed()).limit(15).toList();
        plugin.messages().send(sender, Component.text("Cached Mira player profiles: " + plugin.api().profiles().cached().size(), NamedTextColor.AQUA));
        for (PlayerProfile profile : profiles) {
            plugin.messages().send(sender, Component.text(" • " + profile.name() + " " + (profile.online() ? "ONLINE" : "last " + TIME.format(profile.lastSeen())), profile.online() ? NamedTextColor.GREEN : NamedTextColor.GRAY));
        }
        return true;
    }

    private boolean maintenance(CommandSender sender, String[] args) {
        MaintenanceService maintenance = plugin.maintenance();

        if (args.length < 2 || args[1].equalsIgnoreCase("status")) {
            plugin.messages().send(sender, Component.text("Maintenance: " + (maintenance.enabled() ? "ENABLED" : "disabled"),
                    maintenance.enabled() ? NamedTextColor.RED : NamedTextColor.GREEN));
            maintenance.reason().ifPresent(reason ->
                    plugin.messages().send(sender, Component.text("Reason: " + reason, NamedTextColor.GRAY)));
            maintenance.scheduledStart().ifPresent(start ->
                    plugin.messages().send(sender, Component.text("Scheduled start: " + MAINTENANCE_TIME.format(start), NamedTextColor.YELLOW)));
            maintenance.scheduledEnd().ifPresent(end ->
                    plugin.messages().send(sender, Component.text("Scheduled end: " + MAINTENANCE_TIME.format(end), NamedTextColor.YELLOW)));
            plugin.messages().send(sender, Component.text("Bypass re-entry permission: " + maintenance.bypassPermission(), NamedTextColor.GRAY));
            return true;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "on" -> {
                if (maintenance.enabled()) {
                    plugin.messages().send(sender, Component.text("Maintenance mode is already active.", NamedTextColor.YELLOW));
                    return true;
                }

                String reason = args.length >= 3
                        ? String.join(" ", Arrays.copyOfRange(args, 2, args.length))
                        : plugin.getConfig().getString("maintenance.default-reason", "Server maintenance");
                long countdown = Math.max(0L, plugin.getConfig().getLong("maintenance.activation-countdown-seconds", 30L));

                if (countdown <= 0L) {
                    maintenance.enable(sender.getName(), reason);
                    plugin.messages().send(sender, Component.text("Maintenance mode enabled immediately.", NamedTextColor.GREEN));
                } else {
                    Instant startAt = Instant.now().plusSeconds(countdown);
                    maintenance.schedule(startAt, null, sender.getName(), reason);
                    plugin.messages().send(sender, Component.text(
                            "Maintenance countdown started for " + countdown + "s. Reason: " + reason,
                            NamedTextColor.GREEN));
                }
            }
            case "force" -> {
                String reason = args.length >= 3
                        ? String.join(" ", Arrays.copyOfRange(args, 2, args.length))
                        : plugin.getConfig().getString("maintenance.default-reason", "Server maintenance");
                maintenance.enable(sender.getName(), reason);
                plugin.messages().send(sender, Component.text("Maintenance mode enabled immediately.", NamedTextColor.GREEN));
            }
            case "off" -> {
                maintenance.disable(sender.getName());
                plugin.messages().send(sender, Component.text("Maintenance mode disabled.", NamedTextColor.GREEN));
            }
            case "cancel" -> {
                maintenance.clearSchedule(sender.getName());
                plugin.messages().send(sender, Component.text("Maintenance schedule/countdown cleared.", NamedTextColor.GREEN));
            }
            case "schedule" -> {
                if (args.length < 3) {
                    plugin.messages().send(sender, Component.text(
                            "Usage: /miracore maintenance schedule <delay> [duration] [reason...]", NamedTextColor.RED));
                    return true;
                }

                Duration delay = parseDuration(args[2]);
                if (delay == null || delay.isNegative() || delay.isZero()) {
                    plugin.messages().send(sender, Component.text("Use durations such as 10m, 2h or 1d.", NamedTextColor.RED));
                    return true;
                }

                Duration duration = null;
                int reasonIndex = 3;
                if (args.length >= 4) {
                    Duration possibleDuration = parseDuration(args[3]);
                    if (possibleDuration != null) {
                        if (possibleDuration.isNegative() || possibleDuration.isZero()) {
                            plugin.messages().send(sender, Component.text("Maintenance duration must be positive.", NamedTextColor.RED));
                            return true;
                        }
                        duration = possibleDuration;
                        reasonIndex = 4;
                    }
                }

                String reason = reasonIndex < args.length
                        ? String.join(" ", Arrays.copyOfRange(args, reasonIndex, args.length))
                        : plugin.getConfig().getString("maintenance.default-reason", "Server maintenance");

                Instant startAt = Instant.now().plus(delay);
                Instant endAt = duration == null ? null : startAt.plus(duration);
                try {
                    maintenance.schedule(startAt, endAt, sender.getName(), reason);
                    plugin.messages().send(sender, Component.text(
                            "Maintenance scheduled for " + MAINTENANCE_TIME.format(startAt)
                                    + (endAt == null ? "" : " until " + MAINTENANCE_TIME.format(endAt))
                                    + ". Reason: " + reason,
                            NamedTextColor.GREEN));
                } catch (IllegalStateException exception) {
                    plugin.messages().send(sender, Component.text(exception.getMessage(), NamedTextColor.RED));
                }
            }
            default -> plugin.messages().send(sender, Component.text(
                    "Usage: /miracore maintenance <status|on [reason]|force [reason]|off|schedule <delay> [duration] [reason]|cancel>",
                    NamedTextColor.RED));
        }
        return true;
    }

    private boolean essentials(CommandSender sender, String[] args) {
        var bridge = plugin.essentialsPresentation();
        if (bridge == null) {
            plugin.messages().send(sender, Component.text("Essentials presentation bridge is unavailable.", NamedTextColor.RED));
            return true;
        }

        boolean sync = args.length >= 2 && args[1].equalsIgnoreCase("sync");
        if (sync) {
            boolean ok = bridge.sync(true);
            plugin.messages().send(sender, Component.text(
                    ok ? "EssentialsX messages synchronized through MiraCore." : "EssentialsX message sync failed: " + bridge.status(),
                    ok ? NamedTextColor.GREEN : NamedTextColor.RED));
            return true;
        }

        plugin.messages().send(sender, Component.text("EssentialsX presentation: " + bridge.status(), NamedTextColor.LIGHT_PURPLE));
        if (bridge.managedFile() != null) {
            plugin.messages().send(sender, Component.text("Managed file: " + bridge.managedFile().getFileName(), NamedTextColor.GRAY));
        }
        plugin.messages().send(sender, Component.text("Managed messages: " + bridge.managedMessages(), NamedTextColor.GRAY));
        plugin.messages().send(sender, Component.text("Use /miracore essentials sync after editing the MiraCore message file.", NamedTextColor.DARK_GRAY));
        return true;
    }

    private boolean updates(CommandSender sender, String[] args) {
        boolean refresh = args.length >= 2 && args[1].equalsIgnoreCase("refresh");
        List<UpdateService.UpdateStatus> cached = plugin.updates().cached();
        if (!refresh && !cached.isEmpty()) {
            showUpdates(sender, cached);
            return true;
        }

        plugin.messages().send(sender, Component.text("Checking Mira GitHub releases...", NamedTextColor.AQUA));
        plugin.updates().checkNow().whenComplete((statuses, error) ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (error != null) {
                        plugin.messages().send(sender, Component.text("Update check failed: " + error.getMessage(), NamedTextColor.RED));
                        return;
                    }
                    showUpdates(sender, statuses);
                }));
        return true;
    }

    private void showUpdates(CommandSender sender, List<UpdateService.UpdateStatus> statuses) {
        if (statuses.isEmpty()) {
            plugin.messages().send(sender, Component.text("No configured Mira modules were available to check.", NamedTextColor.GRAY));
            return;
        }
        long outdated = statuses.stream().filter(UpdateService.UpdateStatus::updateAvailable).count();
        plugin.messages().send(sender, Component.text("Mira Updates: " + outdated + " update(s) available", outdated > 0 ? NamedTextColor.YELLOW : NamedTextColor.GREEN));
        for (UpdateService.UpdateStatus status : statuses) {
            if (!status.reachable()) {
                plugin.messages().send(sender, Component.text(" • " + status.pluginName() + " v" + status.installedVersion()
                        + " - check failed (" + status.detail() + ")", NamedTextColor.RED));
                continue;
            }
            NamedTextColor color = status.updateAvailable() ? NamedTextColor.YELLOW : NamedTextColor.GRAY;
            String suffix = status.updateAvailable()
                    ? " -> v" + status.latestVersion() + " AVAILABLE"
                    : " current";
            plugin.messages().send(sender, Component.text(" • " + status.pluginName() + " v" + status.installedVersion() + suffix, color));
        }
        plugin.messages().send(sender, Component.text("MiraUpdater is report-only. It never downloads or replaces plugin JARs.", NamedTextColor.DARK_GRAY));
    }

    private Duration parseDuration(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String clean = raw.trim().toLowerCase(Locale.ROOT);
        long multiplier;
        if (clean.endsWith("s")) multiplier = 1L;
        else if (clean.endsWith("m")) multiplier = 60L;
        else if (clean.endsWith("h")) multiplier = 3600L;
        else if (clean.endsWith("d")) multiplier = 86400L;
        else return null;
        try {
            long value = Long.parseLong(clean.substring(0, clean.length() - 1));
            return Duration.ofSeconds(Math.multiplyExact(value, multiplier));
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private boolean help(CommandSender sender) {
        plugin.messages().send(sender, Component.text("/miracore status", NamedTextColor.AQUA).append(Component.text(" - Suite health dashboard", NamedTextColor.GRAY)));
        plugin.messages().send(sender, Component.text("/miracore test", NamedTextColor.AQUA).append(Component.text(" - Run Core diagnostics", NamedTextColor.GRAY)));
        plugin.messages().send(sender, Component.text("/miracore why <player> <permission>", NamedTextColor.AQUA).append(Component.text(" - Explain live permission result", NamedTextColor.GRAY)));
        plugin.messages().send(sender, Component.text("/miracore audit [query]", NamedTextColor.AQUA).append(Component.text(" - Search global Mira audit log", NamedTextColor.GRAY)));
        plugin.messages().send(sender, Component.text("/miracore profiles", NamedTextColor.AQUA).append(Component.text(" - Inspect shared profile cache", NamedTextColor.GRAY)));
        plugin.messages().send(sender, Component.text("/miracore maintenance <status|on|force|off|schedule|cancel>", NamedTextColor.AQUA)
                .append(Component.text(" - Maintenance countdown, gate and schedules", NamedTextColor.GRAY)));
        plugin.messages().send(sender, Component.text("/miracore updates [refresh]", NamedTextColor.AQUA).append(Component.text(" - Report installed Mira modules against GitHub releases", NamedTextColor.GRAY)));
        plugin.messages().send(sender, Component.text("/miracore essentials [sync]", NamedTextColor.AQUA).append(Component.text(" - Inspect/resync Mira-managed EssentialsX messages", NamedTextColor.GRAY)));
        plugin.messages().send(sender, Component.text("/miracore reload", NamedTextColor.AQUA).append(Component.text(" - Reload Core config", NamedTextColor.GRAY)));
        return true;
    }

    private NamedTextColor healthColor(ModuleHealth health) {
        return switch (health) {
            case HEALTHY -> NamedTextColor.GREEN;
            case DEGRADED -> NamedTextColor.YELLOW;
            case UNHEALTHY -> NamedTextColor.RED;
            case DISABLED -> NamedTextColor.DARK_GRAY;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("miracore.admin")) return List.of();
        if (args.length == 1) {
            String typed = args[0].toLowerCase(Locale.ROOT);
            return SUBCOMMANDS.stream().filter(sub -> sub.startsWith(typed)).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("why")) {
            String typed = args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream().map(org.bukkit.entity.Player::getName).filter(n -> n.toLowerCase(Locale.ROOT).startsWith(typed)).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("maintenance")) {
            String typed = args[1].toLowerCase(Locale.ROOT);
            return List.of("status", "on", "force", "off", "schedule", "cancel").stream().filter(v -> v.startsWith(typed)).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("updates")) {
            return "refresh".startsWith(args[1].toLowerCase(Locale.ROOT)) ? List.of("refresh") : List.of();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("essentials")) {
            return "sync".startsWith(args[1].toLowerCase(Locale.ROOT)) ? List.of("sync") : List.of();
        }
        if ((args.length == 3 || args.length == 4) && args[0].equalsIgnoreCase("maintenance") && args[1].equalsIgnoreCase("schedule")) {
            return args.length == 3 ? List.of("10m", "30m", "1h") : List.of("30m", "1h", "2h");
        }
        return List.of();
    }
}
