package com.tokensmp.lives;

import com.tokensmp.TokenSMP;
import com.tokensmp.config.ConfigManager;
import com.tokensmp.storage.StorageManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LifeManager implements Listener {

    private final TokenSMP plugin;
    private final ConfigManager configManager;
    private final StorageManager storageManager;
    
    private int defaultLives;
    private int minLives;
    private int maxLives;
    private int deathPenalty;
    private boolean actionBarEnabled;
    private int actionBarInterval;
    private boolean eliminationEnabled;
    private String eliminationAction;
    private String eliminationKickMessage;
    private int eliminationBanDuration;
    private boolean eliminationBroadcast;
    private String eliminationBroadcastMessage;
    
    private final ConcurrentHashMap<UUID, Integer> livesCache = new ConcurrentHashMap<>();

    public LifeManager(TokenSMP plugin, StorageManager storageManager) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.storageManager = storageManager;
        loadConfig();
    }

    public void loadConfig() {
        defaultLives = configManager.getConfig().getInt("lives.default", 10);
        minLives = configManager.getConfig().getInt("lives.minimum", 0);
        maxLives = configManager.getConfig().getInt("lives.maximum", 100);
        deathPenalty = configManager.getConfig().getInt("lives.death-penalty", 1);
        
        actionBarEnabled = configManager.getConfig().getBoolean("lives.action-bar.enabled", true);
        actionBarInterval = configManager.getConfig().getInt("lives.action-bar.interval", 20);
        
        eliminationEnabled = configManager.getConfig().getBoolean("lives.elimination.enabled", false);
        eliminationAction = configManager.getConfig().getString("lives.elimination.action", "kick");
        eliminationKickMessage = configManager.getConfig().getString("lives.elimination.kick-message", "<red>You have been eliminated!</red>");
        eliminationBanDuration = configManager.getConfig().getInt("lives.elimination.ban-duration", 0);
        eliminationBroadcast = configManager.getConfig().getBoolean("lives.elimination.broadcast", true);
        eliminationBroadcastMessage = configManager.getConfig().getString("lives.elimination.broadcast-message", "<yellow>%player% has been eliminated!</yellow>");
    }

    public void loadAll() {
        // Start action bar task
        if (actionBarEnabled) {
            startActionBarTask();
        }
        
        // Load online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            loadPlayer(player.getUniqueId());
        }
    }

    public void loadPlayer(UUID uuid) {
        if (!storageManager.playerExists(uuid)) {
            storageManager.createPlayer(uuid, defaultLives);
        }
        int lives = storageManager.getLives(uuid);
        if (lives >= 0) {
            livesCache.put(uuid, lives);
        }
    }

    public int getLives(UUID uuid) {
        return livesCache.getOrDefault(uuid, -1);
    }

    public void setLives(UUID uuid, int lives) {
        int clamped = Math.max(minLives, Math.min(maxLives, lives));
        livesCache.put(uuid, clamped);
        storageManager.setLives(uuid, clamped);
    }

    public void addLives(UUID uuid, int amount) {
        int current = getLives(uuid);
        if (current < 0) {
            loadPlayer(uuid);
            current = getLives(uuid);
        }
        int newLives = Math.min(maxLives, current + amount);
        setLives(uuid, newLives);
    }

    public void removeLives(UUID uuid, int amount) {
        int current = getLives(uuid);
        if (current < 0) {
            loadPlayer(uuid);
            current = getLives(uuid);
        }
        int newLives = Math.max(minLives, current - amount);
        setLives(uuid, newLives);
        
        // Check for elimination
        if (newLives <= minLives && eliminationEnabled) {
            handleElimination(uuid);
        }
    }

    public boolean canGainLife(UUID uuid) {
        int current = getLives(uuid);
        return current < maxLives;
    }

    public boolean grantLife(UUID uuid) {
        if (!canGainLife(uuid)) {
            return false;
        }
        addLives(uuid, 1);
        return true;
    }

    public void onDeath(UUID uuid) {
        removeLives(uuid, deathPenalty);
    }

    private void handleElimination(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;

        switch (eliminationAction.toLowerCase()) {
            case "kick" -> {
                String message = configManager.getMessage("life-eliminated", "player", player.getName());
                player.kickPlayer(net.kyori.adventure.text.Component.text(message));
            }
            case "ban" -> {
                if (eliminationBanDuration > 0) {
                    player.ban(eliminationKickMessage, null, java.time.Instant.now().plusSeconds(eliminationBanDuration));
                } else {
                    player.ban(eliminationKickMessage, null, null);
                }
            }
            case "spectator" -> {
                player.setGameMode(org.bukkit.GameMode.SPECTATOR);
            }
        }

        if (eliminationBroadcast) {
            String broadcast = eliminationBroadcastMessage.replace("%player%", player.getName());
            Bukkit.broadcast(net.kyori.adventure.text.Component.text(configManager.getMessage(broadcast)));
        }
    }

    private void startActionBarTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    int lives = getLives(player.getUniqueId());
                    if (lives >= 0) {
                        String message = configManager.getMessage("lives-display", "lives", String.valueOf(lives));
                        player.sendActionBar(net.kyori.adventure.text.Component.text(message));
                    }
                }
            }
        }.runTaskTimer(plugin, 0, actionBarInterval);
    }

    public void saveAll() {
        // Lives are saved immediately on change, but we can force save here if needed
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            Integer lives = livesCache.get(uuid);
            if (lives != null) {
                storageManager.setLives(uuid, lives);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        loadPlayer(uuid);
        storageManager.updateLastLogin(uuid);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        // Save on quit
        Integer lives = livesCache.get(uuid);
        if (lives != null) {
            storageManager.setLives(uuid, lives);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        UUID uuid = event.getEntity().getUniqueId();
        // Prevent duplicate death processing
        if (event.getEntity().hasMetadata("tokensmp_death_processed")) return;
        event.getEntity().setMetadata("tokensmp_death_processed", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
        
        // Schedule removal of metadata next tick
        Bukkit.getScheduler().runTask(plugin, () -> {
            event.getEntity().removeMetadata("tokensmp_death_processed", plugin);
        });
        
        onDeath(uuid);
    }

    // Getters for config values
    public int getDefaultLives() { return defaultLives; }
    public int getMinLives() { return minLives; }
    public int getMaxLives() { return maxLives; }
    public int getDeathPenalty() { return deathPenalty; }
    public boolean isEliminationEnabled() { return eliminationEnabled; }
}