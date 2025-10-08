package org.plugin.bantools;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 临时封禁管理器
 * 管理fakeban功能，包括确认机制和自动过期清理
 */
public class FakeBanManager {
    private final ConfigManager configManager;
    private final WhitelistManager whitelistManager;
    private final ProxyServer server;
    private final Logger logger;
    private final ScheduledExecutorService scheduler;
    
    // 存储待确认的fakeban操作
    private final Map<String, PendingFakeBan> pendingFakeBans = new ConcurrentHashMap<>();
    // 存储活跃的临时封禁记录
    private final Map<String, FakeBanEntry> activeFakeBans = new ConcurrentHashMap<>();

    public FakeBanManager(ConfigManager configManager, WhitelistManager whitelistManager, 
                         ProxyServer server, Logger logger) {
        this.configManager = configManager;
        this.whitelistManager = whitelistManager;
        this.server = server;
        this.logger = logger;
        this.scheduler = Executors.newScheduledThreadPool(2);
        
        loadActiveFakeBans();
        startCleanupTask();
    }

    /**
     * 待确认的fakeban操作
     */
    private static class PendingFakeBan {
        final String adminName;
        final String targetPlayer;
        final String reason;
        final long expireTime;

        PendingFakeBan(String adminName, String targetPlayer, String reason, long expireTime) {
            this.adminName = adminName;
            this.targetPlayer = targetPlayer;
            this.reason = reason;
            this.expireTime = expireTime;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }

    /**
     * 发起fakeban操作（第一次执行）
     * @param adminName 管理员名称
     * @param targetPlayer 目标玩家
     * @param reason 封禁原因
     * @return 确认消息或错误信息
     */
    public String initiateFakeBan(String adminName, String targetPlayer, String reason) {
        // 输入验证
        if (targetPlayer == null || targetPlayer.trim().isEmpty()) {
            return "玩家名不能为空";
        }
        if (targetPlayer.length() > 16 || !targetPlayer.matches("^[a-zA-Z0-9_]{1,16}$")) {
            return "无效的玩家名格式";
        }

        // 白名单保护检查
        String protectionCheck = whitelistManager.checkProtection(targetPlayer);
        if (protectionCheck != null) {
            return protectionCheck;
        }

        // 检查是否已存在有效的临时封禁
        FakeBanEntry existingFakeBan = findActiveFakeBan(targetPlayer);
        if (existingFakeBan != null) {
            return "该玩家已被临时封禁！剩余时间：" + existingFakeBan.getRemainingTimeFormatted();
        }

        // 检查是否已存在普通封禁
        // 这里需要与BanManager集成，暂时跳过

        // 创建待确认的操作
        String pendingKey = adminName + ":" + targetPlayer;
        long timeoutMinutes = configManager.getFakeBanConfirmationTimeoutMinutes();
        long expireTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(timeoutMinutes);
        
        String finalReason = (reason == null || reason.trim().isEmpty()) ? 
                configManager.getDefaultFakeBanReason() : reason.trim();
        
        pendingFakeBans.put(pendingKey, new PendingFakeBan(adminName, targetPlayer, finalReason, expireTime));
        
        // 设置自动清理
        scheduler.schedule(() -> {
            pendingFakeBans.remove(pendingKey);
            logger.info("管理员 " + adminName + " 的fakeban确认已超时：" + targetPlayer);
        }, timeoutMinutes, TimeUnit.MINUTES);

        return configManager.getFakeBanConfirmationMessage();
    }

    /**
     * 确认并执行fakeban操作（第二次执行相同命令）
     * @param adminName 管理员名称
     * @param targetPlayer 目标玩家
     * @param reason 封禁原因
     * @return 执行结果消息
     */
    public String confirmFakeBan(String adminName, String targetPlayer, String reason) {
        String pendingKey = adminName + ":" + targetPlayer;
        PendingFakeBan pending = pendingFakeBans.get(pendingKey);
        
        if (pending == null) {
            return initiateFakeBan(adminName, targetPlayer, reason);
        }

        if (pending.isExpired()) {
            pendingFakeBans.remove(pendingKey);
            return initiateFakeBan(adminName, targetPlayer, reason);
        }

        // 执行fakeban
        pendingFakeBans.remove(pendingKey);
        return executeFakeBan(targetPlayer, pending.reason);
    }

