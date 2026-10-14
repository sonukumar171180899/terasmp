package com.tokensmp.recipes;

import com.tokensmp.TokenSMP;
import com.tokensmp.config.ConfigManager;
import com.tokensmp.tokens.Token;
import com.tokensmp.tokens.TokenRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class RecipeManager {

    private final TokenSMP plugin;
    private final ConfigManager configManager;
    private final TokenRegistry tokenRegistry;
    private final Map<String, Recipe> registeredRecipes = new HashMap<>();

    public RecipeManager(TokenSMP plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.tokenRegistry = plugin.getTokenRegistry();
    }

    public void registerRecipes() {
        // Unregister old recipes
        for (Recipe recipe : registeredRecipes.values()) {
            Bukkit.removeRecipe(recipe.getKey());
        }
        registeredRecipes.clear();

        if (!configManager.getConfig().getBoolean("crafting.enabled", true)) {
            plugin.getLogger().info("Custom crafting is disabled in config.");
            return;
        }

        FileConfiguration recipesConfig = configManager.getRecipesConfig();
        ConfigurationSection recipesSection = recipesConfig.getConfigurationSection("recipes");
        
        if (recipesSection == null) {
            plugin.getLogger().warning("No recipes section found in recipes.yml");
            return;
        }

        for (String key : recipesSection.getKeys(false)) {
            ConfigurationSection recipeSection = recipesSection.getConfigurationSection(key);
            if (recipeSection == null) continue;

            try {
                registerRecipe(key, recipeSection);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to register recipe: " + key, e);
            }
        }
        
        plugin.getLogger().info("Registered " + registeredRecipes.size() + " custom recipes.");
    }

    private void registerRecipe(String key, ConfigurationSection section) {
        String tokenId = section.getString("token");
        if (tokenId == null) {
            throw new IllegalArgumentException("Recipe " + key + " missing token field");
        }

        Token token = tokenRegistry.getToken(tokenId);
        if (token == null) {
            throw new IllegalArgumentException("Token not found: " + tokenId);
        }

        if (!token.isEnabled()) {
            plugin.getLogger().info("Skipping recipe for disabled token: " + tokenId);
            return;
        }

        String type = section.getString("type", "shaped").toLowerCase();
        ItemStack result = token.createItemStack(section.getInt("result.amount", 1));
        NamespacedKey recipeKey = new NamespacedKey(plugin, "tokensmp_" + tokenId);

        Recipe recipe;
        if ("shapeless".equals(type)) {
            ShapelessRecipe shapeless = new ShapelessRecipe(recipeKey, result);
            ConfigurationSection ingredients = section.getConfigurationSection("ingredients");
            if (ingredients != null) {
                for (String ingredientKey : ingredients.getKeys(false)) {
                    String materialStr = ingredients.getString(ingredientKey);
                    int amount = 1;
                    if (materialStr.contains(":")) {
                        String[] parts = materialStr.split(":");
                        materialStr = parts[0];
                        amount = Integer.parseInt(parts[1]);
                    }
                    Material material = Material.valueOf(materialStr.toUpperCase());
                    for (int i = 0; i < amount; i++) {
                        shapeless.addIngredient(material);
                    }
                }
            }
            recipe = shapeless;
        } else {
            ShapedRecipe shaped = new ShapedRecipe(recipeKey, result);
            ConfigurationSection patternSection = section.getConfigurationSection("pattern");
            if (patternSection != null) {
                String[] pattern = new String[patternSection.getKeys(false).size()];
                for (String patternKey : patternSection.getKeys(false)) {
                    int index = Integer.parseInt(patternKey) - 1;
                    if (index >= 0 && index < pattern.length) {
                        pattern[index] = patternSection.getString(patternKey);
                    }
                }
                shaped.shape(pattern);
            }

            ConfigurationSection ingredients = section.getConfigurationSection("ingredients");
            if (ingredients != null) {
                for (String ingredientKey : ingredients.getKeys(false)) {
                    String materialStr = ingredients.getString(ingredientKey);
                    char key = ingredientKey.charAt(0);
                    Material material = Material.valueOf(materialStr.toUpperCase());
                    shaped.setIngredient(key, material);
                }
            }
            recipe = shaped;
        }

        Bukkit.addRecipe(recipe);
        registeredRecipes.put(key, recipe);
    }

    public Map<String, Recipe> getRegisteredRecipes() {
        return new HashMap<>(registeredRecipes);
    }

    public void reload() {
        registerRecipes();
    }
}