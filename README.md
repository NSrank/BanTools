# BanTools - Velocity 封禁管理插件

![Velocity](https://img.shields.io/badge/Velocity-3.x-blue) ![Java](https://img.shields.io/badge/Java-11-green) ![License](https://img.shields.io/badge/License-GPLv3-green.svg)

**BanTools** 是一个专为 Minecraft Velocity 服务端设计的高级封禁管理插件。它支持通过 UUID、IP 地址或用户名封禁玩家，并提供动态配置重载和实时踢出在线玩家的功能。

> **注意**：本插件由 AI 开发，旨在帮助服务器管理员更高效地管理玩家封禁行为。

---

## 功能特性

- **封禁功能**：
    - 支持按 UUID、IP 地址或玩家名封禁。
    - 默认封禁时间为永久（如果未指定时间）。
    - 支持指定封禁时长（如 `7d` 表示 7 天，`2024/1/10-2025/01/10` 表示自定义日期范围）。
    - 自动踢出被封禁的在线玩家。
- **解封功能**：
    - 支持通过 `/bantools unban` 命令解除指定玩家的封禁状态。
    - 解封后不会删除封禁记录，而是将封禁状态标记为无效。
- **踢出功能**：
    - 支持通过 `/bantools kick` 命令立即踢出指定玩家。
    - 可以指定踢出原因（默认使用配置文件中的默认踢出原因）。
- **自动解封机制**：
    - 如果指定了封禁时长，到达封禁结束时间后会自动解除封禁。
- **多条件匹配**：
    - 登录时会同时检查 UUID、IP 地址和玩家名是否匹配封禁记录。
    - 如果任意一项匹配，则视为被封禁。
- **配置文件支持**：
    - 所有封禁记录存储在 `config.conf` 文件中，支持手动编辑。
    - 配置文件中可以设置默认封禁原因和踢出原因。
- **动态配置重载**：
    - 支持通过 `/bantools reload` 命令动态重载配置文件，无需重启服务器。
- **实时同步**：
    - 所有封禁、解封和踢出操作会实时同步到所有下游服务器。

---

## 安装步骤

### 1. 下载插件
从 [GitHub](https://github.com/NSrank/BanTools) 或其他分发渠道下载最新版本的 `BanTools.jar`。

### 2. 安装插件
将下载的 `BanTools.jar` 文件放入 Velocity 服务端的 `plugins/` 目录中。

### 3. 启动服务器
启动 Velocity 服务端，插件会自动生成默认配置文件 `plugins/BanTools/config.conf`。

## 配置文件（`config.conf`）
```
defaults {
  ban_reason = "违反服务器规则"
  kick_reason = "管理员强制踢出"
}

bans {
  "Player1": {
    name: "Player1"
    uuid: "069a79f4-44e9-4726-a5be-fca90e38aaf5"
    ip: "192.168.1.100"
    reason: "作弊行为"
    start_time: 1698765432
    end_time: null  # 永久封禁
    state: true     # 封禁状态（true：生效，false：解除）
  }
}
```
- `defaults.ban_reason`：默认封禁原因。
- `defaults.kick_reason`：默认踢出原因。
- `bans`：存储所有封禁记录，每个条目包含以下字段：
  - `name`：玩家名。
  - `uuid`：玩家 UUID。
  - `ip`：玩家 IP 地址。
  - `reason`：封禁原因。
  - `start_time`：封禁开始时间（Unix 时间戳）。
  - `end_time`：封禁结束时间（Unix 时间戳），如果为 `null` 表示永久封禁。
  - `state`：封禁状态（`true` 表示生效，`false` 表示解除）。

---

## 使用方法

### 命令列表

| 命令                             | 权限节点                      | 描述            |
|--------------------------------|---------------------------|---------------|
| `/bantools reload`             | `bantools.command.reload` | 重新加载插件配置文件。   |
| `/bantools ban <type> <value>` | `bantools.command.ban`    | 封禁指定玩家。       |
| `/bantools unban <player>`     | `bantools.command.unban`  | 解除指定玩家的封禁状态。  |
| `/bantools kick <player>`      | `bantools.command.kick`   | 踢出指定玩家。       |

### 示例
1. 封禁用户名为 `Bianpao_xiaohai` 的玩家：`/bantools ban Bianpao_xiaohai`（默认封禁时长为永久，后可加封禁原因）
2. 解封用户名为 `Steve` 的玩家：`/bantools unban Steve`
3. 踢出用户名为 `Steve` 的玩家：`/bantools kick Steve`

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

- **Ban Functionality**:
    - Supports banning by UUID, IP address, or player name.
    - Default ban duration is permanent (if no duration is specified).
    - Supports specifying ban duration (e.g., `7d` for 7 days, `2024/1/10-2025/01/10` for a custom date range).
    - Automatically kicks banned online players.
- **Unban Functionality**:
    - Supports unbanning a player using the `/bantools unban` command.
    - Unbanning does not delete the ban record but marks the ban status as invalid.
- **Kick Functionality**:
    - Supports immediately kicking a player using the `/bantools kick` command.
    - A custom kick reason can be specified (default uses the configured reason in the config file).
- **Automatic Unban Mechanism**:
    - If a ban duration is specified, the ban will automatically expire when the time ends.
- **Multi-Condition Matching**:
    - On login, checks if UUID, IP address, or player name matches any ban records.
    - If any condition matches, the player is considered banned.
- **Configuration File Support**:
    - All ban records are stored in the `config.conf` file, which supports manual editing.
    - The configuration file allows setting default ban and kick reasons.
- **Dynamic Configuration Reload**:
    - Supports dynamically reloading the configuration file via the `/bantools reload` command without restarting the server.
- **Real-Time Synchronization**:
    - All ban, unban, and kick operations are synchronized in real-time across all downstream servers.

---

## Installation

### 1. Download the Plugin
Download the latest version of `BanTools.jar` from [GitHub](https://github.com/NSrank/BanTools) or other distribution channels.

### 2. Install the Plugin
Place the downloaded `BanTools.jar` file into the `plugins/` directory of your Velocity server.

### 3. Start the Server
Start the Velocity server. The plugin will automatically generate a default configuration file at `plugins/BanTools/config.conf`.

## Configuration（`config.conf`）
```
defaults {
  ban_reason = "违反服务器规则"
  kick_reason = "管理员强制踢出"
}

bans {
  "Player1": {
    name: "Player1"
    uuid: "069a79f4-44e9-4726-a5be-fca90e38aaf5"
    ip: "192.168.1.100"
    reason: "作弊行为"
    start_time: 1698765432
    end_time: null  # 永久封禁
    state: true     # 封禁状态（true：生效，false：解除）
  }
}
```
- `defaults.ban_reason`: Default ban reason.
- `defaults.kick_reason`: Default kick reason.
- `bans`: Stores all ban records, each entry contains the following fields:
  - `name`: Player name.
  - `uuid`: Player UUID.
  - `ip`: Player IP address.
  - `reason`: Ban reason.
  - `start_time`: Ban start time (Unix timestamp).
  - `end_time`: Ban end time (Unix timestamp), set to `null` for permanent bans.
  - `state`: Ban status (true for active, false for unban).

---

## Usage

### Commands

| Command                             | Permission Node               | Description                          |
|-------------------------------------|-------------------------------|--------------------------------------|
| `/bantools reload`                  | `bantools.command.reload`     | Reloads the plugin configuration file. |
| `/bantools ban <type> <value>`      | `bantools.command.ban`        | Bans the specified player.           |
| `/bantools unban <player>`          | `bantools.command.unban`      | Unbans the specified player.         |
| `/bantools kick <player>`           | `bantools.command.kick`       | Kicks the specified player.          |

### Examples
1. Ban a player named `Bianpao_xiaohai`: `/bantools ban Bianpao_xiaohai` (default ban duration is permanent; a reason can be added afterward).
2. Unban a player named `Steve`: `/bantools unban Steve`.
3. Kick a player named `Steve`: `/bantools kick Steve`.

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