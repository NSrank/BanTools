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
    private FakeBanManager fakeBanManager; // Delayed initialization to avoid circular dependencies.
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
     * Set the FakeBanManager (deferred initialization)
     */
    public void setFakeBanManager(FakeBanManager fakeBanManager) {
        this.fakeBanManager = fakeBanManager;
    }

    /**
     * Get a list of all banned player names
     */
    public List<String> getBannedPlayers() {
        return banEntries.values().stream()
                .filter(entry -> entry.getState() && !isExpired(entry))
                .map(BanEntry::getName)
                .collect(Collectors.toList());
    }



    /**
     * Check if a player is whitelisted
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
        logger.info("Loaded " + banEntries.size() + " valid ban entries");
    }

    public boolean isBanned(String uuid, String ip, String username) {
        // Check normal bans
        boolean normalBan = banEntries.values().stream()
                .filter(entry -> !isExpired(entry))
                .anyMatch(entry -> {
                    // Prefer checking player name (most reliable identifier)
                    if (entry.getName().equalsIgnoreCase(username)) {
                        // If it's an offline ban (UUID or IP is null), update info
                        if ((entry.getUuid() == null || entry.getIp() == null) &&
                            uuid != null && !uuid.isEmpty() && ip != null && !ip.isEmpty()) {
                            updateBanEntryInfo(entry, uuid, ip);
                        }
                        return true;
                    }
                    // Only match when UUID and IP are non-null and non-empty
                    return (entry.getUuid() != null && entry.getUuid().equals(uuid)) ||
                           (entry.getIp() != null && entry.getIp().equals(ip));
                });

        // Check temporary bans
        boolean fakeBan = fakeBanManager != null && fakeBanManager.isFakeBanned(uuid, ip, username);

        return normalBan || fakeBan;
    }

    private void updateBanEntryInfo(BanEntry entry, String uuid, String ip) {
        try {
            entry.setUuid(uuid);
            entry.setIp(ip);
            configManager.updateBanEntry(entry);
            logger.info("Updated ban info for player " + entry.getName());
        } catch (Exception e) {
            logger.error("Failed to update ban info", e);
        }
    }

    public String getBanMessage(String uuid, String ip, String username) {
        // Check normal bans
        BanEntry entry = findBanEntry(uuid, ip, username);
        if (entry != null) {
            String reason = entry.getReason();
            if (entry.isPermanent()) {
                return "§cYou have been permanently banned!\nReason: " + reason;
            } else {
                return String.format("§cYou are banned until %s\nReason: %s",
                        entry.getEndTimeFormatted(),
                        reason);
            }
        }

        // Check temporary bans
        if (fakeBanManager != null) {
            FakeBanEntry fakeBanEntry = fakeBanManager.getFakeBanInfo(uuid, ip, username);
            if (fakeBanEntry != null) {
                return String.format("§cYou have been temporarily banned!\nReason: %s\nTime remaining: %s",
                        fakeBanEntry.getReason(),
                        fakeBanEntry.getRemainingTimeFormatted());
            }
        }

        return "";
    }

    public String banPlayer(String target, String reason, String duration) {
        // Input validation
        if (target == null || target.trim().isEmpty()) {
            logger.warn("Attempted to ban an empty player name");
            return "Player name cannot be empty";
        }
        if (target.length() > 16 || !target.matches("^[a-zA-Z0-9_]{1,16}$")) {
            logger.warn("Invalid player name format: " + target);
            return "Invalid player name format";
        }

        // Whitelist protection check
        String protectionCheck = whitelistManager.checkProtection(target);
        if (protectionCheck != null) {
            logger.warn("Attempted to ban a protected player: " + target);
            return protectionCheck;
        }

        // Check if already banned
        BanEntry existingBan = findExistingBan(target);
        if (existingBan != null) {
            String banInfo = formatExistingBanInfo(existingBan);
            logger.info("Attempted to ban already banned player: " + target + " - ban already exists");
            return "Player is already banned! " + banInfo;
        }

        Player player = server.getPlayer(target).orElse(null);
        BanEntry entry = new BanEntry();

        entry.setName(target);
        // Improved offline player handling - if the player is not online, only record the name; UUID and IP will be validated on next login
        if (player != null) {
            entry.setUuid(player.getUniqueId().toString());
            entry.setIp(player.getRemoteAddress().getAddress().getHostAddress());
        } else {
            entry.setUuid(null); // null means unknown, will be updated on login
            entry.setIp(null);
            logger.info("Banned offline player: " + target + "; UUID and IP will be updated on next login");
        }
        entry.setReason(reason == null || reason.trim().isEmpty() ? configManager.getDefaultBanReason() : reason.trim());
        entry.setStartTime(System.currentTimeMillis());
        entry.setState(true); // Ensure ban state is active

        // Handle ban duration (default: permanent)
        if (duration == null || duration.isEmpty() || duration.equalsIgnoreCase("permanent")) {
            entry.setEndTime(null); // Permanent ban
        } else {
            entry.setEndTime(parseDuration(duration));
        }

        configManager.addBan(entry);
        // ConfigManager.addBan() already called loadBans(); call loadBans() here to synchronize BanManager's data
        loadBans();
        kickPlayer(target, entry.getReason());
        return null; // Successfully banned, return null to indicate no error
    }

    private long parseDuration(String duration) {
        try {
            if (duration.endsWith("d")) {
                String dayStr = duration.replace("d", "");
                int days = Integer.parseInt(dayStr);
                if (days <= 0 || days > 3650) { // up to 10 years
                    logger.warn("Invalid ban days: " + days);
                    return System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1); // default 1 day
                }
                return System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days);
            } else if (duration.contains("-")) {
                String[] dates = duration.split("-");
                if (dates.length != 2) {
                    logger.warn("Invalid date range format: " + duration);
                    return System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1);
                }
                return parseAbsoluteDate(dates[1]);
            }
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse ban duration: " + duration, e);
        }
        return System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1); // default 1 day
    }

    private long parseAbsoluteDate(String dateStr) {
        try {
            return Instant.from(DateTimeFormatter.ofPattern("yyyy/MM/dd")
                    .parse(dateStr)).toEpochMilli();
        } catch (Exception e) {
            logger.warn("Failed to parse date: " + dateStr, e);
            return System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1); // default 1 day
        }
    }

    public String unbanPlayer(String target) {
        // Input validation
        if (target == null || target.trim().isEmpty()) {
            logger.warn("Attempted to unban an empty player name");
            return "Player name cannot be empty";
        }
        if (target.length() > 16 || !target.matches("^[a-zA-Z0-9_]{1,16}$")) {
            logger.warn("Invalid player name format: " + target);
            return "Invalid player name format";
        }

        // Check if an active ban record exists
        BanEntry existingBan = findExistingBan(target);
        if (existingBan == null) {
            // Check if an inactive/unbanned record exists
            BanEntry inactiveBan = findInactiveBan(target);
            if (inactiveBan != null) {
                logger.info("Attempted to unban player already unbanned: " + target);
                return "Player is not banned or already unbanned!";
            } else {
                logger.info("Attempted to unban a non-existent player: " + target);
                return "This player has no ban record!";
            }
        }

        configManager.setBanState(target, false);
        loadBans();
        logger.info("Successfully unbanned player: " + target);
        return null; // Successfully unbanned, return null to indicate no error
    }

    public String kickPlayer(String target, String reason) {
        // Input validation
        if (target == null || target.trim().isEmpty()) {
            logger.warn("Attempted to kick an empty player name");
            return "Player name cannot be empty";
        }
        if (target.length() > 16 || !target.matches("^[a-zA-Z0-9_]{1,16}$")) {
            logger.warn("Invalid player name format: " + target);
            return "Invalid player name format";
        }

        // Whitelist protection check
        String protectionCheck = whitelistManager.checkProtection(target);
        if (protectionCheck != null) {
            logger.warn("Attempted to kick a protected player: " + target);
            return protectionCheck;
        }

        server.getAllPlayers().stream()
                .filter(p -> p.getUsername().equalsIgnoreCase(target))
                .forEach(p -> p.disconnect(Component.text("§c" + reason)));

        logger.info("Kicked player: " + target + ", reason: " + reason);
        return null; // Successfully kicked, return null to indicate no error
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
     * Find existing ban record for a specified player
     * @param target player name
     * @return BanEntry if an active ban is found, otherwise null
     */
    private BanEntry findExistingBan(String target) {
        // First check active bans in memory
        for (BanEntry entry : banEntries.values()) {
            if (entry.getName().equalsIgnoreCase(target) && !isExpired(entry)) {
                return entry;
            }
        }

        // Check all ban records in the config (including unbanned ones)
        Map<String, BanEntry> allBans = configManager.getBans();
        for (BanEntry entry : allBans.values()) {
            if (entry.getName().equalsIgnoreCase(target) && entry.getState() && !isExpired(entry)) {
                return entry;
            }
        }

        return null;
    }

    /**
     * Find inactive (unbanned) record for a specified player
     * @param target player name
     * @return BanEntry if an inactive ban record is found, otherwise null
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
     * Format existing ban info into a user-friendly string
     * @param banEntry ban record
     * @return formatted ban info string
     */
    private String formatExistingBanInfo(BanEntry banEntry) {
        StringBuilder info = new StringBuilder();
        info.append("Reason: ").append(banEntry.getReason());

        if (banEntry.isPermanent()) {
            info.append(", duration: permanent ban");
        } else {
            info.append(", duration: until ").append(banEntry.getEndTimeFormatted());
        }

        return info.toString();
    }
}