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
        config = ConfigFactory.parseFile(configFile);
        loadBans();
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
            java.nio.file.Files.write(configFile.toPath(), defaultConfig.getBytes());
        } catch (Exception e) {
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
            java.nio.file.Files.write(configFile.toPath(), updatedConfig.root().render().getBytes());
            config = updatedConfig;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadBans() {
        // 清空现有封禁列表
        bans.clear();

        // 确保 "bans" 字段存在且为对象
        if (!config.hasPath("bans") || config.getObject("bans").isEmpty()) {
            return; // 如果没有封禁数据，则直接返回
        }

        Config bansConfig = config.getConfig("bans");
        for (Map.Entry<String, ConfigValue> entry : bansConfig.entrySet()) {
            String playerName = entry.getKey();
            ConfigValue value = entry.getValue();

            // 检查 ConfigValue 是否为 ConfigObject
            if (!(value instanceof ConfigObject)) {
                System.err.println("Invalid data type for player '" + playerName + "'. Skipping...");
                continue;
            }

            // 安全地转换为 Config 对象
            Config playerConfig = ((ConfigObject) value).toConfig();

            // 创建 BanEntry 并填充数据
            BanEntry banEntry = new BanEntry();
            banEntry.setName(playerName);
            banEntry.setUuid(playerConfig.getString("uuid"));
            banEntry.setIp(playerConfig.getString("ip"));
            banEntry.setReason(playerConfig.getString("reason"));
            banEntry.setStartTime(playerConfig.getLong("start_time"));

            // 处理可能为空的 end_time
            if (playerConfig.hasPath("end_time") && !playerConfig.getIsNull("end_time")) {
                banEntry.setEndTime(playerConfig.getLong("end_time"));
            } else {
                banEntry.setEndTime(null); // 永久封禁
            }

            banEntry.setState(playerConfig.getBoolean("state"));
            bans.put(playerName, banEntry);
        }
    }
}