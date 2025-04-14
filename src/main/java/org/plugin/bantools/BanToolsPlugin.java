package org.plugin.bantools;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

@Plugin(id = "bantools", name = "BanTools", version = "1.0", description = "Advanced banning plugin for Velocity")
public class BanToolsPlugin {

    @Inject
    private ProxyServer server;

    @Inject
    private Logger logger;

    private BanManager banManager;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // 初始化封禁管理器
        this.banManager = new BanManager(server, logger);

        // 注册事件监听器
        server.getEventManager().register(this, new LoginListener(banManager));

        // 注册命令
        registerCommands();

        logger.info("BanTools has been enabled!");
    }

    private void registerCommands() {
        CommandManager commandManager = server.getCommandManager();

        // 创建命令元数据
        CommandMeta meta = commandManager.metaBuilder("bantools")
                .aliases("bt")
                .build();

        // 注册命令
        commandManager.register(meta, new BanToolsCommand(banManager, server, logger));
    }

    public BanManager getBanManager() {
        return banManager;
    }
}