    /**
     * 执行临时封禁
     */
    private String executeFakeBan(String targetPlayer, String reason) {
        try {
            // 创建临时封禁记录
            long durationMinutes = configManager.getFakeBanDurationMinutes();
            long durationMs = TimeUnit.MINUTES.toMillis(durationMinutes);
            
            FakeBanEntry fakeBanEntry = new FakeBanEntry(targetPlayer, reason, durationMs);
            
            // 如果玩家在线，获取UUID和IP
            server.getPlayer(targetPlayer).ifPresent(player -> {
                fakeBanEntry.setUuid(player.getUniqueId().toString());
                fakeBanEntry.setIp(player.getRemoteAddress().getAddress().getHostAddress());
            });

            // 保存到配置文件
            configManager.addFakeBan(fakeBanEntry);
            
            // 添加到活跃列表
            activeFakeBans.put(targetPlayer, fakeBanEntry);

            // 踢出在线玩家
            kickPlayer(targetPlayer, reason);

            logger.info("成功临时封禁玩家: " + targetPlayer + "，时长: " + durationMinutes + "分钟");
            return "成功临时封禁玩家: " + targetPlayer + "，时长: " + durationMinutes + "分钟";
            
        } catch (Exception e) {
            logger.error("执行临时封禁失败: " + targetPlayer, e);
            return "执行临时封禁失败，请检查日志";
        }
    }

    /**
     * 解除临时封禁
     */
    public String unFakeBan(String targetPlayer) {
        // 输入验证
        if (targetPlayer == null || targetPlayer.trim().isEmpty()) {
            return "玩家名不能为空";
        }
        if (targetPlayer.length() > 16 || !targetPlayer.matches("^[a-zA-Z0-9_]{1,16}$")) {
            return "无效的玩家名格式";
        }

        FakeBanEntry fakeBan = findActiveFakeBan(targetPlayer);
        if (fakeBan == null) {
            return "该玩家没有有效的临时封禁记录！";
        }

        // 设置为非活跃状态
        configManager.setFakeBanState(targetPlayer, false);
        activeFakeBans.remove(targetPlayer);

        logger.info("成功解除临时封禁: " + targetPlayer);
        return "成功解除临时封禁: " + targetPlayer;
    }

    /**
     * 检查玩家是否被临时封禁
     */
    public boolean isFakeBanned(String uuid, String ip, String username) {
        return activeFakeBans.values().stream()
                .filter(entry -> !entry.isExpired())
                .anyMatch(entry -> {
                    // 优先检查玩家名
                    if (entry.getName().equalsIgnoreCase(username)) {
                        return true;
                    }
                    // 检查UUID和IP（如果不为空）
                    return (!entry.getUuid().isEmpty() && entry.getUuid().equals(uuid)) ||
                           (!entry.getIp().isEmpty() && entry.getIp().equals(ip));
                });
    }

    /**
     * 获取临时封禁信息
     */
    public FakeBanEntry getFakeBanInfo(String uuid, String ip, String username) {
        return activeFakeBans.values().stream()
                .filter(entry -> !entry.isExpired())
                .filter(entry -> {
                    if (entry.getName().equalsIgnoreCase(username)) {
                        return true;
                    }
                    return (!entry.getUuid().isEmpty() && entry.getUuid().equals(uuid)) ||
                           (!entry.getIp().isEmpty() && entry.getIp().equals(ip));
                })
                .findFirst()
                .orElse(null);
    }

    /**
     * 查找活跃的临时封禁记录
     */
    private FakeBanEntry findActiveFakeBan(String targetPlayer) {
        return activeFakeBans.values().stream()
                .filter(entry -> entry.getName().equalsIgnoreCase(targetPlayer))
                .filter(entry -> !entry.isExpired())
                .findFirst()
                .orElse(null);
    }

    /**
     * 踢出玩家
     */
    private void kickPlayer(String targetPlayer, String reason) {
        server.getPlayer(targetPlayer).ifPresent(player -> {
            Component kickMessage = Component.text(reason);
            player.disconnect(kickMessage);
            logger.info("已踢出玩家: " + targetPlayer + "，原因: " + reason);
        });
    }

    /**
     * 加载活跃的临时封禁记录
     */
    private void loadActiveFakeBans() {
        activeFakeBans.clear();
        Map<String, FakeBanEntry> fakeBans = configManager.getFakeBans();
        
        for (FakeBanEntry entry : fakeBans.values()) {
            if (entry.getState() && !entry.isExpired()) {
                activeFakeBans.put(entry.getName(), entry);
            }
        }
        
        logger.info("加载了 " + activeFakeBans.size() + " 个活跃的临时封禁记录");
    }

    /**
     * 启动清理任务
     */
    private void startCleanupTask() {
        // 每分钟清理一次过期的临时封禁
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // 清理过期的待确认操作
                pendingFakeBans.entrySet().removeIf(entry -> entry.getValue().isExpired());
                
                // 清理过期的临时封禁
                configManager.cleanupExpiredFakeBans();
                loadActiveFakeBans();
                
            } catch (Exception e) {
                logger.error("清理过期临时封禁时发生错误", e);
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * 关闭管理器
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 获取所有被临时封禁的玩家名列表
     */
    public List<String> getFakeBannedPlayers() {
        return activeFakeBans.values().stream()
                .filter(entry -> !entry.isExpired())
                .map(FakeBanEntry::getName)
                .collect(Collectors.toList());
    }
}
