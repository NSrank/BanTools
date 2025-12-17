package org.plugin.bantools;

import com.typesafe.config.*;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {
    private Config config;
    private final File configFile;
    private final Map<String, BanEntry> bans = new HashMap<>();
    private final Map<String, FakeBanEntry> fakeBans = new HashMap<>();

    public ConfigManager() {
        configFile = new File("plugins/BanTools/config.conf");
        loadConfig();
    }

    public void loadConfig() {
        if (!configFile.exists()) {
            createDefaultConfig();
        }
        try {
            config = ConfigFactory.parseFile(configFile);
            loadBans();
            loadFakeBans();
        } catch (Exception e) {
            System.err.println("Configuration file parsing failed, attempting repair...");
            e.printStackTrace();
            // If the configuration file is corrupted, back up and recreate it
            backupAndRecreateConfig();
        }
    }

    private void createDefaultConfig() {
        configFile.getParentFile().mkdirs();
        String defaultConfig = "defaults {\n" +
                "  ban_reason = \"Violation of server rules\"\n" +
                "  kick_reason = \"Kicked by an administrator\"\n" +
                "  fakeban_reason = \"Temporarily kicked, please try again later\"\n" +
                "}\n" +
                "\n" +
                "fakeban {\n" +
                "  duration_minutes = 30\n" +
                "  confirmation_message = \"This action will temporarily kick the player; they cannot rejoin for thirty minutes. Please check the surrounding area of AFK players. To confirm, re-enter the command.\"\n" +
                "  confirmation_timeout_minutes = 3\n" +
                "}\n" +
                "\n" +
                "whitelist {\n" +
                "  enabled = true\n" +
                "  players = [\"Admin\", \"Owner\"]\n" +
                "  protection_message = \"This player is protected by the whitelist and cannot be modified!\"\n" +
                "}\n" +
                "\n" +
                "bans = {}\n" +
                "fakebans = {}";
        try {
            java.nio.file.Files.write(configFile.toPath(), defaultConfig.getBytes("UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void backupAndRecreateConfig() {
        try {
            // Backup corrupted configuration file
            File backupFile = new File(configFile.getParent(), "config.conf.backup." + System.currentTimeMillis());
            if (configFile.exists()) {
                java.nio.file.Files.copy(configFile.toPath(), backupFile.toPath());
                System.out.println("Backed up corrupted configuration file to: " + backupFile.getName());
            }

            // Recreate default configuration
            createDefaultConfig();
            config = ConfigFactory.parseFile(configFile);
            loadBans();

        } catch (Exception e) {
            System.err.println("Failed to repair configuration file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Map<String, BanEntry> getBans() {
        return new HashMap<>(bans);
    }

    public Map<String, FakeBanEntry> getFakeBans() {
        return new HashMap<>(fakeBans);
    }

    public String getDefaultBanReason() {
        return config.getString("defaults.ban_reason");
    }

    public String getDefaultKickReason() {
        return config.getString("defaults.kick_reason");
    }

    public String getDefaultFakeBanReason() {
        return config.getString("defaults.fakeban_reason");
    }

    public int getFakeBanDurationMinutes() {
        return config.getInt("fakeban.duration_minutes");
    }

    public String getFakeBanConfirmationMessage() {
        return config.getString("fakeban.confirmation_message");
    }

    public int getFakeBanConfirmationTimeoutMinutes() {
        return config.getInt("fakeban.confirmation_timeout_minutes");
    }

    public boolean isWhitelistEnabled() {
        return config.getBoolean("whitelist.enabled");
    }

    public List<String> getWhitelistPlayers() {
        return config.getStringList("whitelist.players");
    }

    public String getWhitelistProtectionMessage() {
        return config.getString("whitelist.protection_message");
    }

    public void addBan(BanEntry entry) {
        Config updatedConfig = config.withValue("bans." + entry.getName(),
                ConfigValueFactory.fromMap(entryToMap(entry)));
        saveConfig(updatedConfig);
        loadBans(); // Reload ban data into memory
    }

    public void setBanState(String target, boolean state) {
        Config updatedConfig = config.withValue("bans." + target + ".state",
                ConfigValueFactory.fromAnyRef(state));
        saveConfig(updatedConfig);
        loadBans(); // Reload ban data into memory
    }

    public void updateBanEntry(BanEntry entry) {
        Config updatedConfig = config.withValue("bans." + entry.getName(),
                ConfigValueFactory.fromMap(entryToMap(entry)));
        saveConfig(updatedConfig);
        loadBans(); // Reload ban data into memory
    }

    private Map<String, Object> entryToMap(BanEntry entry) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", entry.getName());
        map.put("uuid", entry.getUuid());
        map.put("ip", entry.getIp());
        map.put("reason", entry.getReason());
        map.put("start_time", entry.getStartTime());
        map.put("end_time", entry.getEndTime());
        map.put("state", entry.getState());
        return map;
    }

    private void saveConfig(Config updatedConfig) {
        try {
            // Use formatted render options to preserve nested structure
            ConfigRenderOptions options = ConfigRenderOptions.defaults()
                    .setOriginComments(false)
                    .setComments(false)
                    .setFormatted(true);
            String configContent = updatedConfig.root().render(options);
            java.nio.file.Files.write(configFile.toPath(), configContent.getBytes("UTF-8"));
            config = updatedConfig;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadBans() {
        // Clear existing ban list
        bans.clear();

        // Ensure the "bans" field exists and is an object
        if (!config.hasPath("bans")) {
            return; // If there are no ban records, return
        }

        try {
            // Check if the configuration is flattened (corrupted format)
            if (detectFlattenedConfig()) {
                System.out.println("Detected flattened configuration file, attempting to repair...");
                fixFlattenedConfig();
                return;
            }

            ConfigObject bansObject = config.getObject("bans");
            if (bansObject.isEmpty()) {
                return; // Empty ban list
            }

            for (Map.Entry<String, ConfigValue> entry : bansObject.entrySet()) {
                String playerName = entry.getKey();
                ConfigValue value = entry.getValue();

                // Check whether ConfigValue is a ConfigObject
                if (!(value instanceof ConfigObject)) {
                    System.err.println("Invalid data type for player '" + playerName + "'. Expected ConfigObject, got " + value.getClass().getSimpleName() + ". Skipping...");
                    continue;
                }

                try {
                    ConfigObject playerObject = (ConfigObject) value;

                    // Create BanEntry and populate data
                    BanEntry banEntry = new BanEntry();
                    banEntry.setName(playerName);

                    // Safely retrieve each field
                    ConfigValue uuidValue = playerObject.get("uuid");
                    if (uuidValue != null && uuidValue.valueType() == ConfigValueType.STRING) {
                        banEntry.setUuid((String) uuidValue.unwrapped());
                    } else {
                        banEntry.setUuid(null);
                    }

                    ConfigValue ipValue = playerObject.get("ip");
                    if (ipValue != null && ipValue.valueType() == ConfigValueType.STRING) {
                        banEntry.setIp((String) ipValue.unwrapped());
                    } else {
                        banEntry.setIp(null);
                    }

                    // Get required fields
                    ConfigValue reasonValue = playerObject.get("reason");
                    if (reasonValue != null && reasonValue.valueType() == ConfigValueType.STRING) {
                        banEntry.setReason((String) reasonValue.unwrapped());
                    } else {
                        System.err.println("Missing or invalid reason for player '" + playerName + "'. Skipping...");
                        continue;
                    }

                    ConfigValue startTimeValue = playerObject.get("start_time");
                    if (startTimeValue != null && startTimeValue.valueType() == ConfigValueType.NUMBER) {
                        banEntry.setStartTime(((Number) startTimeValue.unwrapped()).longValue());
                    } else {
                        System.err.println("Missing or invalid start_time for player '" + playerName + "'. Skipping...");
                        continue;
                    }

                    ConfigValue stateValue = playerObject.get("state");
                    if (stateValue != null && stateValue.valueType() == ConfigValueType.BOOLEAN) {
                        banEntry.setState((Boolean) stateValue.unwrapped());
                    } else {
                        System.err.println("Missing or invalid state for player '" + playerName + "'. Skipping...");
                        continue;
                    }

                    // Handle possibly null end_time
                    ConfigValue endTimeValue = playerObject.get("end_time");
                    if (endTimeValue != null && endTimeValue.valueType() == ConfigValueType.NUMBER) {
                        banEntry.setEndTime(((Number) endTimeValue.unwrapped()).longValue());
                    } else {
                        banEntry.setEndTime(null); // Permanent ban
                    }

                    bans.put(playerName, banEntry);

                } catch (Exception e) {
                    System.err.println("Error loading ban data for player '" + playerName + "': " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading bans configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean detectFlattenedConfig() {
        // Check for keys like "player.field" which indicate the config has been flattened
        for (String key : config.root().keySet()) {
            if (key.contains(".") && (key.endsWith(".name") || key.endsWith(".uuid") ||
                key.endsWith(".ip") || key.endsWith(".reason") ||
                key.endsWith(".start_time") || key.endsWith(".end_time") ||
                key.endsWith(".state"))) {
                return true;
            }
        }
        return false;
    }

    private void fixFlattenedConfig() {
        try {
            // Collect all flattened data
            Map<String, Map<String, Object>> playerData = new HashMap<>();

            for (Map.Entry<String, ConfigValue> entry : config.root().entrySet()) {
                String key = entry.getKey();
                if (key.contains(".")) {
                    String[] parts = key.split("\\.", 2);
                    if (parts.length == 2) {
                        String playerName = parts[0];
                        String fieldName = parts[1];

                        playerData.computeIfAbsent(playerName, k -> new HashMap<>())
                                  .put(fieldName, entry.getValue().unwrapped());
                    }
                }
            }

            // Rebuild configuration
            Map<String, Object> newConfig = new HashMap<>();
            newConfig.put("defaults", Map.of(
                "ban_reason", "Violation of server rules",
                "kick_reason", "Kicked by an administrator"
            ));
            newConfig.put("bans", playerData);

            // Save the repaired configuration
            Config fixedConfig = ConfigFactory.parseMap(newConfig);
            saveConfig(fixedConfig);

            // Reload
            config = fixedConfig;
            loadBans();

            System.out.println("Configuration repair completed, reloaded " + playerData.size() + " player ban records");

        } catch (Exception e) {
            System.err.println("Failed to repair flattened configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load temporary ban data
     */
    public void loadFakeBans() {
        fakeBans.clear();
        try {
            if (!config.hasPath("fakebans")) {
                return;
            }

            ConfigObject fakeBansObject = config.getObject("fakebans");
            for (Map.Entry<String, ConfigValue> entry : fakeBansObject.entrySet()) {
                String playerName = entry.getKey();
                try {
                    ConfigObject playerObject = (ConfigObject) entry.getValue();

                    FakeBanEntry fakeBanEntry = new FakeBanEntry();
                    fakeBanEntry.setName(playerName);

                    // Handle possibly null UUID and IP
                    ConfigValue uuidValue = playerObject.get("uuid");
                    if (uuidValue != null && uuidValue.valueType() != ConfigValueType.NULL) {
                        fakeBanEntry.setUuid((String) uuidValue.unwrapped());
                    }

                    ConfigValue ipValue = playerObject.get("ip");
                    if (ipValue != null && ipValue.valueType() != ConfigValueType.NULL) {
                        fakeBanEntry.setIp((String) ipValue.unwrapped());
                    }

                    ConfigValue reasonValue = playerObject.get("reason");
                    if (reasonValue != null && reasonValue.valueType() == ConfigValueType.STRING) {
                        fakeBanEntry.setReason((String) reasonValue.unwrapped());
                    }

                    ConfigValue startTimeValue = playerObject.get("start_time");
                    if (startTimeValue != null && startTimeValue.valueType() == ConfigValueType.NUMBER) {
                        fakeBanEntry.setStartTime(((Number) startTimeValue.unwrapped()).longValue());
                    }

                    ConfigValue endTimeValue = playerObject.get("end_time");
                    if (endTimeValue != null && endTimeValue.valueType() == ConfigValueType.NUMBER) {
                        fakeBanEntry.setEndTime(((Number) endTimeValue.unwrapped()).longValue());
                    }

                    ConfigValue stateValue = playerObject.get("state");
                    if (stateValue != null && stateValue.valueType() == ConfigValueType.BOOLEAN) {
                        fakeBanEntry.setState((Boolean) stateValue.unwrapped());
                    }

                    // Only load valid and non-expired temporary bans
                    if (fakeBanEntry.getState() && !fakeBanEntry.isExpired()) {
                        fakeBans.put(playerName, fakeBanEntry);
                    }

                } catch (Exception e) {
                    System.err.println("Error loading fakeban data for player '" + playerName + "': " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading fakebans configuration: " + e.getMessage());
        }
    }

    /**
     * Add temporary ban record
     */
    public void addFakeBan(FakeBanEntry entry) {
        Config updatedConfig = config.withValue("fakebans." + entry.getName(),
                ConfigValueFactory.fromMap(fakeBanEntryToMap(entry)));
        saveConfig(updatedConfig);
        loadFakeBans();
    }

    /**
     * Set temporary ban state
     */
    public void setFakeBanState(String playerName, boolean state) {
        if (config.hasPath("fakebans." + playerName)) {
            Config updatedConfig = config.withValue("fakebans." + playerName + ".state",
                    ConfigValueFactory.fromAnyRef(state));
            saveConfig(updatedConfig);
            loadFakeBans();
        }
    }

    /**
     * Convert FakeBanEntry to Map
     */
    private Map<String, Object> fakeBanEntryToMap(FakeBanEntry entry) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", entry.getName());
        map.put("uuid", entry.getUuid());
        map.put("ip", entry.getIp());
        map.put("reason", entry.getReason());
        map.put("start_time", entry.getStartTime());
        map.put("end_time", entry.getEndTime());
        map.put("state", entry.getState());
        return map;
    }

    /**
     * Clean up expired temporary ban records
     */
    public void cleanupExpiredFakeBans() {
        boolean hasChanges = false;
        Config updatedConfig = config;

        for (Map.Entry<String, FakeBanEntry> entry : new HashMap<>(fakeBans).entrySet()) {
            if (entry.getValue().isExpired()) {
                updatedConfig = updatedConfig.withValue("fakebans." + entry.getKey() + ".state",
                        ConfigValueFactory.fromAnyRef(false));
                hasChanges = true;
            }
        }

        if (hasChanges) {
            saveConfig(updatedConfig);
            loadFakeBans();
        }
    }
}