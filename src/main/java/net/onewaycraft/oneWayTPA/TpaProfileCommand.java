package net.onewaycraft.oneWayTPA;

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
 * Executor for {@code /tpaprofile} — lets a player view and change their
 * per-flow UI preferences (GUI vs chat). State lives in {@link PreferenceService}
 * (preferences.yml); defaults come from {@link PluginSettings#uiDefaultFor}.
 */
public final class TpaProfileCommand implements CommandExecutor, TabCompleter {

    private final MessageService messages;
    private final TpaService service;

    public TpaProfileCommand(MessageService messages, TpaService service) {
        this.messages = messages;
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return true;
        }

        if (args.length == 0) {
            showProfile(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "gui" -> handleSetGui(player, args);
            case "reset" -> handleReset(player);
            default -> messages.send(player, "profile-usage");
        }
        return true;
    }

    private void showProfile(Player player) {
        messages.send(player, "profile-header");
        PreferenceService preferences = service.preferenceService();
        for (UiFlow flow : UiFlow.values()) {
            boolean gui = preferences.isGuiEnabled(player.getUniqueId(), flow);
            boolean explicit = preferences.hasGuiOverride(player.getUniqueId());
            String value = (gui ? "gui" : "chat") + (explicit ? "" : " (default)");
            messages.send(player, "profile-line",
                    Map.of("flow", flow.configKey(), "value", value));
        }
    }

    private void handleSetGui(Player player, String[] args) {
        if (args.length != 3) {
            messages.send(player, "profile-usage");
            return;
        }
        UiFlow flow = UiFlow.fromKey(args[1]);
        if (flow == null) {
            messages.send(player, "profile-unknown-flow");
            return;
        }
        Boolean enabled = parseOnOff(args[2]);
        if (enabled == null) {
            messages.send(player, "profile-usage");
            return;
        }
        service.preferenceService().setGui(player.getUniqueId(), flow, enabled);
        messages.send(player, "profile-flow-set",
                Map.of("flow", flow.configKey(), "value", enabled ? "gui" : "chat"));
    }

    private void handleReset(Player player) {
        service.preferenceService().resetGuiPreferences(player.getUniqueId());
        messages.send(player, "profile-reset");
    }

    private Boolean parseOnOff(String value) {
        String v = value.toLowerCase(Locale.ROOT);
        return switch (v) {
            case "on", "true", "gui", "yes", "enable", "enabled" -> Boolean.TRUE;
            case "off", "false", "chat", "no", "disable", "disabled" -> Boolean.FALSE;
            default -> null;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterByPrefix(List.of("gui", "reset"), args[0].toLowerCase(Locale.ROOT));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("gui")) {
            List<String> flows = new ArrayList<>();
            for (UiFlow flow : UiFlow.values()) {
                flows.add(flow.configKey());
            }
            return filterByPrefix(flows, args[1].toLowerCase(Locale.ROOT));
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("gui")) {
            return filterByPrefix(List.of("on", "off"), args[2].toLowerCase(Locale.ROOT));
        }
        return List.of();
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
