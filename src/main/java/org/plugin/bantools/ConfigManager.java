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
            System.err.println("配置文件解析失败，尝试修复...");
            e.printStackTrace();
            // 如果配置文件损坏，备份并重新创建
            backupAndRecreateConfig();
        }
    }

    private void createDefaultConfig() {
        configFile.getParentFile().mkdirs();
        String defaultConfig = "defaults {\n" +
                "  ban_reason = \"违反服务器规则\"\n" +
                "  kick_reason = \"管理员强制踢出\"\n" +
                "  fakeban_reason = \"暂时踢出，请稍后重试\"\n" +
                "}\n" +
                "\n" +
                "fakeban {\n" +
                "  duration_minutes = 30\n" +
                "  confirmation_message = \"此操作将会暂时踢出玩家直到三十分钟后才可以重新加入，建议检查挂机玩家周遭情况，确认执行请再次输入指令\"\n" +
                "  confirmation_timeout_minutes = 3\n" +
                "}\n" +
                "\n" +
                "whitelist {\n" +
                "  enabled = true\n" +
                "  players = [\"Admin\", \"Owner\"]\n" +
                "  protection_message = \"该玩家受到白名单保护，无法执行此操作！\"\n" +
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
            // 备份损坏的配置文件
            File backupFile = new File(configFile.getParent(), "config.conf.backup." + System.currentTimeMillis());
            if (configFile.exists()) {
                java.nio.file.Files.copy(configFile.toPath(), backupFile.toPath());
                System.out.println("已备份损坏的配置文件到: " + backupFile.getName());
            }

            // 重新创建默认配置
            createDefaultConfig();
            config = ConfigFactory.parseFile(configFile);
            loadBans();

        } catch (Exception e) {
            System.err.println("修复配置文件失败: " + e.getMessage());
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
        loadBans(); // 重新加载封禁数据到内存
    }

    public void setBanState(String target, boolean state) {
        Config updatedConfig = config.withValue("bans." + target + ".state",
                ConfigValueFactory.fromAnyRef(state));
        saveConfig(updatedConfig);
        loadBans(); // 重新加载封禁数据到内存
    }

    public void updateBanEntry(BanEntry entry) {
        Config updatedConfig = config.withValue("bans." + entry.getName(),
                ConfigValueFactory.fromMap(entryToMap(entry)));
        saveConfig(updatedConfig);
        loadBans(); // 重新加载封禁数据到内存
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
            // 使用格式化的渲染选项来保持嵌套结构
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
        // 清空现有封禁列表
        bans.clear();

        // 确保 "bans" 字段存在且为对象
        if (!config.hasPath("bans")) {
            return; // 如果没有封禁数据，则直接返回
        }

        try {
            // 检查是否是扁平化的配置（损坏的格式）
            if (detectFlattenedConfig()) {
                System.out.println("检测到扁平化的配置文件，尝试修复...");
                fixFlattenedConfig();
                return;
            }

            ConfigObject bansObject = config.getObject("bans");
            if (bansObject.isEmpty()) {
                return; // 空的封禁列表
            }

            for (Map.Entry<String, ConfigValue> entry : bansObject.entrySet()) {
                String playerName = entry.getKey();
                ConfigValue value = entry.getValue();

                // 检查 ConfigValue 是否为 ConfigObject
                if (!(value instanceof ConfigObject)) {
                    System.err.println("Invalid data type for player '" + playerName + "'. Expected ConfigObject, got " + value.getClass().getSimpleName() + ". Skipping...");
                    continue;
                }

                try {
                    ConfigObject playerObject = (ConfigObject) value;

                    // 创建 BanEntry 并填充数据
                    BanEntry banEntry = new BanEntry();
                    banEntry.setName(playerName);

                    // 安全地获取各个字段
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

                    // 获取必需字段
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

                    // 处理可能为空的 end_time
                    ConfigValue endTimeValue = playerObject.get("end_time");
                    if (endTimeValue != null && endTimeValue.valueType() == ConfigValueType.NUMBER) {
                        banEntry.setEndTime(((Number) endTimeValue.unwrapped()).longValue());
                    } else {
                        banEntry.setEndTime(null); // 永久封禁
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
        // 检查是否存在类似 "player.field" 的键，这表明配置被扁平化了
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
            // 收集所有扁平化的数据
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

            // 重建配置
            Map<String, Object> newConfig = new HashMap<>();
            newConfig.put("defaults", Map.of(
                "ban_reason", "违反服务器规则",
                "kick_reason", "管理员强制踢出"
            ));
            newConfig.put("bans", playerData);

            // 保存修复后的配置
            Config fixedConfig = ConfigFactory.parseMap(newConfig);
            saveConfig(fixedConfig);

            // 重新加载
            config = fixedConfig;
            loadBans();

            System.out.println("配置文件修复完成，重新加载了 " + playerData.size() + " 个玩家的封禁记录");

        } catch (Exception e) {
            System.err.println("修复扁平化配置失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 加载临时封禁数据
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

                    // 处理可能为空的 UUID 和 IP
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

                    // 只加载有效且未过期的临时封禁
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
     * 添加临时封禁记录
     */
    public void addFakeBan(FakeBanEntry entry) {
        Config updatedConfig = config.withValue("fakebans." + entry.getName(),
                ConfigValueFactory.fromMap(fakeBanEntryToMap(entry)));
        saveConfig(updatedConfig);
        loadFakeBans();
    }

    /**
     * 设置临时封禁状态
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
     * 将FakeBanEntry转换为Map
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
     * 清理过期的临时封禁记录
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