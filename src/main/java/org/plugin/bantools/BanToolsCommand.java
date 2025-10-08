package org.plugin.bantools;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.*;
import java.util.stream.Collectors;

public class BanToolsCommand implements SimpleCommand {
    private final BanManager banManager;
    private final ConfigManager configManager;
    private final FakeBanManager fakeBanManager;
    private final ProxyServer server;

    public BanToolsCommand(BanManager banManager, ConfigManager configManager,
                          FakeBanManager fakeBanManager, ProxyServer server) {
        this.banManager = banManager;
        this.configManager = configManager;
        this.fakeBanManager = fakeBanManager;
        this.server = server;
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        CommandSource source = invocation.source();

        if (args.length < 1) {
            sendHelpMessage(source);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "ban":
                handleBanCommand(args, source);
                break;
            case "unban":
                handleUnbanCommand(args, source);
                break;
            case "fakeban":
                handleFakeBanCommand(args, source);
                break;
            case "unfakeban":
                handleUnFakeBanCommand(args, source);
                break;
            case "kick":
                handleKickCommand(args, source);
                break;
            case "reload":
                banManager.loadBans();
                source.sendMessage(Component.text("配置已重新加载", NamedTextColor.GREEN));
                break;
            default:
                sendHelpMessage(source);
        }
    }

    private void handleBanCommand(String[] args, CommandSource source) {
        if (args.length < 2) {
            sendBanUsage(source);
            return;
        }

        String target = args[1];
        String reason = configManager.getDefaultBanReason();
        String duration = null;

        if (args.length >= 3) {
            reason = args[2];
        }
        if (args.length >= 4) {
            duration = args[3];
        }

        String result = banManager.banPlayer(target, reason, duration);
        if (result != null) {
            // 封禁失败，显示错误信息
            source.sendMessage(Component.text(result, NamedTextColor.RED));
        } else {
            // 封禁成功
            source.sendMessage(Component.text("成功封禁玩家: " + target, NamedTextColor.GREEN));
        }
    }

    private void handleUnbanCommand(String[] args, CommandSource source) {
        if (args.length != 2) {
            sendUnbanUsage(source);
            return;
        }

        String target = args[1].trim();
        // 输入验证
        if (target.isEmpty()) {
            source.sendMessage(Component.text("玩家名不能为空", NamedTextColor.RED));
            return;
        }
        if (target.length() > 16 || !target.matches("^[a-zA-Z0-9_]{1,16}$")) {
            source.sendMessage(Component.text("无效的玩家名格式", NamedTextColor.RED));
            return;
        }

        String result = banManager.unbanPlayer(target);
        if (result != null) {
            // 解封失败，显示错误信息
            source.sendMessage(Component.text(result, NamedTextColor.RED));
        } else {
            // 解封成功
            source.sendMessage(Component.text("已解封玩家: " + target, NamedTextColor.GREEN));
        }
    }

    private void handleFakeBanCommand(String[] args, CommandSource source) {
        if (args.length < 2) {
            sendFakeBanUsage(source);
            return;
        }

        String target = args[1].trim();
        String reason = null;
        if (args.length >= 3) {
            reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        }

        // 获取管理员名称
        String adminName = source instanceof Player ? ((Player) source).getUsername() : "Console";

        String result = fakeBanManager.confirmFakeBan(adminName, target, reason);
        if (result != null) {
            source.sendMessage(Component.text(result, NamedTextColor.YELLOW));
        }
    }

    private void handleUnFakeBanCommand(String[] args, CommandSource source) {
        if (args.length != 2) {
            sendUnFakeBanUsage(source);
            return;
        }

        String target = args[1].trim();
        String result = fakeBanManager.unFakeBan(target);
        if (result != null) {
            if (result.startsWith("成功")) {
                source.sendMessage(Component.text(result, NamedTextColor.GREEN));
            } else {
                source.sendMessage(Component.text(result, NamedTextColor.RED));
            }
        }
    }

    private void handleKickCommand(String[] args, CommandSource source) {
        if (args.length < 2) {
            sendKickUsage(source);
            return;
        }

        String target = args[1];
        String reason = configManager.getDefaultKickReason();

        if (args.length >= 3) {
            reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        }

        String result = banManager.kickPlayer(target, reason);
        if (result != null) {
            // 踢出失败，显示错误信息
            source.sendMessage(Component.text(result, NamedTextColor.RED));
        } else {
            // 踢出成功
            source.sendMessage(Component.text("已踢出玩家: " + target, NamedTextColor.GREEN));
        }
    }

