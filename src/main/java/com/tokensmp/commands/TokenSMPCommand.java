package com.tokensmp.commands;

import com.tokensmp.TokenSMP;
import com.tokensmp.tokens.Token;
import com.tokensmp.tokens.TokenRegistry;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TokenSMPCommand implements CommandExecutor, TabCompleter {

    private final TokenSMP plugin;

    public TokenSMPCommand(TokenSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload" -> {
                if (!sender.hasPermission("tokensmp.admin.reload")) {
                    sender.sendMessage(net.kyori.adventure.text.Component.text(plugin.getConfigManager().getMessage("no-permission")));
                    return true;
                }
                plugin.reload();
                sender.sendMessage(net.kyori.adventure.text.Component.text(plugin.getConfigManager().getMessage("reload-success")));
            }
            case "list" -> {
                if (!sender.hasPermission("tokensmp.admin.list")) {
                    sender.sendMessage(net.kyori.adventure.text.Component.text(plugin.getConfigManager().getMessage("no-permission")));
                    return true;
                }
                listTokens(sender);
            }
            case "give" -> {
                if (!sender.hasPermission("tokensmp.admin.give")) {
                    sender.sendMessage(net.kyori.adventure.text.Component.text(plugin.getConfigManager().getMessage("no-permission")));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(net.kyori.adventure.text.Component.text("<red>Usage: /tokensmp give <player> <token> [amount]</red>"));
                    return true;
                }
                giveToken(sender, args[1], args[2], args.length > 3 ? Integer.parseInt(args[3]) : 1);
            }
            case "take" -> {
                if (!sender.hasPermission("tokensmp.admin.take")) {
                    sender.sendMessage(net.kyori.adventure.text.Component.text(plugin.getConfigManager().getMessage("no-permission")));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(net.kyori.adventure.text.Component.text("<red>Usage: /tokensmp take <player> <token> [amount]</red>"));
                    return true;
                }
                takeToken(sender, args[1], args[2], args.length > 3 ? Integer.parseInt(args[3]) : 1);
            }
            case "help" -> showHelp(sender);
            default -> {
                sender.sendMessage(net.kyori.adventure.text.Component.text("<red>Unknown subcommand. Use /tokensmp help</red>"));
            }
        }
        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(net.kyori.adventure.text.Component.text("<gradient:#00ffff:#ff00ff>=== TokenSMP Help ===</gradient>"));
        sender.sendMessage(net.kyori.adventure.text.Component.text("<white>/tokens</white> <gray>- Open token collection GUI</gray>"));
        sender.sendMessage(net.kyori.adventure.text.Component.text("<white>/tokenrecipe</white> <gray>- Open recipe book GUI</gray>"));
        sender.sendMessage(net.kyori.adventure.text.Component.text("<white>/tokeninfo <token></white> <gray>- View token information</gray>"));
        sender.sendMessage(net.kyori.adventure.text.Component.text("<white>/lives [player]</white> <gray>- View lives</gray>"));
        
        if (sender.hasPermission("tokensmp.admin")) {
            sender.sendMessage(net.kyori.adventure.text.Component.text("<gradient:#ff0000:#ff8c00>--- Admin Commands ---</gradient>"));
            sender.sendMessage(net.kyori.adventure.text.Component.text("<white>/tokensmp give <player> <token> [amount]</white> <gray>- Give token to player</gray>"));
            sender.sendMessage(net.kyori.adventure.text.Component.text("<white>/tokensmp take <player> <token> [amount]</white> <gray>- Take token from player</gray>"));
            sender.sendMessage(net.kyori.adventure.text.Component.text("<white>/tokensmp list</white> <gray>- List all tokens</gray>"));
            sender.sendMessage(net.kyori.adventure.text.Component.text("<white>/tokensmp reload</white> <gray>- Reload configuration</gray>"));
            sender.sendMessage(net.kyori.adventure.text.Component.text("<white>/lives set/add/remove <player> <amount></white> <gray>- Manage lives</gray>"));
        }
        sender.sendMessage(net.kyori.adventure.text.Component.text("<gradient:#00ffff:#ff00ff>=======================</gradient>"));
    }

    private void listTokens(CommandSender sender) {
        TokenRegistry registry = plugin.getTokenRegistry();
        String header = plugin.getConfigManager().getMessage("list-tokens.header");
        String footer = plugin.getConfigManager().getMessage("list-tokens.footer");
        
        sender.sendMessage(net.kyori.adventure.text.Component.text(header));
        for (Token token : registry.getAllTokens().values()) {
            String entry = plugin.getConfigManager().getMessage("list-tokens.entry",
                "id", token.getId(),
                "display_name", token.getDisplayName(),
                "rarity", token.getRarity().getDisplayName(),
                "enabled", token.isEnabled() ? "<green>Enabled</green>" : "<red>Disabled</red>"
            );
            sender.sendMessage(net.kyori.adventure.text.Component.text(entry));
        }
        sender.sendMessage(net.kyori.adventure.text.Component.text(footer));
    }

    private void giveToken(CommandSender sender, String playerName, String tokenId, int amount) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(net.kyori.adventure.text.Component.text(plugin.getConfigManager().getMessage("player-not-found", "player", playerName)));
            return;
        }

        TokenRegistry registry = plugin.getTokenRegistry();
        Token token = registry.getToken(tokenId.toLowerCase());
        if (token == null) {
            sender.sendMessage(net.kyori.adventure.text.Component.text(plugin.getConfigManager().getMessage("invalid-token", "token", tokenId)));
            return;
        }

        if (amount <= 0 || amount > 64) {
            sender.sendMessage(net.kyori.adventure.text.Component.text(plugin.getConfigManager().getMessage("invalid-amount")));
            return;
        }

        ItemStack item = token.createItemStack(amount);
        target.getInventory().addItem(item);
        
        sender.sendMessage(net.kyori.adventure.text.Component.text(
            plugin.getConfigManager().getMessage("give-success", "amount", String.valueOf(amount), "token", token.getDisplayName(), "player", target.getName())
        ));
    }

    private void takeToken(CommandSender sender, String playerName, String tokenId, int amount) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(net.kyori.adventure.text.Component.text(plugin.getConfigManager().getMessage("player-not-found", "player", playerName)));
            return;
        }

        TokenRegistry registry = plugin.getTokenRegistry();
        Token token = registry.getToken(tokenId.toLowerCase());
        if (token == null) {
            sender.sendMessage(net.kyori.adventure.text.Component.text(plugin.getConfigManager().getMessage("invalid-token", "token", tokenId)));
            return;
        }

        int removed = 0;
        for (ItemStack item : target.getInventory().getContents()) {
            if (item != null && Token.getTokenId(item) != null && Token.getTokenId(item).equalsIgnoreCase(tokenId)) {
                int toRemove = Math.min(amount - removed, item.getAmount());
                item.setAmount(item.getAmount() - toRemove);
                if (item.getAmount() <= 0) {
                    item.setType(org.bukkit.Material.AIR);
                }
                removed += toRemove;
                if (removed >= amount) break;
            }
        }

        sender.sendMessage(net.kyori.adventure.text.Component.text(
            plugin.getConfigManager().getMessage("take-success", "amount", String.valueOf(removed), "token", token.getDisplayName(), "player", target.getName())
        ));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("reload");
            completions.add("list");
            completions.add("give");
            completions.add("take");
            completions.add("help");
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("take"))) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("take"))) {
            TokenRegistry registry = plugin.getTokenRegistry();
            completions.addAll(registry.getAllTokens().keySet());
        }
        
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .collect(Collectors.toList());
    }
}