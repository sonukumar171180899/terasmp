package com.tokensmp.config;

import com.tokensmp.TokenSMP;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;

public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration tokensConfig;
    private FileConfiguration recipesConfig;
    private FileConfiguration messagesConfig;
    private File configFile;
    private File tokensFile;
    private File recipesFile;
    private File messagesFile;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        loadMainConfig();
        loadTokensConfig();
        loadRecipesConfig();
        loadMessagesConfig();
    }

    private void loadMainConfig() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        configFile = new File(plugin.getDataFolder(), "config.yml");
    }

    private void loadTokensConfig() {
        tokensFile = new File(plugin.getDataFolder(), "tokens.yml");
        if (!tokensFile.exists()) {
            plugin.saveResource("tokens.yml", false);
        }
        tokensConfig = YamlConfiguration.loadConfiguration(tokensFile);
        
        // Also load defaults from jar
        InputStream defaultStream = plugin.getResource("tokens.yml");
        if (defaultStream != null) {
            FileConfiguration defaults = YamlConfiguration.loadConfiguration(defaultStream);
            tokensConfig.setDefaults(defaults);
        }
    }

    private void loadRecipesConfig() {
        recipesFile = new File(plugin.getDataFolder(), "recipes.yml");
        if (!recipesFile.exists()) {
            plugin.saveResource("recipes.yml", false);
        }
        recipesConfig = YamlConfiguration.loadConfiguration(recipesFile);
        
        InputStream defaultStream = plugin.getResource("recipes.yml");
        if (defaultStream != null) {
            FileConfiguration defaults = YamlConfiguration.loadConfiguration(defaultStream);
            recipesConfig.setDefaults(defaults);
        }
    }

    private void loadMessagesConfig() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        
        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            FileConfiguration defaults = YamlConfiguration.loadConfiguration(defaultStream);
            messagesConfig.setDefaults(defaults);
        }
    }

    public void reloadAll() {
        loadAll();
    }

    public void saveTokensConfig() {
        try {
            tokensConfig.save(tokensFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save tokens.yml", e);
        }
    }

    public void saveRecipesConfig() {
        try {
            recipesConfig.save(recipesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save recipes.yml", e);
        }
    }

    public void saveMessagesConfig() {
        try {
            messagesConfig.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save messages.yml", e);
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getTokensConfig() {
        return tokensConfig;
    }

    public FileConfiguration getRecipesConfig() {
        return recipesConfig;
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    public String getMessage(String path) {
        return messagesConfig.getString(path, "Missing message: " + path);
    }

    public String getMessage(String path, Object... args) {
        String message = getMessage(path);
        for (int i = 0; i < args.length; i += 2) {
            if (i + 1 < args.length) {
                message = message.replace("%" + args[i] + "%", args[i + 1].toString());
            }
        }
        return message;
    }
}