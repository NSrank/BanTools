# BanTools - Velocity 封禁管理插件

![Velocity](https://img.shields.io/badge/Velocity-3.x-blue) ![Java](https://img.shields.io/badge/Java-11-green) ![License](https://img.shields.io/badge/License-GPLv3-green.svg)

**BanTools** 是一个专为 Minecraft Velocity 服务端设计的高级封禁管理插件。它支持通过 UUID、IP 地址或用户名封禁玩家，并提供动态配置重载和实时踢出在线玩家的功能。

> **注意**：本插件由 AI 开发，旨在帮助服务器管理员更高效地管理玩家封禁行为。

---

## 功能特性

- **多维度封禁**：
    - 支持按 UUID、IP 地址或用户名封禁玩家。
    - 封禁权重优先级：UUID > IP > 用户名。
- **动态封禁**：
    - 使用 `/ban uuid <uuid>`、`/ban ip <ip>` 和 `/ban name <name>` 命令动态添加封禁数据。
    - 封禁数据会实时写入配置文件（`config.conf`）。
- **在线玩家处理**：
    - 如果被封禁的玩家当前在线，则立即踢出。
- **配置重载**：
    - 使用 `/bantools reload` 命令动态重载配置文件，无需重启服务器。
- **易用性**：
    - 配置文件采用 HOCON 格式，易于维护和扩展。

---

## 安装步骤

### 1. 下载插件
从 [GitHub](https://github.com/NSrank/BanTools) 或其他分发渠道下载最新版本的 `BanTools.jar`。

### 2. 安装插件
将下载的 `BanTools.jar` 文件放入 Velocity 服务端的 `plugins/` 目录中。

### 3. 启动服务器
启动 Velocity 服务端，插件会自动生成默认配置文件 `plugins/BanTools/config.conf`。

---

## 使用方法

### 命令列表

| 命令                     | 权限   | 描述                                                                 |
|--------------------------|--------|----------------------------------------------------------------------|
| `/bantools reload`       | 管理员 | 重新加载插件配置文件，无需重启服务器。                               |
| `/ban uuid <uuid>`       | 管理员 | 按 UUID 封禁玩家，并将其加入封禁列表。                              |
| `/ban ip <ip>`           | 管理员 | 按 IP 地址封禁玩家，并将其加入封禁列表。                            |
| `/ban name <name>`       | 管理员 | 按用户名封禁玩家，并将其加入封禁列表。                              |

### 示例
1. 封禁 UUID 为 `123e4567-e89b-12d3-a456-426614174000` 的玩家：`/ban uuid 123e4567-e89b-12d3-a456-426614174000`
2. 封禁 IP 地址为 `192.168.1.100` 的玩家：`/ban ip 192.168.1.100`
3. 封禁用户名为 `Bianpao_xiaohai` 的玩家：`/ban name Bianpao_xiaohai`

---

## 配置文件

插件的配置文件位于 `plugins/BanTools/config.conf`，格式如下：

```
banned {
 uuids = [
     # 示例 UUID
     # "123e4567-e89b-12d3-a456-426614174000"
 ]
 ips = [
     # 示例 IP 地址
     # "192.168.1.100"
 ]
 usernames = [
     # 示例用户名
     # "Bianpao_xiaohai"
 ]
}

你可以通过修改 `config.conf` 文件来添加或移除封禁数据。
```

---

### 技术支持与反馈
如果您在使用插件过程中遇到任何问题，或希望提出改进建议，请通过以下方式联系我：

- **GitHub Issues** : [提交问题](https://github.com/NSrank/BanTools/issues)

---

### 版权声明
- 开发声明 ：本插件由 AI 开发，旨在为 Minecraft Velocity 社区提供高效的封禁管理工具。
- 许可证 ：本插件遵循 GNU General Public License v3.0 许可证，您可以自由使用、修改和分发，但需遵守许可证条款。
- 免责条款 ：开发者不对因使用本插件而导致的任何问题负责。

---

### 特别感谢
感谢以下技术和工具对本插件的支持：

- [Velocity API](https://papermc.io/software/velocity)
- [Typesafe Config](https://github.com/lightbend/config?spm=a2ty_o01.29997173.0.0.7c5733f51H3mj8)
- [Adventure API](https://github.com/KyoriPowered/adventure?spm=a2ty_o01.29997173.0.0.7c5733f51H3mj8)

---

# BanTools - Velocity Ban Management Plugin

![Velocity](https://img.shields.io/badge/Velocity-3.x-blue) ![Java](https://img.shields.io/badge/Java-11-green) ![License](https://img.shields.io/badge/License-GPLv3-green.svg)

**BanTools** is an advanced ban management plugin designed for Minecraft Velocity servers. It supports banning players by UUID, IP address, or username, and provides dynamic configuration reloading and real-time kicking of online players.

> **Note**: This plugin is AI-developed to help server administrators manage player bans more efficiently.

---

## Features

- **Multi-Dimensional Bans**:
  - Supports banning players by UUID, IP address, or username.
  - Ban priority: UUID > IP > Username.
- **Dynamic Ban Management**:
  - Use `/ban uuid <uuid>`, `/ban ip <ip>`, and `/ban name <name>` to dynamically add bans.
  - Bans are instantly written to the configuration file (`config.conf`).
- **Online Player Handling**:
  - Banned players currently online are kicked immediately.
- **Configuration Reload**:
  - Use `/bantools reload` to reload the configuration file without restarting the server.
- **User-Friendly**:
  - Configuration files use HOCON format for easy maintenance and scalability.

---

## Installation

### 1. Download the Plugin
Download the latest version of `BanTools.jar` from [GitHub](https://github.com/NSrank/BanTools) or other distribution channels.

### 2. Install the Plugin
Place the downloaded `BanTools.jar` file into the `plugins/` directory of your Velocity server.

### 3. Start the Server
Start the Velocity server. The plugin will automatically generate a default configuration file at `plugins/BanTools/config.conf`.

---

## Usage

### Commands

| Command                   | Permission | Description                                                      |
|---------------------------|------------|------------------------------------------------------------------|
| `/bantools reload`        | Admin      | Reloads the plugin configuration without restarting the server. |
| `/ban uuid <uuid>`        | Admin      | Bans a player by UUID and adds it to the ban list.              |
| `/ban ip <ip>`            | Admin      | Bans a player by IP address and adds it to the ban list.        |
| `/ban name <name>`        | Admin      | Bans a player by username and adds it to the ban list.          |

### Examples
1. Ban a player with UUID `123e4567-e89b-12d3-a456-426614174000`:  
   `/ban uuid 123e4567-e89b-12d3-a456-426614174000`
2. Ban a player with IP address `192.168.1.100`:  
   `/ban ip 192.168.1.100`
3. Ban a player with username `Bianpao_xiaohai`:  
   `/ban name Bianpao_xiaohai`

---

## Configuration

The configuration file is located at `plugins/BanTools/config.conf` and follows this structure:

```
banned {
 uuids = [
  # Example UUID
  # "123e4567-e89b-12d3-a456-426614174000"
    ]
 ips = [
  # Example IP address
  # "192.168.1.100"
    ]
 usernames = [
  # Example username
  # "Bianpao_xiaohai"
    ]
}

Modify `config.conf` to add or remove bans.
```

---

### Support & Feedback
If you encounter issues or have suggestions, please contact us via:

- **GitHub Issues**: [Submit an Issue](https://github.com/NSrank/BanTools/issues)

---

### License & Disclaimer
- **Development Notice**: This plugin is AI-developed to provide efficient ban management tools for the Minecraft Velocity community.
- **License**: Distributed under the GNU General Public License v3.0. You may use, modify, and distribute it under the license terms.
- **Disclaimer**: The developer is not responsible for any issues arising from the use of this plugin.

---

### Acknowledgments
Special thanks to the following technologies and tools:

- [Velocity API](https://papermc.io/software/velocity)
- [Typesafe Config](https://github.com/lightbend/config)
- [Adventure API](https://github.com/KyoriPowered/adventure)