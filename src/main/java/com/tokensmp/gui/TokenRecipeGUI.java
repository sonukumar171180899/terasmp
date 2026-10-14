package com.tokensmp.gui;

import com.tokensmp.TokenSMP;
import com.tokensmp.config.ConfigManager;
import com.tokensmp.recipes.RecipeManager;
import com.tokensmp.tokens.Token;
import com.tokensmp.tokens.TokenRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TokenRecipeGUI implements Listener {

    private final TokenSMP plugin;
    private final ConfigManager configManager;
    private final TokenRegistry tokenRegistry;
    private final RecipeManager recipeManager;
    private final String title;
    private final int size;
    private final int itemsPerPage;

    public TokenRecipeGUI(TokenSMP plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.tokenRegistry = plugin.getTokenRegistry();
        this.recipeManager = plugin.getRecipeManager();
        this.title = configManager.getConfig().getString("gui.recipes.title", "<gradient:#ffd700:#ff8c00>Token Recipes</gradient>");
        this.size = configManager.getConfig().getInt("gui.recipes.size", 54);
        this.itemsPerPage = configManager.getConfig().getInt("gui.recipes.items-per-page", 45);
        
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        Map<String, Token> tokens = tokenRegistry.getEnabledTokens();
        List<Token> tokenList = new ArrayList<>(tokens.values());
        
        int totalPages = (int) Math.ceil((double) tokenList.size() / itemsPerPage);
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;
        
        Inventory inventory = Bukkit.createInventory(null, size,
            net.kyori.adventure.text.Component.text(title + " <gray>- Page " + (page + 1) + "/" + Math.max(1, totalPages) + "</gray>"));
        
        int start = page * itemsPerPage;
        int end = Math.min(start + itemsPerPage, tokenList.size());
        
        for (int i = start; i < end; i++) {
            Token token = tokenList.get(i);
            inventory.setItem(i - start, createRecipeItem(token));
        }
        
        // Fill empty slots
        ItemStack filler = createFillerItem();
        for (int i = tokenList.size() - start; i < itemsPerPage; i++) {
            inventory.setItem(i, filler);
        }
        
        // Navigation buttons
        if (totalPages > 1) {
            if (page > 0) {
                inventory.setItem(45, createNavigationItem("previous", "<green>Previous Page</green>", page - 1));
            }
            if (page < totalPages - 1) {
                inventory.setItem(53, createNavigationItem("next", "<green>Next Page</green>", page + 1));
            }
        }
        
        // Close button
        inventory.setItem(49, createCloseItem());
        
        player.openInventory(inventory);
    }

    private ItemStack createRecipeItem(Token token) {
        ItemStack item = token.createItemStack(1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>(meta.lore() != null ? meta.lore() : Collections.emptyList());
            lore.add(net.kyori.adventure.text.Component.text(""));
            lore.add(net.kyori.adventure.text.Component.text("<yellow>Click to view recipe</yellow>"));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createNavigationItem(String action, String name, int page) {
        ItemStack item = new ItemStack(action.equals("previous") ? Material.ARROW : Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text(name));
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            lore.add(net.kyori.adventure.text.Component.text("<gray>Page: </gray><white>" + (page + 1) + "</white>"));
            meta.lore(lore);
            // Store page in PDC
            meta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "recipe_page"),
                org.bukkit.persistence.PersistentDataType.INTEGER,
                page
            );
            meta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "recipe_action"),
                org.bukkit.persistence.PersistentDataType.STRING,
                action
            );
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCloseItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text("<red>Close</red>"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createFillerItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text(" "));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().title().toString().contains("Token Recipes")) return;
        
        event.setCancelled(true);
        
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;
        
        // Check for navigation
        if (clicked.hasItemMeta() && clicked.getItemMeta().getPersistentDataContainer()
                .has(new org.bukkit.NamespacedKey(plugin, "recipe_action"), org.bukkit.persistence.PersistentDataType.STRING)) {
            String action = clicked.getItemMeta().getPersistentDataContainer()
                    .get(new org.bukkit.NamespacedKey(plugin, "recipe_action"), org.bukkit.persistence.PersistentDataType.STRING);
            int page = clicked.getItemMeta().getPersistentDataContainer()
                    .get(new org.bukkit.NamespacedKey(plugin, "recipe_page"), org.bukkit.persistence.PersistentDataType.INTEGER);
            
            if ("previous".equals(action) || "next".equals(action)) {
                open(player, page);
            }
            return;
        }
        
        // Check for close
        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }
        
        // Check for token recipe
        String tokenId = Token.getTokenId(clicked);
        if (tokenId != null) {
            Token token = tokenRegistry.getToken(tokenId);
            if (token != null) {
                showRecipePreview(player, token);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().title().toString().contains("Token Recipes")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Cleanup if needed
    }

    private void showRecipePreview(Player player, Token token) {
        player.closeInventory();
        
        // Find the recipe for this token
        org.bukkit.inventory.Recipe recipe = null;
        for (Map.Entry<String, org.bukkit.inventory.Recipe> entry : recipeManager.getRegisteredRecipes().entrySet()) {
            if (entry.getKey().equals(token.getId())) {
                recipe = entry.getValue();
                break;
            }
        }
        
        String header = configManager.getMessage("recipe-preview.header", "token", token.getDisplayName());
        player.sendMessage(net.kyori.adventure.text.Component.text(header));
        
        if (recipe instanceof org.bukkit.inventory.ShapedRecipe shaped) {
            player.sendMessage(net.kyori.adventure.text.Component.text(configManager.getMessage("recipe-preview.ingredients")));
            
            Map<Character, org.bukkit.inventory.ItemStack> ingredients = shaped.getIngredientMap();
            String[] shape = shaped.getShape();
            
            for (int row = 0; row < shape.length; row++) {
                String line = shape[row];
                for (int col = 0; col < line.length(); col++) {
                    char key = line.charAt(col);
                    if (key != ' ') {
                        ItemStack ingredient = ingredients.get(key);
                        if (ingredient != null) {
                            String slot = "Slot " + (row * 3 + col + 1);
                            String itemName = ingredient.getType().name();
                            int amount = ingredient.getAmount();
                            String line2 = configManager.getMessage("recipe-preview.ingredient-line",
                                "slot", slot, "item", itemName, "amount", String.valueOf(amount));
                            player.sendMessage(net.kyori.adventure.text.Component.text(line2));
                        }
                    } else {
                        String slot = "Slot " + (row * 3 + col + 1);
                        String line2 = configManager.getMessage("recipe-preview.ingredient-line",
                            "slot", slot, "item", "AIR", "amount", "0");
                        player.sendMessage(net.kyori.adventure.text.Component.text(line2));
                    }
                }
            }
        }
        
        ItemStack result = recipe != null ? recipe.getResult() : token.createItemStack(1);
        String outputLine = configManager.getMessage("recipe-preview.output",
            "output", result.getType().name(), "amount", String.valueOf(result.getAmount()));
        player.sendMessage(net.kyori.adventure.text.Component.text(outputLine));
        
        String footer = configManager.getMessage("recipe-preview.footer");
        player.sendMessage(net.kyori.adventure.text.Component.text(footer));
    }
}