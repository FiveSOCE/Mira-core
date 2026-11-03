package com.mira.core.service;

import com.mira.core.MiraCorePlugin;
import com.mira.core.api.AuditEntry;
import com.mira.core.api.AuditService;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.time.Instant;
import java.util.*;

public final class CoreAuditService implements AuditService {
    private final MiraCorePlugin plugin;
    private final File file;
    private final YamlConfiguration yaml;

    public CoreAuditService(MiraCorePlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "audit.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public synchronized void record(String source, String action, UUID actor, String actorName, String target, String message, Map<String, String> metadata) {
        long now = System.currentTimeMillis();
        String id = now + "-" + UUID.randomUUID().toString().substring(0, 8);
        String base = "entries." + id;
        yaml.set(base + ".time", now);
        yaml.set(base + ".source", source == null ? "unknown" : source);
        yaml.set(base + ".action", action == null ? "unknown" : action);
        yaml.set(base + ".actor", actor == null ? null : actor.toString());
        yaml.set(base + ".actor-name", actorName == null ? "SYSTEM" : actorName);
        yaml.set(base + ".target", target == null ? "" : target);
        yaml.set(base + ".message", message == null ? "" : message);
        if (metadata != null) for (Map.Entry<String,String> entry : metadata.entrySet()) yaml.set(base + ".metadata." + entry.getKey(), entry.getValue());
        trim();
        save();
    }

    @Override public synchronized List<AuditEntry> recent(int limit) { return read(null, limit); }
    @Override public synchronized List<AuditEntry> search(String query, int limit) { return read(query, limit); }

    private List<AuditEntry> read(String query, int limit) {
        ConfigurationSection root = yaml.getConfigurationSection("entries");
        if (root == null) return List.of();
        String needle = query == null ? null : query.toLowerCase(Locale.ROOT);
        List<AuditEntry> out = new ArrayList<>();
        for (String id : root.getKeys(false)) {
            String base = "entries." + id;
            String source = yaml.getString(base + ".source", "unknown");
            String action = yaml.getString(base + ".action", "unknown");
            String actorName = yaml.getString(base + ".actor-name", "SYSTEM");
            String target = yaml.getString(base + ".target", "");
            String message = yaml.getString(base + ".message", "");
            if (needle != null && !(source + " " + action + " " + actorName + " " + target + " " + message).toLowerCase(Locale.ROOT).contains(needle)) continue;
            UUID actor = null;
            try { String raw = yaml.getString(base + ".actor"); if (raw != null) actor = UUID.fromString(raw); } catch (Exception ignored) { }
            Map<String,String> metadata = new HashMap<>();
            ConfigurationSection meta = yaml.getConfigurationSection(base + ".metadata");
            if (meta != null) for (String key : meta.getKeys(false)) metadata.put(key, meta.getString(key, ""));
            out.add(new AuditEntry(Instant.ofEpochMilli(yaml.getLong(base + ".time", 0L)), source, action, actor, actorName, target, message, Map.copyOf(metadata)));
        }
        out.sort(Comparator.comparing(AuditEntry::time).reversed());
        if (out.size() > Math.max(1, limit)) return List.copyOf(out.subList(0, Math.max(1, limit)));
        return List.copyOf(out);
    }

    private void trim() {
        int max = Math.max(100, plugin.getConfig().getInt("audit.max-entries", 5000));
        ConfigurationSection root = yaml.getConfigurationSection("entries");
        if (root == null || root.getKeys(false).size() <= max) return;
        List<String> ids = new ArrayList<>(root.getKeys(false));
        ids.sort(Comparator.comparingLong(id -> yaml.getLong("entries." + id + ".time", 0L)));
        while (ids.size() > max) yaml.set("entries." + ids.remove(0), null);
    }

    private void save() {
        try { yaml.save(file); }
        catch (Exception ex) { plugin.getLogger().warning("Could not save audit.yml: " + ex.getMessage()); }
    }
}
