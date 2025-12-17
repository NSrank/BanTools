package org.plugin.bantools;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class UnbanCommand implements SimpleCommand {
    private final BanManager banManager;

    public UnbanCommand(BanManager banManager) {
        this.banManager = banManager;
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        CommandSource source = invocation.source();

        if (args.length != 1) {
            source.sendMessage(Component.text("Usage: /unban <player>", NamedTextColor.RED));
            return;
        }

        String target = args[0].trim();
        // Input validation
        if (target.isEmpty()) {
            source.sendMessage(Component.text("Player name cannot be empty", NamedTextColor.RED));
            return;
        }
        if (target.length() > 16 || !target.matches("^[a-zA-Z0-9_]{1,16}$")) {
            source.sendMessage(Component.text("Invalid player name format", NamedTextColor.RED));
            return;
        }

        banManager.unbanPlayer(target);
        source.sendMessage(Component.text("Player unbanned: " + target, NamedTextColor.GREEN));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("bantools.command.unban");
    }
}
