package com.mira.core.gui;

import com.mira.core.api.MessageService;
import com.mira.core.api.RewardService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class CoreRewardGui {
    private static final int PAGE_SIZE = 45;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.of("Australia/Brisbane"));

    public record ListHolder(int page) implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }

    public record PreviewHolder(UUID rewardId, int page) implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }

    private final RewardService rewards;
    private final MessageService messages;

    public CoreRewardGui(RewardService rewards, MessageService messages) {
        this.rewards = rewards;
        this.messages = messages;
    }

    public void openList(Player player, int requestedPage) {
        List<RewardService.QueuedReward> pending = rewards.pending(player.getUniqueId());
        int pages = Math.max(1, (pending.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.max(0, Math.min(requestedPage, pages - 1));

        Inventory inventory = Bukkit.createInventory(new ListHolder(page), 54,
                Component.text("Mira Rewards " + (page + 1) + "/" + pages));
        fillFooter(inventory);

        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && start + i < pending.size(); i++) {
            RewardService.QueuedReward reward = pending.get(start + i);
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Source: " + reward.source()));
            lore.add(Component.text("Queued: " + TIME.format(reward.queuedAt())));
            lore.add(Component.text("Item stacks: " + reward.items().size()));
            lore.add(Component.text("Command rewards: " + reward.commands().size()));
            lore.add(Component.empty());
            lore.add(Component.text("Click to preview and claim."));
            inventory.setItem(i, item(Material.CHEST, reward.title(), lore));
        }

        if (page > 0) inventory.setItem(45, item(Material.ARROW, "Previous Page", List.of()));
        inventory.setItem(49, item(Material.PAPER, "Pending Rewards: " + pending.size(), List.of()));
        if (page + 1 < pages) inventory.setItem(53, item(Material.ARROW, "Next Page", List.of()));
        player.openInventory(inventory);
    }

    public void openPreview(Player player, UUID rewardId, int requestedPage) {
        RewardService.QueuedReward reward = rewards.reward(player.getUniqueId(), rewardId).orElse(null);
        if (reward == null) {
            messages.send(player, "&cThat reward is no longer pending.");
            openList(player, 0);
            return;
        }

        List<ItemStack> preview = new ArrayList<>();
        for (ItemStack item : reward.items()) preview.add(item.clone());
        for (String command : reward.commands()) {
            preview.add(item(Material.COMMAND_BLOCK, "Command Reward",
                    List.of(Component.text(command))));
        }

        int pages = Math.max(1, (preview.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        Inventory inventory = Bukkit.createInventory(new PreviewHolder(rewardId, page), 54,
                Component.text(reward.title() + " " + (page + 1) + "/" + pages));
        fillFooter(inventory);

        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && start + i < preview.size(); i++) {
            inventory.setItem(i, preview.get(start + i).clone());
        }

        if (page > 0) inventory.setItem(45, item(Material.ARROW, "Previous Page", List.of()));
        inventory.setItem(48, item(Material.BOOK, "Source: " + reward.source(),
                List.of(Component.text("Queued: " + TIME.format(reward.queuedAt())))));
        inventory.setItem(49, item(Material.LIME_CONCRETE, "Claim Reward",
                List.of(Component.text("Overflow stays safely queued."))));
        inventory.setItem(50, item(Material.BARRIER, "Back to Rewards", List.of()));
        if (page + 1 < pages) inventory.setItem(53, item(Material.ARROW, "Next Page", List.of()));
        player.openInventory(inventory);
    }

    public UUID rewardAt(Player player, int page, int slot) {
        if (slot < 0 || slot >= PAGE_SIZE) return null;
        List<RewardService.QueuedReward> pending = rewards.pending(player.getUniqueId());
        int index = page * PAGE_SIZE + slot;
        return index >= 0 && index < pending.size() ? pending.get(index).id() : null;
    }

    private void fillFooter(Inventory inventory) {
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 45; slot < 54; slot++) inventory.setItem(slot, filler.clone());
    }

    private ItemStack item(Material material, String name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
