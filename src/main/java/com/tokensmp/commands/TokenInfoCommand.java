package com.tokensmp.commands;

import com.tokensmp.TokenSMP;
import com.tokensmp.tokens.Token;
import com.tokensmp.tokens.TokenRegistry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TokenInfoCommand implements CommandExecutor, TabCompleter {

    private final TokenSMP plugin;

    public TokenInfoCommand(TokenSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(net.kyori.adventure.text.Component.text("<red>Usage: /tokeninfo <token></red>"));
            return true;
        }

        String tokenId = args[0].toLowerCase();
        TokenRegistry registry = plugin.getTokenRegistry();
        Token token = registry.getToken(tokenId);

        if (token == null) {
            sender.sendMessage(net.kyori.adventure.text.Component.text(plugin.getConfigManager().getMessage("invalid-token", "token", tokenId)));
            return true;
        }

        String header = plugin.getConfigManager().getMessage("token-info.header", "token", token.getDisplayName());
        String name = plugin.getConfigManager().getMessage("token-info.name", "display_name", token.getDisplayName());
        String rarity = plugin.getConfigManager().getMessage("token-info.rarity", "rarity", token.getRarity().getDisplayName());
        String material = plugin.getConfigManager().getMessage("token-info.material", "material", token.getMaterial().name());
        String health = plugin.getConfigManager().getMessage("token-info.health", "health", String.valueOf(token.getHealth()));
        String primary = plugin.getConfigManager().getMessage("token-info.primary",
            "primary_ability", token.getPrimary().getAbility(),
            "primary_cooldown", token.getPrimary().getCooldown());
        String secondary = plugin.getConfigManager().getMessage("token-info.secondary",
            "secondary_ability", token.getSecondary().getAbility(),
            "secondary_cooldown", token.getSecondary().getCooldown());
        String enabled = plugin.getConfigManager().getMessage("token-info.enabled",
            "enabled", token.isEnabled() ? "<green>Enabled</green>" : "<red>Disabled</red>");
        String footer = plugin.getConfigManager().getMessage("token-info.footer");

        sender.sendMessage(net.kyori.adventure.text.Component.text(header));
        sender.sendMessage(net.kyori.adventure.text.Component.text(name));
        sender.sendMessage(net.kyori.adventure.text.Component.text(rarity));
        sender.sendMessage(net.kyori.adventure.text.Component.text(material));
        sender.sendMessage(net.kyori.adventure.text.Component.text(health));
        sender.sendMessage(net.kyori.adventure.text.Component.text(primary));
        sender.sendMessage(net.kyori.adventure.text.Component.text(secondary));
        sender.sendMessage(net.kyori.adventure.text.Component.text(enabled));
        sender.sendMessage(net.kyori.adventure.text.Component.text(footer));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            TokenRegistry registry = plugin.getTokenRegistry();
            return registry.getAllTokens().keySet().stream()
                .filter(id -> id.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}