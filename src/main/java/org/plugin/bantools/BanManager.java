package org.plugin.bantools;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class BanManager {
    private final ProxyServer server;
    private final Logger logger;
    private final ConfigManager configManager;
    private final WhitelistManager whitelistManager;
    private FakeBanManager fakeBanManager; // 延迟初始化，避免循环依赖
    private final Map<String, BanEntry> banEntries = new HashMap<>();

    public BanManager(ProxyServer server, Logger logger, ConfigManager configManager,
                     WhitelistManager whitelistManager) {
        this.server = server;
        this.logger = logger;
        this.configManager = configManager;
        this.whitelistManager = whitelistManager;
        loadBans();
    }

    /**
     * 设置FakeBanManager（延迟初始化）
     */
    public void setFakeBanManager(FakeBanManager fakeBanManager) {
        this.fakeBanManager = fakeBanManager;
    }

    /**
     * 获取所有被封禁的玩家名列表
     */
    public List<String> getBannedPlayers() {
        return banEntries.values().stream()
                .filter(entry -> entry.getState() && !isExpired(entry))
                .map(BanEntry::getName)
                .collect(Collectors.toList());
    }



    /**
     * 检查玩家是否在白名单中
     */
    public boolean isWhitelisted(String playerName) {
        return whitelistManager.isWhitelisted(playerName);
    }

    public void loadBans() {
        banEntries.clear();
        Map<String, BanEntry> allBans = configManager.getBans();

        allBans.forEach((key, entry) -> {
            if (entry.getState() && !isExpired(entry)) {
                banEntries.put(key, entry);
            }
        });
        logger.info("加载了 " + banEntries.size() + " 个有效封禁记录");
    }

    public boolean isBanned(String uuid, String ip, String username) {
        // 检查普通封禁
        boolean normalBan = banEntries.values().stream()
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

        // 检查临时封禁
        boolean fakeBan = fakeBanManager != null && fakeBanManager.isFakeBanned(uuid, ip, username);

        return normalBan || fakeBan;
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
        // 检查普通封禁
        BanEntry entry = findBanEntry(uuid, ip, username);
        if (entry != null) {
            String reason = entry.getReason();
            if (entry.isPermanent()) {
                return "§c你已被永久封禁！\n原因：" + reason;
            } else {
                return String.format("§c你已被封禁至 %s\n原因：%s",
                        entry.getEndTimeFormatted(),
                        reason);
            }
        }

        // 检查临时封禁
        if (fakeBanManager != null) {
            FakeBanEntry fakeBanEntry = fakeBanManager.getFakeBanInfo(uuid, ip, username);
            if (fakeBanEntry != null) {
                return String.format("§c你已被临时封禁！\n原因：%s\n剩余时间：%s",
                        fakeBanEntry.getReason(),
                        fakeBanEntry.getRemainingTimeFormatted());
            }
        }

        return "";
    }

    public String banPlayer(String target, String reason, String duration) {
        // 输入验证
        if (target == null || target.trim().isEmpty()) {
            logger.warn("尝试封禁空的玩家名");
            return "玩家名不能为空";
        }
        if (target.length() > 16 || !target.matches("^[a-zA-Z0-9_]{1,16}$")) {
            logger.warn("无效的玩家名格式: " + target);
            return "无效的玩家名格式";
        }

        // 白名单保护检查
        String protectionCheck = whitelistManager.checkProtection(target);
        if (protectionCheck != null) {
            logger.warn("尝试封禁受保护的玩家: " + target);
            return protectionCheck;
        }

        // 检查是否已经被封禁
        BanEntry existingBan = findExistingBan(target);
        if (existingBan != null) {
            String banInfo = formatExistingBanInfo(existingBan);
            logger.info("尝试重复封禁玩家: " + target + "，已存在封禁记录");
            return "该玩家已被封禁！" + banInfo;
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
        entry.setStartTime(System.currentTimeMillis());
        entry.setState(true); // 确保封禁状态为激活

        // 处理封禁时间（默认永久）
        if (duration == null || duration.isEmpty() || duration.equalsIgnoreCase("permanent")) {
            entry.setEndTime(null); // 永久封禁
        } else {
            entry.setEndTime(parseDuration(duration));
        }

        configManager.addBan(entry);
        // ConfigManager.addBan() 已经调用了 loadBans()，这里调用 loadBans() 来同步 BanManager 的数据
        loadBans();
        kickPlayer(target, entry.getReason());
        return null; // 成功封禁，返回null表示没有错误
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

    public String unbanPlayer(String target) {
        // 输入验证
        if (target == null || target.trim().isEmpty()) {
            logger.warn("尝试解封空的玩家名");
            return "玩家名不能为空";
        }
        if (target.length() > 16 || !target.matches("^[a-zA-Z0-9_]{1,16}$")) {
            logger.warn("无效的玩家名格式: " + target);
            return "无效的玩家名格式";
        }

        // 检查是否存在有效的封禁记录
        BanEntry existingBan = findExistingBan(target);
        if (existingBan == null) {
            // 检查是否存在已解封的记录
            BanEntry inactiveBan = findInactiveBan(target);
            if (inactiveBan != null) {
                logger.info("尝试重复解封玩家: " + target + "，该玩家已处于解封状态");
                return "该玩家未被封禁或已被解封！";
            } else {
                logger.info("尝试解封不存在的玩家: " + target);
                return "该玩家没有封禁记录！";
            }
        }

        configManager.setBanState(target, false);
        loadBans();
        logger.info("成功解封玩家: " + target);
        return null; // 成功解封，返回null表示没有错误
    }

    public String kickPlayer(String target, String reason) {
        // 输入验证
        if (target == null || target.trim().isEmpty()) {
            logger.warn("尝试踢出空的玩家名");
            return "玩家名不能为空";
        }
        if (target.length() > 16 || !target.matches("^[a-zA-Z0-9_]{1,16}$")) {
            logger.warn("无效的玩家名格式: " + target);
            return "无效的玩家名格式";
        }

        // 白名单保护检查
        String protectionCheck = whitelistManager.checkProtection(target);
        if (protectionCheck != null) {
            logger.warn("尝试踢出受保护的玩家: " + target);
            return protectionCheck;
        }

        server.getAllPlayers().stream()
                .filter(p -> p.getUsername().equalsIgnoreCase(target))
                .forEach(p -> p.disconnect(Component.text("§c" + reason)));

        logger.info("已踢出玩家: " + target + "，原因: " + reason);
        return null; // 成功踢出，返回null表示没有错误
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

    /**
     * 查找指定玩家的现有封禁记录
     * @param target 玩家名
     * @return 如果找到有效的封禁记录则返回BanEntry，否则返回null
     */
    private BanEntry findExistingBan(String target) {
        // 首先检查内存中的活跃封禁记录
        for (BanEntry entry : banEntries.values()) {
            if (entry.getName().equalsIgnoreCase(target) && !isExpired(entry)) {
                return entry;
            }
        }

        // 检查配置文件中的所有封禁记录（包括已解封的）
        Map<String, BanEntry> allBans = configManager.getBans();
        for (BanEntry entry : allBans.values()) {
            if (entry.getName().equalsIgnoreCase(target) && entry.getState() && !isExpired(entry)) {
                return entry;
            }
        }

        return null;
    }

    /**
     * 查找指定玩家的已解封记录
     * @param target 玩家名
     * @return 如果找到已解封的记录则返回BanEntry，否则返回null
     */
    private BanEntry findInactiveBan(String target) {
        Map<String, BanEntry> allBans = configManager.getBans();
        for (BanEntry entry : allBans.values()) {
            if (entry.getName().equalsIgnoreCase(target) && !entry.getState()) {
                return entry;
            }
        }
        return null;
    }

    /**
     * 格式化现有封禁信息为用户友好的字符串
     * @param banEntry 封禁记录
     * @return 格式化的封禁信息字符串
     */
    private String formatExistingBanInfo(BanEntry banEntry) {
        StringBuilder info = new StringBuilder();
        info.append("理由：").append(banEntry.getReason());

        if (banEntry.isPermanent()) {
            info.append("，时长：永久封禁");
        } else {
            info.append("，时长：至 ").append(banEntry.getEndTimeFormatted());
        }

        return info.toString();
    }
}