package com.tokensmp.commands;

import com.tokensmp.TokenSMP;
import com.tokensmp.lives.LifeManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class LivesCommand implements CommandExecutor, TabCompleter {

    private final TokenSMP plugin;

    public LivesCommand(TokenSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LifeManager lifeManager = plugin.getLifeManager();

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(net.kyori.adventure.text.Component.text("<red>Usage: /lives <player></red>"));
                return true;
            }
            
            if (!player.hasPermission("tokensmp.lives")) {
                player.sendMessage(net.kyori.adventure.text.Component.text(plugin.getConfigManager().getMessage("no-permission")));
                return true;
            }

            int lives = lifeManager.getLives(player.getUniqueId());
            if (lives < 0) {
                player.sendMessage(net.kyori.adventure.text.Component.text("<red>Failed to load your lives data.</red>"));
                return true;
            }

            player.sendMessage(net.kyori.adventure.text.Component.text(
                plugin.getConfigManager().getMessage("lives-self", "lives", String.valueOf(lives))
            ));
            return true;
        }

        // Admin subcommands
        if (!sender.hasPermission("tokensmp.admin.lives")) {
            sender.sendMessage(net.kyori.adventure.text.Component.text(plugin.getConfigManager().getMessage("no-permission")));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("set") || subCommand.equals("add") || subCommand.equals("remove")) {
            if (args.length < 3) {
                sender.sendMessage(net.kyori.adventure.text.Component.text("<red>Usage: /lives " + subCommand + " <player> <amount></red>"));
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(net.kyori.adventure.text.Component.text(plugin.getConfigManager().getMessage("player-not-found", "player", args[1])));
                return true;
            }

            int amount;
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(net.kyori.adventure.text.Component.text(plugin.getConfigManager().getMessage("invalid-amount")));
                return true;
            }

            if (amount <= 0) {
                sender.sendMessage(net.kyori.adventure.text.Component.text(plugin.getConfigManager().getMessage("invalid-amount")));
                return true;
            }

            UUID uuid = target.getUniqueId();
            int currentLives = lifeManager.getLives(uuid);
            if (currentLives < 0) {
                sender.sendMessage(net.kyori.adventure.text.Component.text("<red>Failed to load player lives data.</red>"));
                return true;
            }

            switch (subCommand) {
                case "set" -> {
                    lifeManager.setLives(uuid, amount);
                    sender.sendMessage(net.kyori.adventure.text.Component.text(
                        plugin.getConfigManager().getMessage("lives-set", "player", target.getName(), "amount", String.valueOf(amount))
                    ));
                }
                case "add" -> {
                    if (currentLives + amount > lifeManager.getMaxLives()) {
                        sender.sendMessage(net.kyori.adventure.text.Component.text(
                            plugin.getConfigManager().getMessage("lives-max-reached", "max", String.valueOf(lifeManager.getMaxLives()))
                        ));
                        return true;
                    }
                    lifeManager.addLives(uuid, amount);
                    sender.sendMessage(net.kyori.adventure.text.Component.text(
                        plugin.getConfigManager().getMessage("lives-add", "player", target.getName(), "amount", String.valueOf(amount), "lives", String.valueOf(lifeManager.getLives(uuid)))
                    ));
                }
                case "remove" -> {
                    if (currentLives - amount < lifeManager.getMinLives()) {
                        sender.sendMessage(net.kyori.adventure.text.Component.text(
                            plugin.getConfigManager().getMessage("lives-min-reached", "min", String.valueOf(lifeManager.getMinLives()))
                        ));
                        return true;
                    }
                    lifeManager.removeLives(uuid, amount);
                    sender.sendMessage(net.kyori.adventure.text.Component.text(
                        plugin.getConfigManager().getMessage("lives-remove", "player", target.getName(), "amount", String.valueOf(amount), "lives", String.valueOf(lifeManager.getLives(uuid)))
                    ));
                }
            }
            return true;
        } else {
            // View other player's lives
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(net.kyori.adventure.text.Component.text(plugin.getConfigManager().getMessage("player-not-found", "player", args[0])));
                return true;
            }

            int lives = lifeManager.getLives(target.getUniqueId());
            if (lives < 0) {
                sender.sendMessage(net.kyori.adventure.text.Component.text("<red>Failed to load player lives data.</red>"));
                return true;
            }

            sender.sendMessage(net.kyori.adventure.text.Component.text(
                plugin.getConfigManager().getMessage("lives-other", "player", target.getName(), "lives", String.valueOf(lives))
            ));
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            if (sender.hasPermission("tokensmp.admin.lives")) {
                completions.add("set");
                completions.add("add");
                completions.add("remove");
            }
            // Add online player names
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
        }
        
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .collect(Collectors.toList());
    }
}