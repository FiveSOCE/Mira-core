package com.mira.core.service;

import com.mira.core.MiraCorePlugin;
import com.mira.core.api.MessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class CoreStarterGuideService implements Listener, CommandExecutor {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final MiraCorePlugin plugin;
    private final MessageService messages;
    private final NamespacedKey starterGivenKey;

    public CoreStarterGuideService(MiraCorePlugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
        this.starterGivenKey = new NamespacedKey(plugin, "starter_given");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("starter.enabled", true)) return;
        Player player = event.getPlayer();

        if (plugin.getConfig().getBoolean("starter.first-join-only", true) && player.hasPlayedBefore()) return;
        if (player.getPersistentDataContainer().has(starterGivenKey, PersistentDataType.BYTE)) return;

        player.getPersistentDataContainer().set(starterGivenKey, PersistentDataType.BYTE, (byte) 1);
        Bukkit.getScheduler().runTask(plugin, () -> grantStarter(player));
    }

    private void grantStarter(Player player) {
        if (!player.isOnline()) return;

        for (String raw : plugin.getConfig().getStringList("starter.console-commands")) {
            String command = placeholders(raw, player);
            if (!command.isBlank()) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), stripSlash(command));
        }
        for (String raw : plugin.getConfig().getStringList("starter.player-commands")) {
            String command = placeholders(raw, player);
            if (!command.isBlank()) player.performCommand(stripSlash(command));
        }

        for (String id : plugin.getConfig().getStringList("starter.give-guides")) {
            ItemStack book = guideBook(id);
            if (book == null) continue;
            player.getInventory().addItem(book).values().forEach(left ->
                    player.getWorld().dropItemNaturally(player.getLocation(), left));
        }

        String message = plugin.getConfig().getString("starter.message", "");
        if (message != null && !message.isBlank()) messages.send(player, placeholders(message, player));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "&cPlayers only.");
            return true;
        }
        if (!plugin.getConfig().getBoolean("guides.enabled", true)) {
            messages.send(player, "&cGuides are currently disabled.");
            return true;
        }
        openGuides(player);
        return true;
    }

    public void openGuides(Player player) {
        int rows = Math.max(1, Math.min(6, plugin.getConfig().getInt("guides.rows", 3)));
        String title = plugin.getConfig().getString("guides.title", "&5&lMira Guides");
        Inventory inventory = Bukkit.createInventory(new GuidesHolder(), rows * 9, LEGACY.deserialize(title == null ? "&5&lMira Guides" : title));

        ConfigurationSection entries = plugin.getConfig().getConfigurationSection("guides.entries");
        if (entries != null) {
            for (String id : entries.getKeys(false)) {
                ConfigurationSection section = entries.getConfigurationSection(id);
                if (section == null) continue;
                int slot = section.getInt("slot", -1);
                if (slot < 0 || slot >= inventory.getSize()) continue;

                Material icon = Material.matchMaterial(section.getString("icon", "WRITTEN_BOOK"));
                if (icon == null || icon.isAir()) icon = Material.WRITTEN_BOOK;
                ItemStack display = new ItemStack(icon);
                ItemMeta meta = display.getItemMeta();
                meta.displayName(LEGACY.deserialize(section.getString("name", id)));
                List<Component> lore = section.getStringList("lore").stream().map(LEGACY::deserialize).toList();
                if (!lore.isEmpty()) meta.lore(lore);
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "guide_id"), PersistentDataType.STRING, id);
                display.setItemMeta(meta);
                inventory.setItem(slot, display);
            }
        }
        player.openInventory(inventory);
    }

    @EventHandler
    public void onGuideClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof GuidesHolder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String id = clicked.getItemMeta().getPersistentDataContainer()
                .get(new NamespacedKey(plugin, "guide_id"), PersistentDataType.STRING);
        if (id == null) return;

        ItemStack book = guideBook(id);
        if (book == null) return;
        player.closeInventory();
        Bukkit.getScheduler().runTask(plugin, () -> player.openBook(book));
    }

    private ItemStack guideBook(String id) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("guides.entries." + id);
        if (section == null) return null;

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        if (!(book.getItemMeta() instanceof BookMeta meta)) return null;

        String name = section.getString("name", id);
        String author = section.getString("author", "Mira");
        meta.title(LEGACY.deserialize(name == null ? id : name));
        meta.author(LEGACY.deserialize(author == null ? "Mira" : author));
        meta.displayName(LEGACY.deserialize(name == null ? id : name));

        List<Component> lore = section.getStringList("lore").stream().map(LEGACY::deserialize).toList();
        if (!lore.isEmpty()) meta.lore(lore);

        List<String> configuredPages = section.getStringList("pages");
        if (configuredPages.isEmpty()) configuredPages = List.of("&7No guide content has been configured.");
        meta.pages(configuredPages.stream().map(LEGACY::deserialize).toList());

        book.setItemMeta(meta);
        return book;
    }

    private String placeholders(String raw, Player player) {
        if (raw == null) return "";
        return raw.replace("{player}", player.getName())
                .replace("{display_name}", player.getName())
                .replace("{uuid}", player.getUniqueId().toString());
    }

    private static String stripSlash(String command) {
        String trimmed = command == null ? "" : command.trim();
        return trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
    }

    private record GuidesHolder() implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }
}
