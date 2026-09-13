package com.mira.core.service;

import com.mira.core.MiraCorePlugin;
import com.mira.core.api.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public final class CoreFixService implements Listener, CommandExecutor {
    private final MiraCorePlugin plugin;
    private final MessageService messages;
    private final NamespacedKey fixAllCooldownKey;

    public CoreFixService(MiraCorePlugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
        this.fixAllCooldownKey = new NamespacedKey(plugin, "fix_all_cooldown_until");
    }

    /**
     * Essentials also registers /fix. Intercept the plain command before Bukkit command
     * resolution so MiraCore always owns the player-facing /fix hand and /fix all flow.
     * Namespaced commands such as /essentials:fix are intentionally left alone.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String raw = event.getMessage();
        if (raw == null || raw.length() < 2) return;
        String body = raw.substring(1).trim();
        if (body.isEmpty()) return;
        String[] args = body.split("\\s+");
        if (!args[0].equalsIgnoreCase("fix")) return;

        event.setCancelled(true);
        String[] subArgs = new String[Math.max(0, args.length - 1)];
        if (subArgs.length > 0) System.arraycopy(args, 1, subArgs, 0, subArgs.length);
        execute(event.getPlayer(), subArgs);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "&cOnly players can use /fix.");
            return true;
        }
        execute(player, args);
        return true;
    }

    private void execute(Player player, String[] args) {
        if (!plugin.getConfig().getBoolean("fix.enabled", true)) {
            messages.send(player, "&cItem fixing is currently disabled.");
            return;
        }
        if (args.length != 1) {
            messages.send(player, "&eUsage: /fix <hand|all>");
            return;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "hand" -> fixHand(player);
            case "all" -> fixAll(player);
            default -> messages.send(player, "&eUsage: /fix <hand|all>");
        }
    }

    private void fixHand(Player player) {
        String permission = plugin.getConfig().getString("fix.hand.permission", "miracore.fix.hand");
        if (!player.hasPermission(permission)) {
            messages.send(player, "&cYou have not unlocked /fix hand.");
            return;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!isDamaged(held)) {
            messages.send(player, "&eThe item in your hand does not need repairing.");
            return;
        }

        long configured = Math.max(0L, plugin.getConfig().getLong("fix.hand.cost", 15000L));
        BigDecimal cost = BigDecimal.valueOf(configured);
        if (!charge(player, cost)) return;

        repair(held);
        player.updateInventory();
        messages.send(player, "&aRepaired the item in your hand for &f" + money(cost) + "&a.");
    }

    private void fixAll(Player player) {
        String permission = plugin.getConfig().getString("fix.all.permission", "miracore.fix.all");
        if (!player.hasPermission(permission)) {
            messages.send(player, "&cYou have not unlocked /fix all.");
            return;
        }

        long now = System.currentTimeMillis();
        long until = player.getPersistentDataContainer().getOrDefault(fixAllCooldownKey, PersistentDataType.LONG, 0L);
        if (until > now && !player.hasPermission("miracore.fix.cooldown.bypass")) {
            messages.send(player, "&e/fix all is on cooldown for another &f" + formatDuration(until - now) + "&e.");
            return;
        }

        int damaged = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isDamaged(item)) damaged++;
        }
        if (damaged == 0) {
            messages.send(player, "&eYou do not have any damaged items to repair.");
            return;
        }

        long perItem = Math.max(0L, plugin.getConfig().getLong("fix.all.cost-per-item", 5000L));
        BigDecimal cost = BigDecimal.valueOf(perItem).multiply(BigDecimal.valueOf(damaged));
        if (!charge(player, cost)) return;

        int repaired = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (repair(item)) repaired++;
        }

        long cooldownSeconds = Math.max(0L, plugin.getConfig().getLong("fix.all.cooldown-seconds", 18000L));
        if (cooldownSeconds > 0L && !player.hasPermission("miracore.fix.cooldown.bypass")) {
            player.getPersistentDataContainer().set(fixAllCooldownKey, PersistentDataType.LONG,
                    System.currentTimeMillis() + cooldownSeconds * 1000L);
        }
        player.updateInventory();
        messages.send(player, "&aRepaired &f" + repaired + " &aitem" + (repaired == 1 ? "" : "s")
                + " for &f" + money(cost) + "&a.");
    }

    private boolean isDamaged(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta instanceof Damageable damageable && damageable.getDamage() > 0;
    }

    private boolean repair(ItemStack item) {
        if (!isDamaged(item)) return false;
        ItemMeta meta = item.getItemMeta();
        Damageable damageable = (Damageable) meta;
        damageable.setDamage(0);
        item.setItemMeta(meta);
        return true;
    }

    private boolean charge(Player player, BigDecimal amount) {
        if (amount.signum() <= 0) return true;
        Plugin essentials = Bukkit.getPluginManager().getPlugin("Essentials");
        if (essentials == null || !essentials.isEnabled()) {
            messages.send(player, "&cThe economy is unavailable right now.");
            return false;
        }

        try {
            Object user = essentialsUser(essentials, player);
            if (user == null) {
                messages.send(player, "&cYour economy account could not be loaded.");
                return false;
            }
            Object rawBalance = user.getClass().getMethod("getMoney").invoke(user);
            if (!(rawBalance instanceof BigDecimal balance)) {
                messages.send(player, "&cYour economy balance could not be read.");
                return false;
            }
            if (balance.compareTo(amount) < 0) {
                messages.send(player, "&cYou need &f" + money(amount) + " &cto do that. &7Balance: &f" + money(balance));
                return false;
            }
            user.getClass().getMethod("takeMoney", BigDecimal.class).invoke(user, amount);
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            Throwable cause = exception instanceof InvocationTargetException invocation && invocation.getCause() != null
                    ? invocation.getCause() : exception;
            plugin.getLogger().warning("Could not charge " + player.getName() + " for /fix: " + cause.getMessage());
            messages.send(player, "&cThe repair charge could not be processed. Nothing was repaired.");
            return false;
        }
    }

    private Object essentialsUser(Plugin essentials, Player player) throws ReflectiveOperationException {
        try {
            Method exact = essentials.getClass().getMethod("getUser", Object.class);
            return exact.invoke(essentials, player);
        } catch (NoSuchMethodException ignored) {
            Method exact = essentials.getClass().getMethod("getUser", Player.class);
            return exact.invoke(essentials, player);
        }
    }

    private String money(BigDecimal amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(0);
        return format.format(amount);
    }

    private String formatDuration(long millis) {
        long total = Math.max(1L, (millis + 999L) / 1000L);
        long hours = total / 3600L;
        long minutes = (total % 3600L) / 60L;
        long seconds = total % 60L;
        if (hours > 0L) return hours + "h " + minutes + "m";
        if (minutes > 0L) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }
}
