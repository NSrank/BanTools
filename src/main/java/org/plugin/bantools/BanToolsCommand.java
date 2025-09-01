package org.plugin.bantools;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Arrays;

public class BanToolsCommand implements SimpleCommand {
    private final BanManager banManager;
    private final ConfigManager configManager;

    public BanToolsCommand(BanManager banManager, ConfigManager configManager) {
        this.banManager = banManager;
        this.configManager = configManager;
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

        banManager.kickPlayer(target, reason);
        source.sendMessage(Component.text("已踢出玩家: " + target, NamedTextColor.GREEN));
    }

    private void sendHelpMessage(CommandSource source) {
        source.sendMessage(Component.text("BanTools 使用说明", NamedTextColor.YELLOW));
        sendBanUsage(source);
        sendUnbanUsage(source);
        sendKickUsage(source);
        source.sendMessage(Component.text("/bt reload - 重新加载配置", NamedTextColor.GOLD));
    }

    private void sendBanUsage(CommandSource source) {
        source.sendMessage(Component.text("封禁用法: /bt ban <玩家> [原因] [时长]", NamedTextColor.RED));
    }

    private void sendUnbanUsage(CommandSource source) {
        source.sendMessage(Component.text("解封用法: /bt unban <玩家>", NamedTextColor.RED));
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
            case "kick":
                return invocation.source().hasPermission("bantools.command.kick");
            case "reload":
                return invocation.source().hasPermission("bantools.command.reload");
            default:
                return false;
        }
    }
}