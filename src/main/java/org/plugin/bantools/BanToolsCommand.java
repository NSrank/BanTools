package org.plugin.bantools;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.io.File;
import java.util.List;

public class BanToolsCommand implements SimpleCommand {

    private final BanManager banManager;
    private final ProxyServer server;
    private final Logger logger;

    public BanToolsCommand(BanManager banManager, ProxyServer server, Logger logger) {
        this.banManager = banManager;
        this.server = server;
        this.logger = logger;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length < 1) {
            source.sendMessage(Component.text("Usage: /bantools reload OR /ban <uuid|ip|name> <value>"));
            return;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "reload":
                handleReload(source);
                break;
            case "ban":
                if (args.length < 3) {
                    source.sendMessage(Component.text("Usage: /ban <uuid|ip|name> <value>"));
                    return;
                }
                String type = args[1].toLowerCase();
                String value = args[2];
                handleBan(source, type, value);
                break;
            default:
                source.sendMessage(Component.text("Invalid command. Use /bantools reload OR /ban <uuid|ip|name> <value>"));
        }
    }

    private void handleReload(CommandSource source) {
        try {
            banManager.loadConfig();
            source.sendMessage(Component.text("Configuration reloaded successfully."));
            logger.info("Configuration reloaded by command.");
        } catch (Exception e) {
            source.sendMessage(Component.text("Failed to reload configuration."));
            logger.error("Failed to reload configuration", e);
        }
    }

    private void handleBan(CommandSource source, String type, String value) {
        try {
            switch (type) {
                case "uuid":
                    banManager.addBannedUuid(value);
                    break;
                case "ip":
                    banManager.addBannedIp(value);
                    break;
                case "name":
                    banManager.addBannedUsername(value);
                    break;
                default:
                    source.sendMessage(Component.text("Invalid type. Use 'uuid', 'ip', or 'name'."));
                    return;
            }
            source.sendMessage(Component.text("Successfully banned " + type + ": " + value));

            // 踢出在线玩家
            kickOnlinePlayers(value);

            logger.info("Banned " + type + ": " + value);
        } catch (Exception e) {
            source.sendMessage(Component.text("Failed to ban " + type + ": " + value));
            logger.error("Failed to ban " + type, e);
        }
    }

    private void kickOnlinePlayers(String value) {
        for (Player player : server.getAllPlayers()) {
            if (player.getUniqueId().toString().equals(value) ||
                    player.getRemoteAddress().getAddress().getHostAddress().equals(value) ||
                    player.getUsername().equalsIgnoreCase(value)) {
                player.disconnect(net.kyori.adventure.text.Component.text("You have been banned."));
            }
        }
    }
}