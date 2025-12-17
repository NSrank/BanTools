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
- **Temporary Ban System (FakeBan)**:
    - Supports temporary bans with automatic expiration.
    - Requires confirmation to prevent accidental actions.
    - Managed independently from normal bans.
- **Whitelist Protection System**:
    - Protects specified players from being banned, kicked, or fakebanned.
    - Configurable whitelist options to prevent malicious bans of administrators.
    - Supports enabling/disabling and custom protection messages.
- **Smart Tab Completion**:
    - Intelligent completion for all commands, filtered by permissions.
    - Player name autocompletion excludes whitelisted players.
    - Quick suggestions for common reasons and durations.
    - Completion of banned player lists to improve unban efficiency.
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

## 📝 Configuration

### Main configuration file (`config.conf`)
```hocon
defaults {
  ban_reason = "Violation of server rules"
  kick_reason = "Kicked by an administrator"
  fakeban_reason = "Temporarily kicked, please try again later"
}

fakeban {
  duration_minutes = 30
  confirmation_message = "This action will temporarily kick the player; they cannot rejoin for thirty minutes. Please check the surrounding area of AFK players. To confirm, re-enter the command."
  confirmation_timeout_minutes = 3
}

whitelist {
  enabled = true
  players = ["Admin", "Owner"]
  protection_message = "This player is protected by the whitelist and cannot be modified!"
}

bans {
  "OnlinePlayer": {
    name: "OnlinePlayer"
    uuid: "069a79f4-44e9-4726-a5be-fca90e38aaf5"
    ip: "192.168.1.100"
    reason: "Cheating"
    start_time: 1698765432
    end_time: null  # Permanent ban
    state: true     # Ban state (true: active, false: revoked)
  }
  "OfflinePlayer": {
    name: "OfflinePlayer"
    uuid: null      # Offline ban; will be updated on login
    ip: null        # Offline ban; will be updated on login
    reason: "Violation of server rules"
    start_time: 1698765432
    end_time: null  # Permanent ban
    state: true     # Ban state (true: active, false: revoked)
  }
}

fakebans {
  "TempBannedPlayer": {
    name: "TempBannedPlayer"
    uuid: "123e4567-e89b-12d3-a456-426614174000"
    ip: "192.168.1.200"
    reason: "AFK"
    start_time: 1698765432
    end_time: 1698767232   # Auto unban after 30 minutes
    state: true            # Temporary ban state
  }
}
```

### Configuration Details

**defaults section**:
- `ban_reason`: Default ban reason
- `kick_reason`: Default kick reason
- `fakeban_reason`: Default temporary ban reason

**fakeban section**:
- `duration_minutes`: Duration of temporary ban (minutes)
- `confirmation_message`: Confirmation message
- `confirmation_timeout_minutes`: Confirmation timeout (minutes)

**whitelist section**:
- `enabled`: Whitelist enabled flag
- `players`: List of protected players
- `protection_message`: Protection message
- `defaults.ban_reason`: Default ban reason.
- `defaults.kick_reason`: Default kick reason.
- `bans`: Stores all ban records; each entry contains these fields:
  - `name`: Player name.
  - `uuid`: Player UUID.
  - `ip`: Player IP address.
  - `reason`: Ban reason.
  - `start_time`: Ban start time (Unix timestamp).
  - `end_time`: Ban end time (Unix timestamp); `null` means permanent ban.
  - `state`: Ban state (`true` means active, `false` means revoked).

---

## 🔧 Changelog

### v1.4.0 (Latest)
**Major New Features:**
- 🆕 **Temporary Ban System (FakeBan)**: New temporary ban feature with automatic expiration and a confirmation mechanism  
- 🆕 **Whitelist Protection System**: Protects specified players from bans, kicks, and temporary bans to prevent malicious banning of administrators  
- 🆕 **Smart Tab Completion**: Full command completion support, improving operation efficiency and accuracy  
- 🆕 **Confirmation Mechanism**: fakeban requires re-entering the same command within the specified time window to confirm the action  
- 🆕 **Automatic Expiration Cleanup**: Temporary bans automatically expire without manual intervention  

**UX Improvements:**  
- 🆕 **Enhanced Tab Completion**: Comprehensive command completion for faster operations  
- 🆕 **Permission-Aware Completion**: Shows available commands based on user permissions  
- 🆕 **Smart Player Filtering**: Automatically excludes whitelisted players from suggestions  
- 🆕 **Quick Options**: Provides quick selection for common ban reasons and durations  
- 🆕 **State-Aware Completion**: `unban` shows banned players, `unfakeban` shows temporarily banned players  

**New Commands:**  
- `/bantools fakeban <player> [reason]` - Temporarily ban a player (requires confirmation)  
- `/bantools unfakeban <player>` - Remove a temporary ban  

