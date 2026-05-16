package net.onewaycraft.oneWayTPA;

import net.onewaycraft.oneWayTPA.gui.SendRequestGui;
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
 * Unified executor for the player-facing commands declared in {@code plugin.yml}:
 * {@code /tpa}, {@code /tpahere}, {@code /tpaccept}, {@code /tpadeny},
 * {@code /tpauto}, {@code /tpaignore}, {@code /tpacancel}.
 * Dispatches by {@link Command#getName()} to the corresponding handler.
 * <p>
 * {@code /tpauto} dispatches further on argument count: with no args it toggles
 * the global auto-accept; with one arg it toggles auto-accept for a specific player.
 * The legacy {@code /tpaauto} name is preserved via the runtime alias system
 * ({@code command-aliases} in {@code config.yml}).
 * <p>
 * All handlers require a {@link Player} sender — console invocations are rejected
 * with the {@code players-only} message. Admin/console operations live in
 * {@link OneWayTpaAdminCommand} instead.
 */
public final class TpaCommand implements CommandExecutor, TabCompleter {

    private final MessageService messages;
    private final TpaService service;

    public TpaCommand(MessageService messages, TpaService service) {
        this.messages = messages;
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return true;
        }

        String commandName = command.getName().toLowerCase(Locale.ROOT);
        switch (commandName) {
            case "tpa" -> handleSend(player, args, RequestType.TPA);
            case "tpahere" -> handleSend(player, args, RequestType.TPA_HERE);
            case "tpaccept" -> handleAccept(player, args);
            case "tpadeny" -> handleDeny(player, args);
            case "tpauto" -> handleTpAuto(player, args);
            case "tpaignore" -> handleIgnore(player, args);
            case "tpacancel" -> handleCancel(player, args);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void handleSend(Player sender, String[] args, RequestType type) {
        if (args.length == 0
                && service.preferenceService().isGuiEnabled(sender.getUniqueId(), UiFlow.SEND)) {
            new SendRequestGui(sender, type).open(sender);
            return;
        }
        if (args.length != 1) {
            String usageKey = type == RequestType.TPA ? "usage-tpa" : "usage-tpahere";
            messages.send(sender, usageKey);
            return;
        }

        Player target = findOnlinePlayer(args[0]);
        if (target == null) {
            messages.send(sender, "player-offline", Map.of("player", args[0]));
            return;
        }

        if (target.getUniqueId().equals(sender.getUniqueId())) {
            messages.send(sender, "cannot-self");
            return;
        }

        service.sendRequest(sender, target, type);
    }

    private void handleAccept(Player receiver, String[] args) {
        String senderName = resolveTargetSender(receiver, args, "usage-tpaccept");
        if (senderName == null) {
            return;
        }
        if (!service.acceptRequest(receiver, senderName)) {
            messages.send(receiver, "request-not-found", Map.of("player", senderName));
        }
    }

    private void handleDeny(Player receiver, String[] args) {
        String senderName = resolveTargetSender(receiver, args, "usage-tpadeny");
        if (senderName == null) {
            return;
        }
        if (!service.denyRequest(receiver, senderName)) {
            messages.send(receiver, "request-not-found", Map.of("player", senderName));
        }
    }

    private String resolveTargetSender(Player receiver, String[] args, String usageKey) {
        if (args.length == 1) {
            return args[0];
        }
        if (args.length == 0 && service.pluginSettings().acceptMostRecentWithoutArgs()) {
            String mostRecent = service.mostRecentPendingSenderName(receiver);
            if (mostRecent == null) {
                messages.send(receiver, "no-pending-requests");
                return null;
            }
            return mostRecent;
        }
        messages.send(receiver, usageKey);
        return null;
    }

    private void handleTpAuto(Player player, String[] args) {
        if (args.length == 0) {
            boolean enabled = service.toggleAutoAcceptAll(player);
            messages.send(player, enabled ? "tpauto-enabled" : "tpauto-disabled");
            return;
        }

        if (args.length == 1) {
            Player target = findOnlinePlayer(args[0]);
            if (target == null) {
                messages.send(player, "player-offline", Map.of("player", args[0]));
                return;
            }
            if (target.getUniqueId().equals(player.getUniqueId())) {
                messages.send(player, "cannot-self");
                return;
            }

            boolean enabled = service.toggleAutoAcceptPlayer(player, target);
            messages.send(
                    player,
                    enabled ? "tpaauto-enabled" : "tpaauto-disabled",
                    Map.of("player", target.getName())
            );
            return;
        }

        messages.send(player, "usage-tpauto");
    }

    private void handleIgnore(Player player, String[] args) {
        if (args.length == 0) {
            boolean enabled = service.toggleIgnoreAll(player);
            messages.send(player, enabled ? "ignore-all-enabled" : "ignore-all-disabled");
            return;
        }

        if (args.length == 1) {
            Player target = findOnlinePlayer(args[0]);
            if (target == null) {
                messages.send(player, "player-offline", Map.of("player", args[0]));
                return;
            }
            if (target.getUniqueId().equals(player.getUniqueId())) {
                messages.send(player, "cannot-self");
                return;
            }

            boolean enabled = service.toggleIgnorePlayer(player, target);
            messages.send(
                    player,
                    enabled ? "ignore-player-enabled" : "ignore-player-disabled",
                    Map.of("player", target.getName())
            );
            return;
        }

        messages.send(player, "usage-tpaignore");
    }

    private void handleCancel(Player player, String[] args) {
        if (args.length != 0) {
            messages.send(player, "usage-tpacancel");
            return;
        }

        PluginSettings settings = service.pluginSettings();
        boolean retractEnabled = settings.retractPendingEnabled();
        boolean cancelWarmupEnabled = settings.cancelWarmupEnabled();

        if (!retractEnabled && !cancelWarmupEnabled) {
            messages.send(player, "tpacancel-disabled");
            return;
        }

        if (retractEnabled) {
            int retracted = service.retractOwnPendingRequests(player);
            if (retracted > 0) {
                messages.send(player, "tpacancel-retract-success",
                        Map.of("count", String.valueOf(retracted)));
                messages.send(player, "request-retracted-self",
                        Map.of("count", String.valueOf(retracted)));
                return;
            }
        }

        if (cancelWarmupEnabled && service.cancelPendingTeleport(player)) {
            messages.send(player, "tpacancel-success");
            return;
        }

        messages.send(player, "tpacancel-none");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }
        if (args.length != 1) {
            return List.of();
        }

        String typed = args[0].toLowerCase(Locale.ROOT);
        String commandName = command.getName().toLowerCase(Locale.ROOT);

        if (commandName.equals("tpaccept") || commandName.equals("tpadeny")) {
            return filterByPrefix(service.pendingSenderNames(player), typed);
        }

        if (commandName.equals("tpa") || commandName.equals("tpahere")
                || commandName.equals("tpauto") || commandName.equals("tpaignore")) {
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getUniqueId().equals(player.getUniqueId())) {
                    continue;
                }
                names.add(online.getName());
            }
            return filterByPrefix(names, typed);
        }

        return List.of();
    }

    private Player findOnlinePlayer(String name) {
        return Bukkit.getPlayerExact(name);
    }

    private List<String> filterByPrefix(List<String> values, String prefix) {
        List<String> filtered = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                filtered.add(value);
            }
        }
        return filtered;
    }
}
