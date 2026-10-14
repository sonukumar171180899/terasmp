package com.tokensmp.listeners;

import com.tokensmp.TokenSMP;
import com.tokensmp.config.ConfigManager;
import com.tokensmp.lives.LifeManager;
import com.tokensmp.tokens.Token;
import com.tokensmp.tokens.TokenRegistry;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class TokenListener implements Listener {

    private final TokenSMP plugin;
    private final ConfigManager configManager;
    private final TokenRegistry tokenRegistry;
    private final LifeManager lifeManager;

    public TokenListener(TokenSMP plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.tokenRegistry = plugin.getTokenRegistry();
        this.lifeManager = plugin.getLifeManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null || item.getType().isAir()) return;
        
        Token token = tokenRegistry.getTokenByItem(item);
        if (token == null) return;
        
        if (!token.isEnabled()) {
            player.sendMessage(net.kyori.adventure.text.Component.text(configManager.getMessage("token-disabled")));
            event.setCancelled(true);
            return;
        }
        
        // Check disabled worlds
        if (configManager.getConfig().getStringList("tokens.disabled-worlds").contains(player.getWorld().getName())) {
            player.sendMessage(net.kyori.adventure.text.Component.text(configManager.getMessage("life-token-not-allowed")));
            event.setCancelled(true);
            return;
        }
        
        // Handle consumable tokens (Life Token)
        if (token.isConsumable()) {
            handleConsumableToken(player, token, item, event);
            return;
        }
        
        // Handle ability activation
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // Primary ability on right click
            activateAbility(player, token, token.getPrimary(), "primary");
        }
    }

    private void handleConsumableToken(Player player, Token token, ItemStack item, PlayerInteractEvent event) {
        // Prevent use in creative mode
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        
        // Check if it's the life token
        if ("life_token".equals(token.getId())) {
            if (!lifeManager.grantLife(player.getUniqueId())) {
                player.sendMessage(net.kyori.adventure.text.Component.text(
                    configManager.getMessage("life-token-max", "max", String.valueOf(lifeManager.getMaxLives()))
                ));
                return;
            }
            
            // Consume the item
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(new ItemStack(org.bukkit.Material.AIR));
            }
            
            player.sendMessage(net.kyori.adventure.text.Component.text(
                configManager.getMessage("life-token-consumed", "lives", String.valueOf(lifeManager.getLives(player.getUniqueId())))
            ));
            
            // Play effects
            player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            player.getWorld().spawnParticle(org.bukkit.Particle.HEART, player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
            
            event.setCancelled(true);
        }
    }

    private void activateAbility(Player player, Token token, Token.AbilityConfig abilityConfig, String abilityType) {
        if (abilityConfig.getAbility().isEmpty()) {
            return;
        }
        
        // Check cooldown
        String cooldownKey = "tokensmp_cooldown_" + token.getId() + "_" + abilityType;
        if (player.hasCooldown(token.getMaterial())) {
            long remaining = player.getCooldown(token.getMaterial());
            player.sendMessage(net.kyori.adventure.text.Component.text(
                configManager.getMessage("cooldown", "time", formatTime(remaining))
            ));
            return;
        }
        
        // Set cooldown
        long cooldownTicks = abilityConfig.getCooldownTicks();
        if (cooldownTicks > 0) {
            player.setCooldown(token.getMaterial(), (int) (cooldownTicks / 20));
        }
        
        // Execute ability
        executeAbility(player, token, abilityConfig.getAbility());
        
        // Feedback
        player.sendMessage(net.kyori.adventure.text.Component.text(
            configManager.getMessage("ability-used", "ability", abilityConfig.getAbility())
        ));
    }

    private void executeAbility(Player player, Token token, String abilityName) {
        // Delegate to ability system (to be implemented in Milestone 6)
        // For now, just log
        plugin.getLogger().info(player.getName() + " used ability: " + abilityName + " from token: " + token.getId());
    }

    private String formatTime(long ticks) {
        long seconds = ticks / 20;
        if (seconds < 60) {
            return seconds + "s";
        } else {
            long minutes = seconds / 60;
            seconds = seconds % 60;
            return minutes + "m " + seconds + "s";
        }
    }

    @EventHandler
    public void onItemConsume(PlayerItemConsumeEvent event) {
        // Prevent consuming tokens through normal eating
        if (event.getItem().hasItemMeta() && Token.isToken(event.getItem())) {
            event.setCancelled(true);
        }
    }
}