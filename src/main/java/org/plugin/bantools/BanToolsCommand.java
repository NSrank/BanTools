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
                source.sendMessage(Component.text("Configuration reloaded", NamedTextColor.GREEN));
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
            // Ban failed, show error message
            source.sendMessage(Component.text(result, NamedTextColor.RED));
        } else {
            // Ban succeeded
            source.sendMessage(Component.text("Successfully banned player: " + target, NamedTextColor.GREEN));
        }
    }

    private void handleUnbanCommand(String[] args, CommandSource source) {
        if (args.length != 2) {
            sendUnbanUsage(source);
            return;
        }

        String target = args[1].trim();
        // Input validation
        if (target.isEmpty()) {
            source.sendMessage(Component.text("Player name cannot be empty", NamedTextColor.RED));
            return;
        }
        if (target.length() > 16 || !target.matches("^[a-zA-Z0-9_]{1,16}$")) {
            source.sendMessage(Component.text("Invalid player name format", NamedTextColor.RED));
            return;
        }

        String result = banManager.unbanPlayer(target);
        if (result != null) {
            // Unban failed, show error message
            source.sendMessage(Component.text(result, NamedTextColor.RED));
        } else {
            // Unban successful
            source.sendMessage(Component.text("Player unbanned: " + target, NamedTextColor.GREEN));
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

        // Get administrator name
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
            if (result.startsWith("Successfully") || result.startsWith("Success")) {
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
            // Kick failed, show error message
            source.sendMessage(Component.text(result, NamedTextColor.RED));
        } else {
            // Kick succeeded
            source.sendMessage(Component.text("Successfully kicked player: " + target, NamedTextColor.GREEN));
        }
    }

    private void sendHelpMessage(CommandSource source) {
        source.sendMessage(Component.text("BanTools Usage", NamedTextColor.YELLOW));
        sendBanUsage(source);
        sendUnbanUsage(source);
        sendFakeBanUsage(source);
        sendUnFakeBanUsage(source);
        sendKickUsage(source);
        source.sendMessage(Component.text("/bt reload - Reload configuration", NamedTextColor.GOLD));
    }

    private void sendBanUsage(CommandSource source) {
        source.sendMessage(Component.text("Ban usage: /bt ban <player> [reason] [duration]", NamedTextColor.RED));
    }

    private void sendUnbanUsage(CommandSource source) {
        source.sendMessage(Component.text("Unban usage: /bt unban <player>", NamedTextColor.RED));
    }

    private void sendFakeBanUsage(CommandSource source) {
        source.sendMessage(Component.text("Temporary ban usage: /bt fakeban <player> [reason]", NamedTextColor.RED));
    }

    private void sendUnFakeBanUsage(CommandSource source) {
        source.sendMessage(Component.text("Remove temporary ban usage: /bt unfakeban <player>", NamedTextColor.RED));
    }

    private void sendKickUsage(CommandSource source) {
        source.sendMessage(Component.text("Kick usage: /bt kick <player> [reason]", NamedTextColor.RED));
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

        // If no parameters, return all available subcommands
        if (args.length <= 1) {
            List<String> suggestions = new ArrayList<>();
            String input = args.length == 0 ? "" : args[0].toLowerCase();

            // Add available commands based on permissions
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

        // Provide parameter completion based on subcommand
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
     * Provide player name completions for ban/fakeban/kick commands
     */
    private List<String> suggestPlayersForBan(String[] args) {
        if (args.length == 2) {
            // Second parameter: player name
            String input = args[1].toLowerCase();
            return server.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .filter(name -> !banManager.isWhitelisted(name)) // filter whitelist players
                    .collect(Collectors.toList());
        } else if (args.length == 3) {
            // Third parameter: reason suggestions
            return Arrays.asList("Violation of server rules", "Cheating", "Griefing", "AFK", "Inappropriate language");
        } else if (args.length == 4 && "ban".equals(args[0].toLowerCase())) {
            // Fourth parameter (ban only): duration suggestions
            return Arrays.asList("1h", "6h", "1d", "3d", "7d", "30d", "permanent");
        }
        return Collections.emptyList();
    }

    /**
     * Provide completions of banned player names for the unban command
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
     * Provide completions of temporarily banned player names for the unfakeban command
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