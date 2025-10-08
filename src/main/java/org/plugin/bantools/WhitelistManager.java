package org.plugin.bantools;

import org.slf4j.Logger;

import java.util.*;

/**
 * 白名单管理器
 * 管理受保护的玩家列表，防止管理员被恶意封禁
 */
public class WhitelistManager {
    private final ConfigManager configManager;
    private final Logger logger;

    private boolean enabled;
    private Set<String> whitelist;
    private String protectionMessage;

    public WhitelistManager(ConfigManager configManager, Logger logger) {
        this.configManager = configManager;
        this.logger = logger;
        this.whitelist = new HashSet<>();
        this.protectionMessage = "该玩家受到白名单保护，无法执行此操作！";

        loadWhitelist();
    }

    /**
     * 加载白名单配置（从主配置文件）
     */
    public void loadWhitelist() {
        try {
            enabled = configManager.isWhitelistEnabled();
            protectionMessage = configManager.getWhitelistProtectionMessage();

            List<String> whitelistPlayers = configManager.getWhitelistPlayers();
            whitelist.clear();
            if (whitelistPlayers != null) {
                whitelist.addAll(whitelistPlayers);
            }

            logger.info("白名单配置已加载，状态: " + (enabled ? "启用" : "禁用") +
                       "，保护玩家数量: " + whitelist.size());

        } catch (Exception e) {
            logger.error("加载白名单配置失败", e);
            // 使用默认配置
            enabled = true;
            protectionMessage = "该玩家受到白名单保护，无法执行此操作！";
            whitelist.clear();
            whitelist.addAll(Arrays.asList("Admin", "Owner"));
        }
    }



    /**
     * 检查玩家是否在白名单中
     */
    public boolean isWhitelisted(String playerName) {
        if (!enabled || playerName == null) {
            return false;
        }
        return whitelist.contains(playerName);
    }

    /**
     * 检查是否可以对玩家执行操作
     * @param playerName 玩家名
     * @return 如果可以执行操作返回null，否则返回保护消息
     */
    public String checkProtection(String playerName) {
        if (isWhitelisted(playerName)) {
            return protectionMessage;
        }
        return null;
    }

    /**
     * 添加玩家到白名单
     * 注意：此方法只修改内存中的白名单，需要手动编辑配置文件来永久保存
     */
    public boolean addToWhitelist(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return false;
        }

        boolean added = whitelist.add(playerName.trim());
        if (added) {
            logger.info("玩家 " + playerName + " 已添加到内存白名单（需要手动编辑配置文件来永久保存）");
        }
        return added;
    }

    /**
     * 从白名单移除玩家
     * 注意：此方法只修改内存中的白名单，需要手动编辑配置文件来永久保存
     */
    public boolean removeFromWhitelist(String playerName) {
        boolean removed = whitelist.remove(playerName);
        if (removed) {
            logger.info("玩家 " + playerName + " 已从内存白名单移除（需要手动编辑配置文件来永久保存）");
        }
        return removed;
    }

    // Getters
    public boolean isEnabled() { return enabled; }
    public Set<String> getWhitelist() { return new HashSet<>(whitelist); }
    public String getProtectionMessage() { return protectionMessage; }
}
