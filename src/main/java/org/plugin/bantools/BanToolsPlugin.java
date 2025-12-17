package org.plugin.bantools;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
        id = "bantools",
        name = "BanTools",
        version = "1.4.0",
        description = "Advanced banning system for Velocity"
)
public class BanToolsPlugin {
    @Inject private ProxyServer server;
    @Inject private Logger logger;
    @Inject @DataDirectory private Path dataDirectory;
    private ConfigManager configManager;
    private WhitelistManager whitelistManager;
    private BanManager banManager;
    private FakeBanManager fakeBanManager;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // Initialize configuration manager
        configManager = new ConfigManager();

        // Initialize whitelist manager
        whitelistManager = new WhitelistManager(configManager, logger);

        // Initialize ban manager
        banManager = new BanManager(server, logger, configManager, whitelistManager);

        // Initialize fake ban manager
        fakeBanManager = new FakeBanManager(configManager, whitelistManager, server, logger);

        // Set cyclic dependency
        banManager.setFakeBanManager(fakeBanManager);

        // Register event listener
        server.getEventManager().register(this, new LoginListener(banManager));

        // Register commands
        registerCommands();

        logger.info("===================================");
        logger.info("BanTools v1.4.0 loaded");
        logger.info("Authors: NSrank & Qwen2.5-Max & Augment");
        logger.info("===================================");
    }

    private void registerCommands() {
        CommandManager commandManager = server.getCommandManager();

        CommandMeta meta = commandManager.metaBuilder("bantools")
                .aliases("bt")
                .build();
        commandManager.register(meta, new BanToolsCommand(banManager, configManager, fakeBanManager, server));
    }
}