**User Experience Improvements:**  
- Smart Tab completion: Shows available commands based on permissions and auto-completes player names and common parameters  
- Player filtering: Automatically excludes whitelisted players during completion  
- Common options: Quick selection for common ban reasons and durations  
- State awareness: `unban` and `unfakeban` only display players in the corresponding state    

**Configuration Enhancements:**  
- Unified configuration: All settings consolidated in the main configuration file, including whitelist settings  
- Added `fakeban` section: supports custom temporary ban duration and confirmation message  
- Supports custom default reason and confirmation timeout for temporary bans  

**Technical Improvements:**  
- Improved command handling architecture with support for dynamic completion  
- Improved player list fetching mechanism  
- Enhanced unified configuration management  

**Security Improvements:**  
- All operations (ban, kick, fakeban) support whitelist protection  
- Prevents malicious banning of administrators due to permission leaks  
- Temporary bans are independent from normal bans  

### v1.3.2
**Important Improvements:**
- ✅ **Unban Command Refactor**: Integrated the standalone `/unban` command into `/bantools unban` or `/bt unban` to avoid conflicts with other plugins
- ✅ **Fixed Data Sync Issues**: Automatically refreshes in-memory data after ban/unban operations without server restart
- ✅ **Duplicate Ban Prevention**: Checks existing ban records to prevent duplicate bans
- ✅ **Duplicate Unban Checks**: Verifies unban state to prevent duplicate unban operations
- ✅ **Unified Command System**: All commands now use a unified `/bantools` or `/bt` prefix
- ✅ **Smart State Detection**: Differentiates between 'unbanned', 'not banned', and 'no record' states

**New Features:**
- 🆕 **Duplicate Ban Check**: Automatically checks if a player is already banned before banning
- 🆕 **Detailed Ban Info**: Shows reason and duration of existing bans
- 🆕 **Real-Time Data Sync**: All ban operations take effect immediately without restarting
- 🆕 **Unban State Validation**: Checks a player's current ban state before unbanning
- 🆕 **Detailed Status Feedback**: Provides clear unban result messages
- 🆕 **Permission Separation**: The unban operation uses a separate permission node

**User Experience Improvements:**
- Reduced command conflict risk: avoids conflicts with other plugins' `/unban` commands
- Clearer operation feedback: distinguishes different unban failure reasons
- Unified command system: all features are managed under the same command

**Technical Improvements:**
- Optimized in-memory data synchronization mechanism

### v1.3.1

**Important Fixes:**
- ✅ **Fixed config flattening issue**: Solved config loading errors after banning offline players and restarting the server
- ✅ **Smart config repair**: Automatically detects and repairs corrupted config files
- ✅ **Improved error handling**: Better config parsing and error recovery
- ✅ **Safe backup mechanism**: Corrupted config files are automatically backed up to prevent data loss

**Technical Improvements:**
- Implemented config flattening detection algorithm
- Added automatic config rebuild feature
- Improved config file save format
- Enhanced offline player handling logic
- Optimized in-memory data synchronization

### v1.3.0
- Fixed permission check vulnerabilities
- Improved offline player ban handling
- Added input validation and security checks
- Updated README documentation

---

## Usage

### Commands List

| Command                                | Alias  | Permission Node                | Description                         |
|---------------------------------------|--------|-------------------------------|-------------------------------------|
| `/bantools reload`                    | `/bt reload` | `bantools.command.reload`      | Reload the plugin configuration file. |
| `/bantools ban <player> [reason] [duration]` | `/bt ban <player> [reason] [duration]` | `bantools.command.ban`        | Ban the specified player.           |
| `/bantools unban <player>`            | `/bt unban <player>` | `bantools.command.unban`      | Unban the specified player.         |
| `/bantools fakeban <player> [reason]` | `/bt fakeban <player> [reason]` | `bantools.command.fakeban` | Temporarily ban a player (requires confirmation). |
| `/bantools unfakeban <player>`        | `/bt unfakeban <player>` | `bantools.command.unfakeban`  | Remove a temporary ban from a player. |
| `/bantools kick <player> [reason]`    | `/bt kick <player> [reason]` | `bantools.command.kick`       | Kick the specified player.          |

### Examples
1. Ban the player with username `Bianpao_xiaohai`: `/bantools ban Bianpao_xiaohai` or `/bt ban Bianpao_xiaohai`
2. Ban a player with a specified reason: `/bt ban Steve Griefing`
3. Ban a player with a duration: `/bt ban Steve Cheating 7d` (auto unban after 7 days)
4. Attempt to ban an already banned player: `/bt ban Steve Repeat offense`
   - System message: `The player is already banned! Reason: Cheating, Duration: until 2024/01/17`
