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
 * Temporary ban manager
 * Manages the fakeban feature, including confirmation flow and automatic expiration cleanup
 */
public class FakeBanManager {
    private final ConfigManager configManager;
    private final WhitelistManager whitelistManager;
    private final ProxyServer server;
    private final Logger logger;
    private final ScheduledExecutorService scheduler;
    
    // Stores pending fakeban operations awaiting confirmation
    private final Map<String, PendingFakeBan> pendingFakeBans = new ConcurrentHashMap<>();
    // Stores active temporary ban records
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
     * Pending fakeban operation
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
     * Initiate a fakeban operation (first execution)
     * @param adminName administrator name
     * @param targetPlayer target player
     * @param reason ban reason
     * @return confirmation message or error
     */
    public String initiateFakeBan(String adminName, String targetPlayer, String reason) {
        // Input validation
        if (targetPlayer == null || targetPlayer.trim().isEmpty()) {
            return "Player name cannot be empty";
        }
        if (targetPlayer.length() > 16 || !targetPlayer.matches("^[a-zA-Z0-9_]{1,16}$")) {
            return "Invalid player name format";
        }

        // Whitelist protection check
        String protectionCheck = whitelistManager.checkProtection(targetPlayer);
        if (protectionCheck != null) {
            return protectionCheck;
        }

        // Check if an active fakeban already exists
        FakeBanEntry existingFakeBan = findActiveFakeBan(targetPlayer);
        if (existingFakeBan != null) {
            return "This player is already temporarily banned! Remaining time: " + existingFakeBan.getRemainingTimeFormatted();
        }

        // Check if a normal ban exists
        // This would require integration with BanManager; skipping for now

        // Create pending operation
        String pendingKey = adminName + ":" + targetPlayer;
        long timeoutMinutes = configManager.getFakeBanConfirmationTimeoutMinutes();
        long expireTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(timeoutMinutes);
        
        String finalReason = (reason == null || reason.trim().isEmpty()) ? 
                configManager.getDefaultFakeBanReason() : reason.trim();
        
        pendingFakeBans.put(pendingKey, new PendingFakeBan(adminName, targetPlayer, finalReason, expireTime));
        
        // Schedule automatic cleanup
        scheduler.schedule(() -> {
            pendingFakeBans.remove(pendingKey);
            logger.info("Fakeban confirmation for admin " + adminName + " timed out: " + targetPlayer);
        }, timeoutMinutes, TimeUnit.MINUTES);

        return configManager.getFakeBanConfirmationMessage();
    }

    /**
     * Confirm and execute a fakeban operation (second execution of the same command)
     * @param adminName administrator name
     * @param targetPlayer target player
     * @param reason ban reason
     * @return execution result message
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

        // Execute fakeban
        pendingFakeBans.remove(pendingKey);
        return executeFakeBan(targetPlayer, pending.reason);
    }

    /**
     * Execute a temporary ban
     */
    private String executeFakeBan(String targetPlayer, String reason) {
        try {
            // Create temporary ban entry
            long durationMinutes = configManager.getFakeBanDurationMinutes();
            long durationMs = TimeUnit.MINUTES.toMillis(durationMinutes);
            
            FakeBanEntry fakeBanEntry = new FakeBanEntry(targetPlayer, reason, durationMs);
            
            // If the player is online, capture UUID and IP
            server.getPlayer(targetPlayer).ifPresent(player -> {
                fakeBanEntry.setUuid(player.getUniqueId().toString());
                fakeBanEntry.setIp(player.getRemoteAddress().getAddress().getHostAddress());
            });

            // Save to configuration
            configManager.addFakeBan(fakeBanEntry);
            
            // Add to active list
            activeFakeBans.put(targetPlayer, fakeBanEntry);

            // Kick online player
            kickPlayer(targetPlayer, reason);

            logger.info("Successfully temporarily banned player: " + targetPlayer + ", duration: " + durationMinutes + " minutes");
            return "Successfully temporarily banned player: " + targetPlayer + ", duration: " + durationMinutes + " minutes";
            
        } catch (Exception e) {
            logger.error("Failed to execute temporary ban: " + targetPlayer, e);
            return "Failed to execute temporary ban, check logs";
        }
    }

    /**
     * Remove temporary ban
     */
    public String unFakeBan(String targetPlayer) {
        // Input validation
        if (targetPlayer == null || targetPlayer.trim().isEmpty()) {
            return "Player name cannot be empty";
        }
        if (targetPlayer.length() > 16 || !targetPlayer.matches("^[a-zA-Z0-9_]{1,16}$")) {
            return "Invalid player name format";
        }

        FakeBanEntry fakeBan = findActiveFakeBan(targetPlayer);
        if (fakeBan == null) {
            return "This player does not have an active temporary ban record!";
        }

        // Set to inactive state
        configManager.setFakeBanState(targetPlayer, false);
        activeFakeBans.remove(targetPlayer);

        logger.info("Successfully removed temporary ban: " + targetPlayer);
        return "Successfully removed temporary ban: " + targetPlayer;
    }

    /**
     * Check whether a player is temporarily banned
     */
    public boolean isFakeBanned(String uuid, String ip, String username) {
        return activeFakeBans.values().stream()
                .filter(entry -> !entry.isExpired())
                .anyMatch(entry -> {
                    // Prefer checking player name first
                    if (entry.getName().equalsIgnoreCase(username)) {
                        return true;
                    }
                    // Check UUID and IP (if not empty)
                    return (!entry.getUuid().isEmpty() && entry.getUuid().equals(uuid)) ||
                           (!entry.getIp().isEmpty() && entry.getIp().equals(ip));
                });
    }

    /**
     * Get temporary ban information
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
     * Find an active temporary ban record
     */
    private FakeBanEntry findActiveFakeBan(String targetPlayer) {
        return activeFakeBans.values().stream()
                .filter(entry -> entry.getName().equalsIgnoreCase(targetPlayer))
                .filter(entry -> !entry.isExpired())
                .findFirst()
                .orElse(null);
    }

    /**
     * Kick a player
     */
    private void kickPlayer(String targetPlayer, String reason) {
        server.getPlayer(targetPlayer).ifPresent(player -> {
            Component kickMessage = Component.text(reason);
            player.disconnect(kickMessage);
            logger.info("Kicked player: " + targetPlayer + ", reason: " + reason);
        });
    }

    /**
     * Load active temporary ban records
     */
    private void loadActiveFakeBans() {
        activeFakeBans.clear();
        Map<String, FakeBanEntry> fakeBans = configManager.getFakeBans();
        
        for (FakeBanEntry entry : fakeBans.values()) {
            if (entry.getState() && !entry.isExpired()) {
                activeFakeBans.put(entry.getName(), entry);
            }
        }
        
        logger.info("Loaded " + activeFakeBans.size() + " active temporary ban records");
    }

    /**
     * Start cleanup task
     */
    private void startCleanupTask() {
        // Clean up expired temporary bans every minute
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // Clean up expired pending operations
                pendingFakeBans.entrySet().removeIf(entry -> entry.getValue().isExpired());
                
                // Clean up expired temporary bans
                configManager.cleanupExpiredFakeBans();
                loadActiveFakeBans();
                
            } catch (Exception e) {
                logger.error("Error occurred while cleaning up expired temporary bans", e);
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * Shutdown the manager
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
     * Get a list of all temporarily banned player names
     */
    public List<String> getFakeBannedPlayers() {
        return activeFakeBans.values().stream()
                .filter(entry -> !entry.isExpired())
                .map(FakeBanEntry::getName)
                .collect(Collectors.toList());
    }
}
