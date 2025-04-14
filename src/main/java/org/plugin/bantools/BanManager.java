package org.plugin.bantools;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BanManager {

    private final ProxyServer server;
    private final Logger logger;

    private final Set<String> bannedUuids = new HashSet<>();
    private final Set<String> bannedIps = new HashSet<>();
    private final Set<String> bannedUsernames = new HashSet<>();

    private File configFile;

    public BanManager(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
        loadConfig();
    }

    public void loadConfig() {
        try {
            // 配置文件路径
            configFile = new File("plugins/BanTools/config.conf");

            // 如果配置文件不存在，则创建默认配置
            if (!configFile.exists()) {
                createDefaultConfig(configFile);
            }

            // 加载配置文件
            Config config = ConfigFactory.parseFile(configFile);

            // 清空旧的封禁列表
            bannedUuids.clear();
            bannedIps.clear();
            bannedUsernames.clear();

            // 读取新的封禁列表
            bannedUuids.addAll(config.getStringList("banned.uuids"));
            bannedIps.addAll(config.getStringList("banned.ips"));
            bannedUsernames.addAll(config.getStringList("banned.usernames"));

            logger.info("Loaded " + bannedUuids.size() + " UUIDs, " + bannedIps.size() + " IPs, and " +
                    bannedUsernames.size() + " usernames from config");
        } catch (Exception e) {
            logger.error("Failed to load configuration file", e);
        }
    }

    private void createDefaultConfig(File configFile) {
        try {
            // 创建默认配置文件目录
            configFile.getParentFile().mkdirs();

            // 写入默认配置内容
            String defaultConfig = "# 默认配置文件\n" +
                    "banned {\n" +
                    "    uuids = []\n" +
                    "    ips = []\n" +
                    "    usernames = []\n" +
                    "}\n";
            Files.write(configFile.toPath(), defaultConfig.getBytes());

            logger.info("Created default configuration file at " + configFile.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Failed to create default configuration file", e);
        }
    }

    public boolean isBanned(String uuid, String ipAddress, String username) {
        return bannedUuids.contains(uuid)
                || bannedIps.contains(ipAddress)
                || bannedUsernames.contains(username);
    }

    public String getBanMessage(String uuid, String ipAddress, String username) {
        if (bannedUuids.contains(uuid)) {
            return "You are banned by UUID.";
        } else if (bannedIps.contains(ipAddress)) {
            return "You are banned by IP.";
        } else {
            return "You are banned by username.";
        }
    }

    public void addBannedUuid(String uuid) {
        bannedUuids.add(uuid);
        updateConfig("banned.uuids", bannedUuids);
    }

    public void addBannedIp(String ip) {
        bannedIps.add(ip);
        updateConfig("banned.ips", bannedIps);
    }

    public void addBannedUsername(String username) {
        bannedUsernames.add(username);
        updateConfig("banned.usernames", bannedUsernames);
    }

    private void updateConfig(String key, Set<String> values) {
        try {
            // 读取现有配置
            Config config = ConfigFactory.parseFile(configFile);

            // 更新指定键的值
            Config updatedConfig = config.withValue(key, ConfigValueFactory.fromIterable(values));

            // 写回文件
            Files.write(configFile.toPath(), updatedConfig.root().render().getBytes());
        } catch (Exception e) {
            logger.error("Failed to update configuration file", e);
        }
    }
}