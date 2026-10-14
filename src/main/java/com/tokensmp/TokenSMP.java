package com.tokensmp;

import com.tokensmp.commands.*;
import com.tokensmp.config.ConfigManager;
import com.tokensmp.gui.TokenGUI;
import com.tokensmp.gui.TokenRecipeGUI;
import com.tokensmp.listeners.TokenListener;
import com.tokensmp.recipes.RecipeManager;
import com.tokensmp.storage.StorageManager;
import com.tokensmp.tokens.TokenRegistry;
import com.tokensmp.lives.LifeManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;

public final class TokenSMP extends JavaPlugin {

    private static TokenSMP instance;
    
    private ConfigManager configManager;
    private TokenRegistry tokenRegistry;
    private StorageManager storageManager;
    private LifeManager lifeManager;
    private RecipeManager recipeManager;
    private TokenGUI tokenGUI;
    private TokenRecipeGUI tokenRecipeGUI;

    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        saveResource("tokens.yml", false);
        saveResource("recipes.yml", false);
        saveResource("messages.yml", false);
        
        configManager = new ConfigManager(this);
        configManager.loadAll();
        
        storageManager = new StorageManager(this);
        storageManager.initialize();
        
        tokenRegistry = new TokenRegistry(this);
        tokenRegistry.loadTokens();
        
        lifeManager = new LifeManager(this, storageManager);
        lifeManager.loadAll();
        
        recipeManager = new RecipeManager(this);
        recipeManager.registerRecipes();
        
        tokenGUI = new TokenGUI(this);
        tokenRecipeGUI = new TokenRecipeGUI(this);
        
        registerCommands();
        registerListeners();
        
        getLogger().info("TokenSMP has been enabled!");
    }

    @Override
    public void onDisable() {
        if (lifeManager != null) {
            lifeManager.saveAll();
        }
        if (storageManager != null) {
            storageManager.close();
        }
        getLogger().info("TokenSMP has been disabled!");
    }

    private void registerCommands() {
        getCommand("tokens").setExecutor(new TokensCommand(this));
        getCommand("tokens").setTabCompleter(new TokensTabCompleter(this));
        
        getCommand("tokenrecipe").setExecutor(new TokenRecipeCommand(this));
        getCommand("tokenrecipe").setTabCompleter(new TokenRecipeTabCompleter(this));
        
        getCommand("tokeninfo").setExecutor(new TokenInfoCommand(this));
        getCommand("tokeninfo").setTabCompleter(new TokenInfoTabCompleter(this));
        
        getCommand("lives").setExecutor(new LivesCommand(this));
        getCommand("lives").setTabCompleter(new LivesTabCompleter(this));
        
        getCommand("tokensmp").setExecutor(new TokenSMPCommand(this));
        getCommand("tokensmp").setTabCompleter(new TokenSMPTabCompleter(this));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new TokenListener(this), this);
    }

    public void reload() {
        configManager.loadAll();
        tokenRegistry.loadTokens();
        recipeManager.registerRecipes();
        getLogger().info("TokenSMP configuration reloaded!");
    }

    public static TokenSMP getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public TokenRegistry getTokenRegistry() {
        return tokenRegistry;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public LifeManager getLifeManager() {
        return lifeManager;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    public TokenGUI getTokenGUI() {
        return tokenGUI;
    }

    public TokenRecipeGUI getTokenRecipeGUI() {
        return tokenRecipeGUI;
    }
}