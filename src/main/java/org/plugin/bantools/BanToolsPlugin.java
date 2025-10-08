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
        // 初始化配置管理器
        configManager = new ConfigManager();

        // 初始化白名单管理器
        whitelistManager = new WhitelistManager(configManager, logger);

        // 初始化封禁管理器
        banManager = new BanManager(server, logger, configManager, whitelistManager);

        // 初始化临时封禁管理器
        fakeBanManager = new FakeBanManager(configManager, whitelistManager, server, logger);

        // 设置循环依赖
        banManager.setFakeBanManager(fakeBanManager);

        // 注册事件监听器
        server.getEventManager().register(this, new LoginListener(banManager));

        // 注册命令
        registerCommands();

        logger.info("===================================");
        logger.info("BanTools v1.4.0 已加载");
        logger.info("作者：NSrank & Qwen2.5-Max & Augment");
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