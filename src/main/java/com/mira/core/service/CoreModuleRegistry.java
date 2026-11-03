package com.mira.core.service;

import com.mira.core.api.ModuleHealth;
import com.mira.core.api.ModuleRegistry;
import com.mira.core.api.ModuleSnapshot;
import com.mira.core.event.MiraModuleHealthChangeEvent;
import com.mira.core.event.MiraModuleRegisteredEvent;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class CoreModuleRegistry implements ModuleRegistry {
    private final ConcurrentMap<String, Entry> modules = new ConcurrentHashMap<>();

    @Override
    public ModuleSnapshot register(Plugin plugin) {
        return register(plugin, plugin.getName());
    }

    @Override
    public ModuleSnapshot register(Plugin plugin, String displayName) {
        assertPrimaryThread();
        Objects.requireNonNull(plugin, "plugin");
        String key = normalize(plugin.getName());
        String cleanDisplay = displayName == null || displayName.isBlank() ? plugin.getName() : displayName.trim();
        Entry entry = new Entry(plugin, cleanDisplay, ModuleHealth.HEALTHY, "Registered");
        Entry previous = modules.putIfAbsent(key, entry);
        if (previous != null) return snapshot(previous);
        ModuleSnapshot snapshot = snapshot(entry);
        Bukkit.getPluginManager().callEvent(new MiraModuleRegisteredEvent(snapshot));
        return snapshot;
    }

    @Override
    public Optional<ModuleSnapshot> get(String pluginName) {
        if (pluginName == null || pluginName.isBlank()) return Optional.empty();
        Entry entry = modules.get(normalize(pluginName));
        return entry == null ? Optional.empty() : Optional.of(snapshot(entry));
    }

    @Override
    public List<ModuleSnapshot> all() {
        List<ModuleSnapshot> snapshots = new ArrayList<>();
        modules.values().forEach(entry -> snapshots.add(snapshot(entry)));
        snapshots.sort(Comparator.comparing(ModuleSnapshot::displayName, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(snapshots);
    }

    @Override
    public ModuleSnapshot setHealth(Plugin plugin, ModuleHealth health, String detail) {
        assertPrimaryThread();
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(health, "health");
        String key = normalize(plugin.getName());
        Entry existing = modules.get(key);
        if (existing == null) throw new IllegalStateException(plugin.getName() + " is not registered with MiraCore");
        ModuleSnapshot previous = snapshot(existing);
        Entry updated = new Entry(plugin, existing.displayName(), health, detail == null ? "" : detail);
        modules.put(key, updated);
        ModuleSnapshot current = snapshot(updated);
        Bukkit.getPluginManager().callEvent(new MiraModuleHealthChangeEvent(previous, current));
        return current;
    }

    @Override
    public boolean unregister(Plugin plugin) {
        assertPrimaryThread();
        Objects.requireNonNull(plugin, "plugin");
        return modules.remove(normalize(plugin.getName())) != null;
    }

    private ModuleSnapshot snapshot(Entry entry) {
        Plugin plugin = entry.plugin();
        return new ModuleSnapshot(
                plugin.getName(),
                entry.displayName(),
                plugin.getDescription().getVersion(),
                entry.health(),
                entry.detail(),
                plugin.isEnabled()
        );
    }

    private String normalize(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private void assertPrimaryThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("MiraCore module registry mutations must run on the server thread");
        }
    }

    private record Entry(Plugin plugin, String displayName, ModuleHealth health, String detail) {
    }
}
