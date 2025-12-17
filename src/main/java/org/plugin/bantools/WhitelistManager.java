package org.plugin.bantools;

import org.slf4j.Logger;

import java.util.*;

/**
 * Whitelist manager
 * Manages the protected players list to prevent administrators from being maliciously banned
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
        this.protectionMessage = "This player is protected by the whitelist and cannot be modified!";

        loadWhitelist();
    }

    /**
     * Load whitelist configuration (from main configuration file)
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

            logger.info("Whitelist configuration loaded, status: " + (enabled ? "enabled" : "disabled") +
                       ", protected players: " + whitelist.size());

        } catch (Exception e) {
            logger.error("Failed to load whitelist configuration", e);
            // Use default configuration
            enabled = true;
            protectionMessage = "This player is protected by the whitelist and cannot be modified!";
            whitelist.clear();
            whitelist.addAll(Arrays.asList("Admin", "Owner"));
        }
    }



    /**
     * Check whether a player is on the whitelist
     */
    public boolean isWhitelisted(String playerName) {
        if (!enabled || playerName == null) {
            return false;
        }
        return whitelist.contains(playerName);
    }

    /**
     * Check whether an operation can be performed on a player
     * @param playerName player name
     * @return null if operation is allowed, otherwise the protection message
     */
    public String checkProtection(String playerName) {
        if (isWhitelisted(playerName)) {
            return protectionMessage;
        }
        return null;
    }

    /**
     * Add a player to the whitelist
     * Note: this only updates the in-memory whitelist; edit the configuration file to persist
     */
    public boolean addToWhitelist(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return false;
        }

        boolean added = whitelist.add(playerName.trim());
        if (added) {
            logger.info("Player " + playerName + " added to in-memory whitelist (edit configuration file to persist)");
        }
        return added;
    }

    /**
     * Remove a player from the whitelist
     * Note: this only updates the in-memory whitelist; edit the configuration file to persist
     */
    public boolean removeFromWhitelist(String playerName) {
        boolean removed = whitelist.remove(playerName);
        if (removed) {
            logger.info("Player " + playerName + " removed from in-memory whitelist (edit configuration file to persist)");
        }
        return removed;
    }

    // Getters
    public boolean isEnabled() { return enabled; }
    public Set<String> getWhitelist() { return new HashSet<>(whitelist); }
    public String getProtectionMessage() { return protectionMessage; }
}
