package net.onewaycraft.oneWayTPA;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Console- and admin-friendly meta-command. Backs {@code /onewaytpa}.
 * <p>
 * Subcommands:
 * <ul>
 *   <li>{@code reload} — reloads configuration and messages (requires {@code onewaytpa.admin.reload}).</li>
 *   <li>{@code version} — prints the plugin version (requires {@code onewaytpa.admin.version}).</li>
 *   <li>{@code cancel <player>} — cancels the player's in-flight teleport regardless of any
 *       config flag — admin override (requires {@code onewaytpa.admin.cancel}).</li>
 *   <li>{@code info <player>} — prints a state snapshot for the given online player
 *       (requires {@code onewaytpa.admin.info}).</li>
 * </ul>
 * Unlike the player-facing commands, this command accepts {@link CommandSender} so it runs from console.
 */
public final class OneWayTpaAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("reload", "version", "cancel", "info");

    private final OneWayTPA plugin;
    private final MessageService messages;
    private final TpaService service;

    public OneWayTpaAdminCommand(OneWayTPA plugin, MessageService messages, TpaService service) {
        this.plugin = plugin;
        this.messages = messages;
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            messages.send(sender, "admin-usage");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> handleReload(sender);
            case "version" -> handleVersion(sender);
            case "cancel" -> handleCancel(sender, args);
            case "info" -> handleInfo(sender, args);
            default -> {
                messages.send(sender, "admin-unknown-subcommand", Map.of("subcommand", args[0]));
                messages.send(sender, "admin-usage");
            }
        }
        return true;
    }

    private void handleReload(CommandSender sender) {
        if (!requirePermission(sender, "onewaytpa.admin.reload")) {
            return;
        }
        plugin.reload();
        messages.send(sender, "admin-reload-success");
    }

    private void handleVersion(CommandSender sender) {
        if (!requirePermission(sender, "onewaytpa.admin.version")) {
            return;
        }
        messages.send(sender, "admin-version", Map.of("version", plugin.getPluginMeta().getVersion()));
    }

    private void handleCancel(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "onewaytpa.admin.cancel")) {
            return;
        }
        if (args.length != 2) {
            messages.send(sender, "admin-usage");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            messages.send(sender, "admin-player-not-found", Map.of("player", args[1]));
            return;
        }
        if (service.cancelPendingTeleport(target)) {
            messages.send(sender, "admin-cancel-success", Map.of("player", target.getName()));
        } else {
            messages.send(sender, "admin-cancel-none", Map.of("player", target.getName()));
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "onewaytpa.admin.info")) {
            return;
        }
        if (args.length != 2) {
            messages.send(sender, "admin-usage");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            messages.send(sender, "admin-player-not-found", Map.of("player", args[1]));
            return;
        }

        PlayerInfoSnapshot info = service.getPlayerInfo(target);
        String yes = messages.rawString("admin-info-line-state-yes", "yes");
        String no = messages.rawString("admin-info-line-state-no", "no");

        messages.send(sender, "admin-info-header", Map.of("player", target.getName()));
        messages.send(sender, "admin-info-line-cooldown",
                Map.of("seconds", String.valueOf(info.cooldownRemainingSeconds())));
        messages.send(sender, "admin-info-line-auto-accept-all",
                Map.of("state", info.autoAcceptAll() ? yes : no));
        messages.send(sender, "admin-info-line-ignore-all",
                Map.of("state", info.ignoreAll() ? yes : no));
        messages.send(sender, "admin-info-line-pending-incoming",
                Map.of("count", String.valueOf(info.incomingPendingCount())));
        messages.send(sender, "admin-info-line-pending-outgoing",
                Map.of("count", String.valueOf(info.outgoingPendingCount())));
        messages.send(sender, "admin-info-line-in-flight",
                Map.of("state", info.involvedInFlightTeleport() ? yes : no));
    }

    private boolean requirePermission(CommandSender sender, String node) {
        if (sender.hasPermission(node)) {
            return true;
        }
        messages.send(sender, "admin-no-permission");
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> filtered = new ArrayList<>();
            String typed = args[0].toLowerCase(Locale.ROOT);
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(typed) && sender.hasPermission("onewaytpa.admin." + sub)) {
                    filtered.add(sub);
                }
            }
            return filtered;
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if ((sub.equals("cancel") || sub.equals("info"))
                    && sender.hasPermission("onewaytpa.admin." + sub)) {
                String typed = args[1].toLowerCase(Locale.ROOT);
                List<String> names = new ArrayList<>();
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.getName().toLowerCase(Locale.ROOT).startsWith(typed)) {
                        names.add(online.getName());
                    }
                }
                return names;
            }
        }
        return List.of();
    }
}
