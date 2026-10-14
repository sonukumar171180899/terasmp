package com.tokensmp.commands;

import com.tokensmp.TokenSMP;
import com.tokensmp.gui.TokenRecipeGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class TokenRecipeCommand implements CommandExecutor, TabCompleter {

    private final TokenSMP plugin;

    public TokenRecipeCommand(TokenSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(net.kyori.adventure.text.Component.text("<red>This command can only be used by players.</red>"));
            return true;
        }

        if (!player.hasPermission("tokensmp.recipe")) {
            player.sendMessage(net.kyori.adventure.text.Component.text(plugin.getConfigManager().getMessage("no-permission")));
            return true;
        }

        plugin.getTokenRecipeGUI().open(player);
        player.sendMessage(net.kyori.adventure.text.Component.text(plugin.getConfigManager().getMessage("recipe-gui-opened")));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}