    private void sendHelpMessage(CommandSource source) {
        source.sendMessage(Component.text("BanTools 使用说明", NamedTextColor.YELLOW));
        sendBanUsage(source);
        sendUnbanUsage(source);
        sendFakeBanUsage(source);
        sendUnFakeBanUsage(source);
        sendKickUsage(source);
        source.sendMessage(Component.text("/bt reload - 重新加载配置", NamedTextColor.GOLD));
    }

    private void sendBanUsage(CommandSource source) {
        source.sendMessage(Component.text("封禁用法: /bt ban <玩家> [原因] [时长]", NamedTextColor.RED));
    }

    private void sendUnbanUsage(CommandSource source) {
        source.sendMessage(Component.text("解封用法: /bt unban <玩家>", NamedTextColor.RED));
    }

    private void sendFakeBanUsage(CommandSource source) {
        source.sendMessage(Component.text("临时封禁用法: /bt fakeban <玩家> [原因]", NamedTextColor.RED));
    }

    private void sendUnFakeBanUsage(CommandSource source) {
        source.sendMessage(Component.text("解除临时封禁用法: /bt unfakeban <玩家>", NamedTextColor.RED));
    }

    private void sendKickUsage(CommandSource source) {
        source.sendMessage(Component.text("踢出用法: /bt kick <玩家> [原因]", NamedTextColor.RED));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length < 1) {
            return invocation.source().hasPermission("bantools.command.ban") ||
                    invocation.source().hasPermission("bantools.command.kick") ||
                    invocation.source().hasPermission("bantools.command.reload");
        }

        switch (args[0].toLowerCase()) {
            case "ban":
                return invocation.source().hasPermission("bantools.command.ban");
            case "unban":
                return invocation.source().hasPermission("bantools.command.unban");
            case "fakeban":
                return invocation.source().hasPermission("bantools.command.fakeban");
            case "unfakeban":
                return invocation.source().hasPermission("bantools.command.unfakeban");
            case "kick":
                return invocation.source().hasPermission("bantools.command.kick");
            case "reload":
                return invocation.source().hasPermission("bantools.command.reload");
            default:
                return false;
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        CommandSource source = invocation.source();

        // 如果没有参数，返回所有可用的子命令
        if (args.length <= 1) {
            List<String> suggestions = new ArrayList<>();
            String input = args.length == 0 ? "" : args[0].toLowerCase();

            // 根据权限添加可用命令
            if (source.hasPermission("bantools.command.ban") && "ban".startsWith(input)) {
                suggestions.add("ban");
            }
            if (source.hasPermission("bantools.command.unban") && "unban".startsWith(input)) {
                suggestions.add("unban");
            }
            if (source.hasPermission("bantools.command.fakeban") && "fakeban".startsWith(input)) {
                suggestions.add("fakeban");
            }
            if (source.hasPermission("bantools.command.unfakeban") && "unfakeban".startsWith(input)) {
                suggestions.add("unfakeban");
            }
            if (source.hasPermission("bantools.command.kick") && "kick".startsWith(input)) {
                suggestions.add("kick");
            }
            if (source.hasPermission("bantools.command.reload") && "reload".startsWith(input)) {
                suggestions.add("reload");
            }

            return suggestions;
        }

        // 根据子命令提供参数补全
        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "ban":
            case "fakeban":
            case "kick":
                return suggestPlayersForBan(args);
            case "unban":
                return suggestPlayersForUnban(args);
            case "unfakeban":
                return suggestPlayersForUnfakeban(args);
            default:
                return Collections.emptyList();
        }
    }

    /**
     * 为ban/fakeban/kick命令提供玩家名补全
     */
    private List<String> suggestPlayersForBan(String[] args) {
        if (args.length == 2) {
            // 第二个参数：玩家名
            String input = args[1].toLowerCase();
            return server.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .filter(name -> !banManager.isWhitelisted(name)) // 过滤白名单玩家
                    .collect(Collectors.toList());
        } else if (args.length == 3) {
            // 第三个参数：原因提示
            return Arrays.asList("违反服务器规则", "作弊行为", "恶意破坏", "挂机行为", "不当言论");
        } else if (args.length == 4 && "ban".equals(args[0].toLowerCase())) {
            // 第四个参数（仅ban命令）：时长提示
            return Arrays.asList("1h", "6h", "1d", "3d", "7d", "30d", "永久");
        }
        return Collections.emptyList();
    }

    /**
     * 为unban命令提供被封禁玩家名补全
     */
    private List<String> suggestPlayersForUnban(String[] args) {
        if (args.length == 2) {
            String input = args[1].toLowerCase();
            return banManager.getBannedPlayers().stream()
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * 为unfakeban命令提供被临时封禁玩家名补全
     */
    private List<String> suggestPlayersForUnfakeban(String[] args) {
        if (args.length == 2) {
            String input = args[1].toLowerCase();
            return fakeBanManager.getFakeBannedPlayers().stream()
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}