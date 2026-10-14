package com.tokensmp.gui;

import com.tokensmp.TokenSMP;
import com.tokensmp.config.ConfigManager;
import com.tokensmp.tokens.Token;
import com.tokensmp.tokens.TokenRegistry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

public class TokenGUI implements Listener {

    private final TokenSMP plugin;
    private final ConfigManager configManager;
    private final TokenRegistry tokenRegistry;
    private final String title;
    private final int size;

    public TokenGUI(TokenSMP plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.tokenRegistry = plugin.getTokenRegistry();
        this.title = configManager.getConfig().getString("gui.tokens.title", "<gradient:#00ffff:#ff00ff>Token Collection</gradient>");
        this.size = configManager.getConfig().getInt("gui.tokens.size", 54);
        
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(null, size, 
            net.kyori.adventure.text.Component.text(title));
        
        Map<String, Token> tokens = tokenRegistry.getEnabledTokens();
        int slot = 0;
        
        for (Token token : tokens.values()) {
            if (slot >= size) break;
            inventory.setItem(slot++, createTokenDisplayItem(token));
        }
        
        // Fill remaining slots with glass panes
        ItemStack filler = createFillerItem();
        for (int i = slot; i < size; i++) {
            inventory.setItem(i, filler);
        }
        
        player.openInventory(inventory);
    }

    private ItemStack createTokenDisplayItem(Token token) {
        ItemStack item = token.createItemStack(1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Add click info to lore
            java.util.List<net.kyori.adventure.text.Component> lore = new java.util.ArrayList<>(meta.lore() != null ? meta.lore() : java.util.Collections.emptyList());
            lore.add(net.kyori.adventure.text.Component.text(""));
            lore.add(net.kyori.adventure.text.Component.text("<gray>Click to view details</gray>"));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createFillerItem() {
        ItemStack item = new ItemStack(org.bukkit.Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text(" "));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(net.kyori.adventure.text.Component.text(title))) return;
        
        event.setCancelled(true);
        
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;
        
        String tokenId = Token.getTokenId(clicked);
        if (tokenId != null) {
            Token token = tokenRegistry.getToken(tokenId);
            if (token != null) {
                showTokenInfo(player, token);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().title().equals(net.kyori.adventure.text.Component.text(title))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Cleanup if needed
    }

    private void showTokenInfo(Player player, Token token) {
        player.closeInventory();
        
        String header = configManager.getMessage("token-info.header", "token", token.getDisplayName());
        String name = configManager.getMessage("token-info.name", "display_name", token.getDisplayName());
        String rarity = configManager.getMessage("token-info.rarity", "rarity", token.getRarity().getDisplayName());
        String material = configManager.getMessage("token-info.material", "material", token.getMaterial().name());
        String health = configManager.getMessage("token-info.health", "health", String.valueOf(token.getHealth()));
        String primary = configManager.getMessage("token-info.primary", 
            "primary_ability", token.getPrimary().getAbility(),
            "primary_cooldown", token.getPrimary().getCooldown());
        String secondary = configManager.getMessage("token-info.secondary",
            "secondary_ability", token.getSecondary().getAbility(),
            "secondary_cooldown", token.getSecondary().getCooldown());
        String enabled = configManager.getMessage("token-info.enabled", 
            "enabled", token.isEnabled() ? "<green>Enabled</green>" : "<red>Disabled</red>");
        String footer = configManager.getMessage("token-info.footer");

        player.sendMessage(net.kyori.adventure.text.Component.text(header));
        player.sendMessage(net.kyori.adventure.text.Component.text(name));
        player.sendMessage(net.kyori.adventure.text.Component.text(rarity));
        player.sendMessage(net.kyori.adventure.text.Component.text(material));
        player.sendMessage(net.kyori.adventure.text.Component.text(health));
        player.sendMessage(net.kyori.adventure.text.Component.text(primary));
        player.sendMessage(net.kyori.adventure.text.Component.text(secondary));
        player.sendMessage(net.kyori.adventure.text.Component.text(enabled));
        player.sendMessage(net.kyori.adventure.text.Component.text(footer));
    }
}