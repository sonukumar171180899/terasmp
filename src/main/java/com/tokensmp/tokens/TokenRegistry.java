package com.tokensmp.tokens;

import com.tokensmp.TokenSMP;
import com.tokensmp.config.ConfigManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class TokenRegistry {

    private final TokenSMP plugin;
    private final ConfigManager configManager;
    private final Map<String, Token> tokens = new HashMap<>();

    public TokenRegistry(TokenSMP plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    public void loadTokens() {
        tokens.clear();
        FileConfiguration tokensConfig = configManager.getTokensConfig();
        ConfigurationSection tokensSection = tokensConfig.getConfigurationSection("tokens");
        
        if (tokensSection == null) {
            plugin.getLogger().warning("No tokens section found in tokens.yml");
            return;
        }

        for (String key : tokensSection.getKeys(false)) {
            ConfigurationSection tokenSection = tokensSection.getConfigurationSection(key);
            if (tokenSection == null) continue;

            try {
                Token token = new Token(key, tokenSection);
                tokens.put(key.toLowerCase(), token);
                if (token.isEnabled()) {
                    plugin.getLogger().info("Loaded token: " + key + " (" + token.getDisplayName() + ")");
                } else {
                    plugin.getLogger().info("Loaded token (disabled): " + key);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load token: " + key, e);
            }
        }
        plugin.getLogger().info("Loaded " + tokens.size() + " tokens total.");
    }

    public Token getToken(String id) {
        if (id == null) return null;
        return tokens.get(id.toLowerCase());
    }

    public Token getTokenByItem(org.bukkit.inventory.ItemStack item) {
        String tokenId = Token.getTokenId(item);
        return tokenId != null ? getToken(tokenId) : null;
    }

    public Map<String, Token> getAllTokens() {
        return new HashMap<>(tokens);
    }

    public Map<String, Token> getEnabledTokens() {
        Map<String, Token> enabled = new HashMap<>();
        for (Map.Entry<String, Token> entry : tokens.entrySet()) {
            if (entry.getValue().isEnabled()) {
                enabled.put(entry.getKey(), entry.getValue());
            }
        }
        return enabled;
    }

    public boolean isTokenEnabled(String id) {
        Token token = getToken(id);
        return token != null && token.isEnabled();
    }

    public int getTokenCount() {
        return tokens.size();
    }

    public void reload() {
        loadTokens();
    }
}