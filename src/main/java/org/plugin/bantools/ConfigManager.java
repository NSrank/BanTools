package org.plugin.bantools;

import com.typesafe.config.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private Config config;
    private final File configFile;
    private final Map<String, BanEntry> bans = new HashMap<>();

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
                "}\n" +
                "\n" +
                "bans = {}";
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

    public String getDefaultBanReason() {
        return config.getString("defaults.ban_reason");
    }

    public String getDefaultKickReason() {
        return config.getString("defaults.kick_reason");
    }

    public void addBan(BanEntry entry) {
        Config updatedConfig = config.withValue("bans." + entry.getName(),
                ConfigValueFactory.fromMap(entryToMap(entry)));
        saveConfig(updatedConfig);
    }

    public void setBanState(String target, boolean state) {
        Config updatedConfig = config.withValue("bans." + target + ".state",
                ConfigValueFactory.fromAnyRef(state));
        saveConfig(updatedConfig);
    }

    public void updateBanEntry(BanEntry entry) {
        Config updatedConfig = config.withValue("bans." + entry.getName(),
                ConfigValueFactory.fromMap(entryToMap(entry)));
        saveConfig(updatedConfig);
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
}