# BanTools - Velocity 封禁管理插件

![Velocity](https://img.shields.io/badge/Velocity-3.x-blue) ![Java](https://img.shields.io/badge/Java-17-green) ![License](https://img.shields.io/badge/License-GPLv3-green.svg)

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
- **重复封禁检查**：
    - 自动检查玩家是否已被封禁，防止重复封禁操作。
    - 显示现有封禁的详细信息（理由和时长）。
- **重复解封检查**：
    - 自动检查玩家是否已被解封或未被封禁，防止重复解封操作。
    - 提供清晰的状态提示信息。
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
  "OnlinePlayer": {
    name: "OnlinePlayer"
    uuid: "069a79f4-44e9-4726-a5be-fca90e38aaf5"
    ip: "192.168.1.100"
    reason: "作弊行为"
    start_time: 1698765432
    end_time: null  # 永久封禁
    state: true     # 封禁状态（true：生效，false：解除）
  }
  "OfflinePlayer": {
    name: "OfflinePlayer"
    uuid: null      # 离线封禁，登录时自动更新
    ip: null        # 离线封禁，登录时自动更新
    reason: "违反服务器规则"
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

## 🔧 版本更新日志

### v1.3.2 (最新版本)
**重要改进：**
- ✅ **解封命令重构**：将独立的 `/unban` 命令整合到 `/bantools unban` 或 `/bt unban` 中，避免与其他插件冲突
- ✅ **修复数据同步问题**：封禁和解封操作后自动刷新内存数据，无需重启服务器
- ✅ **防重复封禁功能**：自动检查现有封禁记录，防止重复封禁操作
- ✅ **重复解封检查**：自动检查玩家解封状态，防止重复解封操作
- ✅ **统一命令体系**：所有命令现在都使用统一的 `/bantools` 或 `/bt` 前缀
- ✅ **智能状态检测**：区分"已解封"、"未封禁"和"无记录"三种状态

**新增功能：**
- 🆕 **重复封禁检查**：封禁前自动检查玩家是否已被封禁
- 🆕 **详细封禁信息提示**：显示现有封禁的理由和时长
- 🆕 **实时数据同步**：所有封禁操作立即生效，无需重启
- 🆕 **解封状态验证**：解封前检查玩家当前封禁状态
- 🆕 **详细状态提示**：提供清晰的解封结果反馈
- 🆕 **权限分离优化**：unban操作使用独立的权限节点

**用户体验改进：**
- 命令冲突风险降低：避免与其他插件的 `/unban` 命令冲突
- 操作反馈更清晰：明确区分不同的解封失败原因
- 命令体系更统一：所有功能都在一个命令下管理

**技术改进：**
- 优化了内存数据同步机制

### v1.3.1
**重要修复：**
- ✅ **修复配置文件扁平化问题**：解决了离线玩家封禁后重启服务器出现的配置加载错误
- ✅ **智能配置修复**：自动检测并修复损坏的配置文件格式
- ✅ **改进错误处理**：更好的配置文件解析和错误恢复机制
- ✅ **安全备份机制**：损坏的配置文件会自动备份，避免数据丢失

**技术改进：**
- 实现了扁平化配置检测算法
- 添加了自动配置重建功能
- 改进了配置文件保存格式
- 增强了离线玩家处理逻辑
- 优化了内存数据同步机制

### v1.3.0
- 修复了权限检查漏洞
- 改进了离线玩家封禁处理
- 添加了输入验证和安全检查
- 更新了README文档

---

## 使用方法

### 命令列表

| 命令                                    | 别名  | 权限节点                      | 描述            |
|---------------------------------------|-----|---------------------------|---------------|
| `/bantools reload`                    | `/bt reload` | `bantools.command.reload` | 重新加载插件配置文件。   |
| `/bantools ban <玩家> [原因] [时长]`      | `/bt ban <玩家> [原因] [时长]` | `bantools.command.ban`    | 封禁指定玩家。       |
| `/bantools unban <玩家>`              | `/bt unban <玩家>` | `bantools.command.unban`  | 解除指定玩家的封禁状态。  |
| `/bantools kick <玩家> [原因]`          | `/bt kick <玩家> [原因]` | `bantools.command.kick`   | 踢出指定玩家。       |

### 示例
1. 封禁用户名为 `Bianpao_xiaohai` 的玩家：`/bantools ban Bianpao_xiaohai` 或 `/bt ban Bianpao_xiaohai`
2. 封禁玩家并指定原因：`/bt ban Steve 恶意破坏`
3. 封禁玩家并指定时长：`/bt ban Steve 作弊行为 7d`（7天后自动解封）
4. 尝试重复封禁已封禁的玩家：`/bt ban Steve 再次作弊`
   - 系统提示：`该玩家已被封禁！理由：作弊行为，时长：至 2024/01/17`
5. 解封用户名为 `Steve` 的玩家：`/bt unban Steve`
6. 尝试重复解封已解封的玩家：`/bt unban Steve`
   - 系统提示：`该玩家未被封禁或已被解封！`
7. 踢出用户名为 `Steve` 的玩家：`/bt kick Steve 违反规则`

---

## ⚠️ 安全注意事项

### 安全建议
- 谨慎分配 `bantools.command.kick` 和 `bantools.command.ban` 权限
- 定期检查配置文件中的封禁记录是否正确加载
- 建议结合其他安全插件使用，如 IP 白名单、反作弊插件等
- 在重要服务器上使用前请先在测试环境验证功能

---

## 🛠️ 故障排除

### 常见问题

**Q: 重启服务器后出现 "Invalid data type for player 'xxx.state'" 错误**
A: 这是配置文件扁平化问题，v1.3.1已自动修复。插件会显示"检测到扁平化的配置文件，尝试修复..."并自动重建配置。

**Q: 封禁的离线玩家无法正确加载**
A: 确保使用v1.3.1或更高版本，该版本已修复离线玩家处理逻辑。

**Q: 配置文件损坏怎么办**
A: 插件会自动备份损坏的配置文件（文件名包含时间戳），然后重新创建默认配置。

**Q: 权限设置问题**
A: 确保正确分配权限：
- `bantools.command.ban` - 封禁权限
- `bantools.command.kick` - 踢出权限
- `bantools.command.unban` - 解封权限
- `bantools.command.reload` - 重载权限

**Q: 解封命令不工作或与其他插件冲突**
A: v1.3.2已将解封命令整合到 `/bt unban` 中，不再使用独立的 `/unban` 命令，避免了插件冲突。

**Q: 提示"该玩家未被封禁或已被解封"**
A: 这表示玩家当前没有有效的封禁记录，可能已经被解封或从未被封禁。

### 配置文件格式

正确的配置文件格式应该是：
```hocon
defaults {
  ban_reason = "违反服务器规则"
  kick_reason = "管理员强制踢出"
}

bans {
  "PlayerName": {
    name: "PlayerName"
    uuid: "player-uuid-here"  # 在线封禁时自动填充
    ip: "player-ip-here"      # 在线封禁时自动填充
    reason: "封禁原因"
    start_time: 1698765432
    end_time: null            # null表示永久封禁
    state: true               # true表示生效
  }
  "OfflinePlayer": {
    name: "OfflinePlayer"
    uuid: null                # 离线封禁，登录时自动更新
    ip: null                  # 离线封禁，登录时自动更新
    reason: "离线封禁"
    start_time: 1698765432
    end_time: null
    state: true
  }
}
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

![Velocity](https://img.shields.io/badge/Velocity-3.x-blue) ![Java](https://img.shields.io/badge/Java-17-green) ![License](https://img.shields.io/badge/License-GPLv3-green.svg)

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
- **Duplicate Ban Prevention**:
    - Automatically checks if a player is already banned to prevent duplicate ban operations.
    - Displays detailed information about existing bans (reason and duration).
- **Duplicate Unban Prevention**:
    - Automatically checks if a player is already unbanned or not banned to prevent duplicate unban operations.
    - Provides clear status notification messages.
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
  "OnlinePlayer": {
    name: "OnlinePlayer"
    uuid: "069a79f4-44e9-4726-a5be-fca90e38aaf5"
    ip: "192.168.1.100"
    reason: "Cheating"
    start_time: 1698765432
    end_time: null  # Permanent ban
    state: true     # Ban status (true: active, false: unbanned)
  }
  "OfflinePlayer": {
    name: "OfflinePlayer"
    uuid: null      # Offline ban, auto-updated on login
    ip: null        # Offline ban, auto-updated on login
    reason: "Rule violation"
    start_time: 1698765432
    end_time: null  # Permanent ban
    state: true     # Ban status (true: active, false: unbanned)
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

## 🔧 Version Changelog

### v1.3.2 (Latest)
**Major Improvements:**
- ✅ **Unban Command Refactoring**: Integrated standalone `/unban` command into `/bantools unban` or `/bt unban` to avoid conflicts with other plugins
- ✅ **Fixed data synchronization**: Ban and unban operations now automatically refresh memory data without server restart
- ✅ **Duplicate ban prevention**: Automatically checks existing ban records to prevent duplicate ban operations
- ✅ **Duplicate Unban Prevention**: Automatically checks player unban status to prevent duplicate unban operations
- ✅ **Unified Command System**: All commands now use unified `/bantools` or `/bt` prefix
- ✅ **Smart Status Detection**: Distinguishes between "already unbanned", "not banned", and "no record" states

**New Features:**
- 🆕 **Duplicate ban checking**: Automatically checks if player is already banned before banning
- 🆕 **Detailed ban info display**: Shows existing ban reason and duration
- 🆕 **Real-time data sync**: All ban operations take effect immediately without restart
- 🆕 **Unban Status Validation**: Checks player's current ban status before unbanning
- 🆕 **Detailed Status Feedback**: Provides clear unban result notifications
- 🆕 **Optimized Permission Separation**: Unban operations use independent permission nodes

**User Experience Improvements:**
- Reduced command conflict risk: Avoids conflicts with other plugins' `/unban` commands
- Clearer operation feedback: Clearly distinguishes different unban failure reasons
- More unified command system: All features managed under one command

**Technical Improvements:**
- Optimized memory data synchronization mechanism

### v1.3.1
**Critical Fixes:**
- ✅ **Fixed config file flattening issue**: Resolved configuration loading errors after restarting server with offline player bans
- ✅ **Smart config repair**: Automatically detects and repairs corrupted configuration file formats
- ✅ **Improved error handling**: Better configuration file parsing and error recovery mechanisms
- ✅ **Safe backup mechanism**: Corrupted config files are automatically backed up to prevent data loss

**Technical Improvements:**
- Implemented flattened configuration detection algorithm
- Added automatic configuration rebuilding functionality
- Improved configuration file save format
- Enhanced offline player handling logic

### v1.3.0
- Fixed permission check vulnerabilities
- Improved offline player ban handling
- Added input validation and security checks
- Updated README documentation

---

## Usage

### Commands

| Command                                    | Alias  | Permission Node               | Description                          |
|--------------------------------------------|--------|-------------------------------|--------------------------------------|
| `/bantools reload`                         | `/bt reload` | `bantools.command.reload`     | Reloads the plugin configuration file. |
| `/bantools ban <player> [reason] [duration]` | `/bt ban <player> [reason] [duration]` | `bantools.command.ban`        | Bans the specified player.           |
| `/bantools unban <player>`                | `/bt unban <player>` | `bantools.command.unban`      | Unbans the specified player.         |
| `/bantools kick <player> [reason]`        | `/bt kick <player> [reason]` | `bantools.command.kick`       | Kicks the specified player.          |

### Examples
1. Ban a player named `Bianpao_xiaohai`: `/bantools ban Bianpao_xiaohai` or `/bt ban Bianpao_xiaohai`
2. Ban a player with reason: `/bt ban Steve Malicious behavior`
3. Ban a player with duration: `/bt ban Steve Cheating 7d` (auto-unban after 7 days)
4. Try to ban an already banned player: `/bt ban Steve Cheating again`
   - System response: `该玩家已被封禁！理由：Cheating，时长：至 2024/01/17`
5. Unban a player named `Steve`: `/bt unban Steve`
6. Try to unban an already unbanned player: `/bt unban Steve`
   - System response: `该玩家未被封禁或已被解封！`
7. Kick a player named `Steve`: `/bt kick Steve Rule violation`

---

## Security Considerations

### Security Recommendations

- Exercise caution when granting `bantools.command.kick` and `bantools.command.ban` permissions
- Regularly check if the ban records in the configuration file are correctly loaded
- Consider using additional security plugins such as IP whitelists and anti-cheat plugins
- Test the plugin thoroughly in a non-production environment before deployment

---

## 🛠️ Troubleshooting

### Common Issues

**Q: Getting "Invalid data type for player 'xxx.state'" errors after server restart**
A: This is a config file flattening issue, automatically fixed in v1.3.1. The plugin will show "检测到扁平化的配置文件，尝试修复..." and automatically rebuild the configuration.

**Q: Offline banned players not loading correctly**
A: Ensure you're using v1.3.1 or higher, which has fixed offline player handling logic.

**Q: What if my config file gets corrupted**
A: The plugin automatically backs up corrupted config files (filename includes timestamp) and recreates default configuration.

**Q: Permission setup issues**
A: Ensure correct permission assignment:
- `bantools.command.ban` - Ban permission
- `bantools.command.kick` - Kick permission
- `bantools.command.unban` - Unban permission
- `bantools.command.reload` - Reload permission

**Q: Unban command not working or conflicts with other plugins**
A: v1.3.2 has integrated the unban command into `/bt unban`, no longer using the standalone `/unban` command, avoiding plugin conflicts.

**Q: Getting "该玩家未被封禁或已被解封" message**
A: This indicates the player currently has no active ban record, possibly already unbanned or never banned.

### Configuration File Format

The correct configuration file format should be:
```hocon
defaults {
  ban_reason = "Rule violation"
  kick_reason = "Kicked by admin"
}

bans {
  "PlayerName": {
    name: "PlayerName"
    uuid: "player-uuid-here"  # Auto-filled when banning online players
    ip: "player-ip-here"      # Auto-filled when banning online players
    reason: "Ban reason"
    start_time: 1698765432
    end_time: null            # null means permanent ban
    state: true               # true means active
  }
  "OfflinePlayer": {
    name: "OfflinePlayer"
    uuid: null                # Offline ban, auto-updated on login
    ip: null                  # Offline ban, auto-updated on login
    reason: "Offline ban"
    start_time: 1698765432
    end_time: null
    state: true
  }
}
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