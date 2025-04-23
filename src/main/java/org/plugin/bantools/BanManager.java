package org.plugin.bantools;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
                .anyMatch(entry ->
                        entry.getUuid().equals(uuid) ||
                                entry.getIp().equals(ip) ||
                                entry.getName().equalsIgnoreCase(username)
                );
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
        Player player = server.getPlayer(target).orElse(null);
        BanEntry entry = new BanEntry();

        entry.setName(target);
        entry.setUuid(player != null ? player.getUniqueId().toString() : "unknown");
        entry.setIp(player != null ? player.getRemoteAddress().getAddress().getHostAddress() : "unknown");
        entry.setReason(reason.isEmpty() ? configManager.getDefaultBanReason() : reason);

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
        if (duration.endsWith("d")) {
            return System.currentTimeMillis() +
                    TimeUnit.DAYS.toMillis(Integer.parseInt(duration.replace("d", "")));
        } else if (duration.contains("-")) {
            String[] dates = duration.split("-");
            return parseAbsoluteDate(dates[1]);
        }
        return System.currentTimeMillis();
    }

    private long parseAbsoluteDate(String dateStr) {
        return Instant.from(DateTimeFormatter.ofPattern("yyyy/MM/dd")
                .parse(dateStr)).toEpochMilli();
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