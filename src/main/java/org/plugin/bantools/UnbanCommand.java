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
            source.sendMessage(Component.text("用法: /unban <玩家>", NamedTextColor.RED));
            return;
        }

        banManager.unbanPlayer(args[0]);
        source.sendMessage(Component.text("已解封玩家: " + args[0], NamedTextColor.GREEN));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("bantools.command.ban");
    }
}
