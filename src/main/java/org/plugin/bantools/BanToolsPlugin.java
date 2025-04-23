package org.plugin.bantools;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

@Plugin(
        id = "bantools",
        name = "BanTools",
        version = "1.2",
        description = "Advanced banning system for Velocity"
)
public class BanToolsPlugin {
    @Inject private ProxyServer server;
    @Inject private Logger logger;
    private BanManager banManager;
    private ConfigManager configManager;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        configManager = new ConfigManager();
        banManager = new BanManager(server, logger, configManager);

        server.getEventManager().register(this, new LoginListener(banManager));
        registerCommands();

        logger.info("===================================");
        logger.info("BanTools v1.2 已加载");
        logger.info("作者：NSrank & Qwen2.5-Max");
        logger.info("===================================");
    }

    private void registerCommands() {
        CommandManager commandManager = server.getCommandManager();

        CommandMeta meta = commandManager.metaBuilder("bantools")
                .aliases("bt")
                .build();
        commandManager.register(meta, new BanToolsCommand(banManager, configManager));

        CommandMeta unbanMeta = commandManager.metaBuilder("unban")
                .build();
        commandManager.register(unbanMeta, new UnbanCommand(banManager));
    }
}