5. Unban the player named `Steve`: `/bt unban Steve`
6. Attempt to unban a player that is not banned: `/bt unban Steve`
   - System message: `The player is not banned or has already been unbanned!`
7. Temporary ban a player (first execution): `/bt fakeban Alice AFK`
   - System prompt: `This action will temporarily kick the player; they cannot rejoin for thirty minutes. Please check the surrounding area of AFK players. To confirm, re-enter the command.`
8. Confirm temporary ban (re-run the same command within 3 minutes): `/bt fakeban Alice AFK`
   - System message: `Successfully temporarily banned player: Alice, duration: 30 minutes`
9. Remove a temporary ban: `/bt unfakeban Alice`
   - System message: `Successfully removed temporary ban: Alice`
10. Kick the player named `Steve`: `/bt kick Steve Violation of rules`

### Tab Completion Demo
- Type `/bt ` and press Tab: shows all available commands (filtered by permissions)
- Type `/bt ban ` and press Tab: shows the online player list (excluding whitelisted players)
- Type `/bt ban PlayerName ` and press Tab: shows common ban reasons
- Type `/bt ban PlayerName Cheating ` and press Tab: shows duration options (1h, 6h, 1d, 7d, etc.)
- Type `/bt unban ` and press Tab: shows the list of banned players
- Type `/bt unfakeban ` and press Tab: shows the list of temporarily banned players

---

## ⚠️ Security Notes

### Security Advice
- Be cautious when assigning `bantools.command.kick` and `bantools.command.ban` permissions
- Regularly check if ban records in the config file are loaded correctly
- It is recommended to use with other security plugins, such as IP whitelist and anti-cheat plugins
- Test in a staging environment before deploying on important servers

---

## 🛠️ Troubleshooting

### Frequently Asked Questions

**Q: After restarting the server, I get "Invalid data type for player 'xxx.state'" error**
A: This is a config flattening issue, which is automatically fixed in v1.3.1. The plugin will display "Detected flattened config file, attempting repair..." and automatically rebuild the config.

**Q: Offline banned players are not loaded correctly**
A: Make sure you are using v1.3.1 or later, which fixes offline player handling logic.

**Q: What if the config file is corrupted?**
A: The plugin will automatically back up the corrupted config file (filename includes a timestamp), then recreate the default config.

**Q: Permission setup issues**
A: Make sure to assign permissions correctly:
- `bantools.command.ban` - Ban permission
- `bantools.command.kick` - Kick permission
- `bantools.command.unban` - Unban permission
- `bantools.command.reload` - Reload permission

**Q: Unban command not working or conflicts with other plugins**
A: In v1.3.2, the unban command was integrated into `/bt unban` and the standalone `/unban` command was removed to avoid plugin conflicts.

**Q: Message: "The player is not banned or has already been unbanned"**
A: This means the player currently has no valid ban record, and may have already been unbanned or was never banned.

### Config File Format

The correct config file format should be:
```hocon
defaults {
  ban_reason = "Violation of server rules"
  kick_reason = "Kicked by an administrator"
}

bans {
  "PlayerName": {
    name: "PlayerName"
    uuid: "player-uuid-here"  # Filled automatically when banning online
    ip: "player-ip-here"      # Filled automatically when banning online
    reason: "Ban reason"
    start_time: 1698765432
    end_time: null            # null means permanent ban
    state: true               # true means active
  }
  "OfflinePlayer": {
    name: "OfflinePlayer"
    uuid: null                # Offline ban, updated on login
    ip: null                  # Offline ban, updated on login
    reason: "Offline ban"
    start_time: 1698765432
    end_time: null
    state: true
  }
}
```

---

### Support and Feedback
If you encounter any issues while using the plugin or have suggestions for improvement, please contact me via:

- **GitHub Issues** : [Submit an issue](https://github.com/NSrank/BanTools/issues)

---

### Copyright
- Development statement: This plugin is AI-developed to provide an efficient ban management tool for the Minecraft Velocity community.
- License: This plugin is licensed under the GNU General Public License v3.0. You are free to use, modify, and distribute it, but must comply with the license terms.
- Disclaimer: The developer is not responsible for any issues caused by the use of this plugin.

---

### Special Thanks
Thanks to the following technologies and tools for supporting this plugin:

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
  ban_reason = "Violation of server rules"
  kick_reason = "Kicked by an administrator"
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
  - System response: `The player is already banned! Reason: Cheating, Duration: until 2024/01/17`
5. Unban a player named `Steve`: `/bt unban Steve`
6. Try to unban an already unbanned player: `/bt unban Steve`
  - System response: `The player is not banned or has already been unbanned!`
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
A: This is a config file flattening issue, automatically fixed in v1.3.1. The plugin will show "Detected flattened config file, attempting repair..." and automatically rebuild the configuration.

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

**Q: Getting "The player is not banned or has already been unbanned" message**
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