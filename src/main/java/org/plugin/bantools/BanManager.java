package org.plugin.bantools;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class BanManager {
    private final ProxyServer server;
    private final Logger logger;
    private final ConfigManager configManager;
    private final Map<String, BanEntry> banEntries = new HashMap<>();

    public BanManager(ProxyServer server, Logger logger, ConfigManager configManager) {
        this.server = server;
        this.logger = logger;
        this.configManager = configManager;
        loadBans();
    }

    public void loadBans() {
        banEntries.clear();
        configManager.getBans().forEach((key, entry) -> {
            if (entry.getState() && !isExpired(entry)) {
                banEntries.put(key, entry);
            }
        });
        logger.info("加载了 " + banEntries.size() + " 个有效封禁记录");
    }

    public boolean isBanned(String uuid, String ip, String username) {
        return banEntries.values().stream()
                .filter(entry -> !isExpired(entry))
                .anyMatch(entry -> {
                    // 优先检查玩家名（最可靠的标识符）
                    if (entry.getName().equalsIgnoreCase(username)) {
                        // 如果是离线封禁（UUID或IP为null），更新信息
                        if ((entry.getUuid() == null || entry.getIp() == null) &&
                            uuid != null && !uuid.isEmpty() && ip != null && !ip.isEmpty()) {
                            updateBanEntryInfo(entry, uuid, ip);
                        }
                        return true;
                    }
                    // 只有当UUID和IP不为null且不为空时才进行匹配
                    return (entry.getUuid() != null && entry.getUuid().equals(uuid)) ||
                           (entry.getIp() != null && entry.getIp().equals(ip));
                });
    }

    private void updateBanEntryInfo(BanEntry entry, String uuid, String ip) {
        try {
            entry.setUuid(uuid);
            entry.setIp(ip);
            configManager.updateBanEntry(entry);
            logger.info("更新了玩家 " + entry.getName() + " 的封禁信息");
        } catch (Exception e) {
            logger.error("更新封禁信息失败", e);
        }
    }

    public String getBanMessage(String uuid, String ip, String username) {
        BanEntry entry = findBanEntry(uuid, ip, username);
        if (entry == null) return "";

        String reason = entry.getReason();
        if (entry.isPermanent()) {
            return "§c你已被永久封禁！\n原因：" + reason;
        } else {
            return String.format("§c你已被封禁至 %s\n原因：%s",
                    entry.getEndTimeFormatted(),
                    reason);
        }
    }

    public void banPlayer(String target, String reason, String duration) {
        // 输入验证
        if (target == null || target.trim().isEmpty()) {
            logger.warn("尝试封禁空的玩家名");
            return;
        }
        if (target.length() > 16 || !target.matches("^[a-zA-Z0-9_]{1,16}$")) {
            logger.warn("无效的玩家名格式: " + target);
            return;
        }

        Player player = server.getPlayer(target).orElse(null);
        BanEntry entry = new BanEntry();

        entry.setName(target);
        // 改进离线玩家处理 - 如果玩家不在线，只记录玩家名，UUID和IP在玩家登录时验证
        if (player != null) {
            entry.setUuid(player.getUniqueId().toString());
            entry.setIp(player.getRemoteAddress().getAddress().getHostAddress());
        } else {
            entry.setUuid(null); // null表示未知，登录时会更新
            entry.setIp(null);
            logger.info("封禁离线玩家: " + target + "，UUID和IP将在玩家下次登录时更新");
        }
        entry.setReason(reason == null || reason.trim().isEmpty() ? configManager.getDefaultBanReason() : reason.trim());

        // 处理封禁时间（默认永久）
        if (duration == null || duration.isEmpty() || duration.equalsIgnoreCase("permanent")) {
            entry.setEndTime(null); // 永久封禁
        } else {
            entry.setStartTime(System.currentTimeMillis());
            entry.setEndTime(parseDuration(duration));
        }

        configManager.addBan(entry);
        loadBans();
        kickPlayer(target, entry.getReason());
    }

    private long parseDuration(String duration) {
        try {
            if (duration.endsWith("d")) {
                String dayStr = duration.replace("d", "");
                int days = Integer.parseInt(dayStr);
                if (days <= 0 || days > 3650) { // 最多10年
                    logger.warn("无效的封禁天数: " + days);
                    return System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1); // 默认1天
                }
                return System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days);
            } else if (duration.contains("-")) {
                String[] dates = duration.split("-");
                if (dates.length != 2) {
                    logger.warn("无效的日期范围格式: " + duration);
                    return System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1);
                }
                return parseAbsoluteDate(dates[1]);
            }
        } catch (NumberFormatException e) {
            logger.warn("解析封禁时长失败: " + duration, e);
        }
        return System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1); // 默认1天
    }

    private long parseAbsoluteDate(String dateStr) {
        try {
            return Instant.from(DateTimeFormatter.ofPattern("yyyy/MM/dd")
                    .parse(dateStr)).toEpochMilli();
        } catch (Exception e) {
            logger.warn("解析日期失败: " + dateStr, e);
            return System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1); // 默认1天
        }
    }

    public void unbanPlayer(String target) {
        configManager.setBanState(target, false);
        loadBans();
    }

    public void kickPlayer(String target, String reason) {
        server.getAllPlayers().stream()
                .filter(p -> p.getUsername().equalsIgnoreCase(target))
                .forEach(p -> p.disconnect(Component.text("§c" + reason)));
    }

    private BanEntry findBanEntry(String uuid, String ip, String username) {
        return banEntries.values().stream()
                .filter(entry -> !isExpired(entry))
                .filter(entry ->
                        entry.getUuid().equals(uuid) ||
                                entry.getIp().equals(ip) ||
                                entry.getName().equalsIgnoreCase(username)
                )
                .findFirst()
                .orElse(null);
    }

    private boolean isExpired(BanEntry entry) {
        return !entry.isPermanent() && entry.getEndTime() < System.currentTimeMillis();
    }
}