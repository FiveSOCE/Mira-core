package com.mira.core.service;

import com.mira.core.api.ModuleRegistry;
import com.mira.core.api.ModuleSnapshot;
import com.mira.core.api.UpdateService;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CoreUpdateService implements UpdateService {
    private static final Pattern TAG = Pattern.compile("\"tag_name\"\s*:\s*\"([^\"]+)\"");
    private final JavaPlugin plugin;
    private final ModuleRegistry modules;
    private final HttpClient client;
    private volatile List<UpdateStatus> cached = List.of();

    public CoreUpdateService(JavaPlugin plugin, ModuleRegistry modules) {
        this.plugin = plugin;
        this.modules = modules;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(2, plugin.getConfig().getInt("updater.timeout-seconds", 8))))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public CompletableFuture<List<UpdateStatus>> checkNow() {
        if (!plugin.getConfig().getBoolean("updater.enabled", true)) {
            cached = List.of();
            return CompletableFuture.completedFuture(cached);
        }

        ConfigurationSection repositories = plugin.getConfig().getConfigurationSection("updater.repositories");
        if (repositories == null) {
            cached = List.of();
            return CompletableFuture.completedFuture(cached);
        }

        List<CompletableFuture<UpdateStatus>> futures = new ArrayList<>();
        for (ModuleSnapshot module : modules.all()) {
            String repository = repositories.getString(module.pluginName());
            if (repository == null || repository.isBlank()) repository = repositories.getString(module.displayName());
            if (repository == null || repository.isBlank()) continue;
            futures.add(check(module, repository.trim()));
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenApply(ignored -> {
                    List<UpdateStatus> statuses = futures.stream().map(CompletableFuture::join)
                            .sorted(Comparator.comparing(UpdateStatus::pluginName, String.CASE_INSENSITIVE_ORDER))
                            .toList();
                    cached = statuses;
                    return statuses;
                });
    }

    @Override public List<UpdateStatus> cached() { return cached; }

    @Override
    public Optional<UpdateStatus> cached(String pluginName) {
        if (pluginName == null) return Optional.empty();
        return cached.stream().filter(status -> status.pluginName().equalsIgnoreCase(pluginName)).findFirst();
    }

    private CompletableFuture<UpdateStatus> check(ModuleSnapshot module, String repository) {
        URI uri = URI.create("https://api.github.com/repos/" + repository + "/releases/latest");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "MiraCore/" + plugin.getDescription().getVersion())
                .timeout(Duration.ofSeconds(Math.max(2, plugin.getConfig().getInt("updater.timeout-seconds", 8))))
                .GET().build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .handle((response, error) -> {
                    if (error != null) {
                        return new UpdateStatus(module.displayName(), module.version(), "", repository,
                                false, false, error.getClass().getSimpleName() + ": " + safe(error.getMessage()));
                    }
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        return new UpdateStatus(module.displayName(), module.version(), "", repository,
                                false, false, "GitHub HTTP " + response.statusCode());
                    }
                    Matcher matcher = TAG.matcher(response.body());
                    if (!matcher.find()) {
                        return new UpdateStatus(module.displayName(), module.version(), "", repository,
                                false, false, "Latest release did not contain tag_name");
                    }
                    String latest = stripV(matcher.group(1));
                    String installed = stripV(module.version());
                    return new UpdateStatus(module.displayName(), module.version(), latest, repository,
                            compareVersions(latest, installed) > 0, true, "Latest release v" + latest);
                });
    }

    static int compareVersions(String left, String right) {
        String[] a = stripV(left).split("[.-]");
        String[] b = stripV(right).split("[.-]");
        int max = Math.max(a.length, b.length);
        for (int i = 0; i < max; i++) {
            String av = i < a.length ? a[i] : "0";
            String bv = i < b.length ? b[i] : "0";
            Integer ai = number(av);
            Integer bi = number(bv);
            int cmp = ai != null && bi != null ? Integer.compare(ai, bi) : av.compareToIgnoreCase(bv);
            if (cmp != 0) return cmp;
        }
        return 0;
    }

    private static Integer number(String value) {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ignored) { return null; }
    }

    private static String stripV(String version) {
        if (version == null) return "";
        String clean = version.trim();
        return clean.toLowerCase(Locale.ROOT).startsWith("v") ? clean.substring(1) : clean;
    }

    private static String safe(String value) { return value == null ? "unknown error" : value; }
}
