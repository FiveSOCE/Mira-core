package com.mira.core.listener;

import com.mira.core.MiraCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class CoreConsumableCooldownListener implements Listener {
    private final MiraCorePlugin plugin;

    public CoreConsumableCooldownListener(MiraCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) return;

        if (item.getType() == Material.ENDER_PEARL
                && plugin.getConfig().getBoolean("item-cooldowns.ender-pearl.enabled", true)) {
            if (bypass(player, "item-cooldowns.ender-pearl.bypass-permission")) return;
            if (player.hasCooldown(Material.ENDER_PEARL)) {
                event.setCancelled(true);
                cooldownMessage(player, Material.ENDER_PEARL, "Ender Pearl");
                return;
            }
            int ticks = Math.max(1, plugin.getConfig().getInt("item-cooldowns.ender-pearl.ticks", 60));
            player.setCooldown(Material.ENDER_PEARL, ticks);
            return;
        }

        if (item.getType() == Material.ENCHANTED_GOLDEN_APPLE
                && plugin.getConfig().getBoolean("item-cooldowns.enchanted-golden-apple.enabled", true)) {
            if (bypass(player, "item-cooldowns.enchanted-golden-apple.bypass-permission")) return;
            if (player.hasCooldown(Material.ENCHANTED_GOLDEN_APPLE)) {
                event.setCancelled(true);
                cooldownMessage(player, Material.ENCHANTED_GOLDEN_APPLE, "Enchanted Golden Apple");
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() != Material.ENCHANTED_GOLDEN_APPLE) return;
        Player player = event.getPlayer();
        if (!plugin.getConfig().getBoolean("item-cooldowns.enchanted-golden-apple.enabled", true)) return;
        if (bypass(player, "item-cooldowns.enchanted-golden-apple.bypass-permission")) return;
        int ticks = Math.max(1, plugin.getConfig().getInt("item-cooldowns.enchanted-golden-apple.ticks", 6000));
        player.setCooldown(Material.ENCHANTED_GOLDEN_APPLE, ticks);
    }

    private boolean bypass(Player player, String path) {
        String permission = plugin.getConfig().getString(path, "").trim();
        return !permission.isBlank() && player.hasPermission(permission);
    }

    private void cooldownMessage(Player player, Material material, String name) {
        int ticks = player.getCooldown(material);
        long seconds = Math.max(1L, (long) Math.ceil(ticks / 20.0D));
        player.sendActionBar(Component.text(name + " ready in " + format(seconds) + ".", NamedTextColor.RED));
    }

    private String format(long seconds) {
        if (seconds < 60) return seconds + "s";
        long minutes = seconds / 60;
        long remainder = seconds % 60;
        return remainder == 0 ? minutes + "m" : minutes + "m " + remainder + "s";
    }
}
