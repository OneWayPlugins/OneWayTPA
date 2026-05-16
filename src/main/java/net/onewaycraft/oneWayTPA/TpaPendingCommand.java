package net.onewaycraft.oneWayTPA;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.onewaycraft.oneWayTPA.gui.PendingRequestsGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * Lists incoming pending TPA requests. Opens the GUI when the player has
 * UiFlow.PENDING set to gui; otherwise renders a numbered chat list with
 * clickable Accept/Deny buttons per row.
 */
public final class TpaPendingCommand implements CommandExecutor {

    private final MessageService messages;
    private final TpaService service;

    public TpaPendingCommand(MessageService messages, TpaService service) {
        this.messages = messages;
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return true;
        }

        List<TeleportRequest> pending = service.pendingRequestsFor(player);
        if (pending.isEmpty()) {
            messages.send(player, "no-pending-requests");
            return true;
        }

        if (service.preferenceService().isGuiEnabled(player.getUniqueId(), UiFlow.PENDING)) {
            new PendingRequestsGui(player, service).open(player);
            return true;
        }

        messages.send(player, "pending-header",
                Map.of("count", String.valueOf(pending.size())));
        for (TeleportRequest request : pending) {
            String senderName = request.senderName();
            Component accept = messages.noPrefix("click-accept", Map.of("player", senderName))
                    .clickEvent(ClickEvent.runCommand("/tpaccept " + senderName))
                    .hoverEvent(HoverEvent.showText(
                            messages.noPrefix("click-accept-hover", Map.of("player", senderName))));
            Component deny = messages.noPrefix("click-deny", Map.of("player", senderName))
                    .clickEvent(ClickEvent.runCommand("/tpadeny " + senderName))
                    .hoverEvent(HoverEvent.showText(
                            messages.noPrefix("click-deny-hover", Map.of("player", senderName))));
            String label2 = request.type() == RequestType.TPA ? "/tpa" : "/tpahere";
            Component line = messages.prefix()
                    .append(Component.text("- " + senderName + " (" + label2 + ") "))
                    .append(accept).append(Component.space()).append(deny);
            player.sendMessage(line);
        }
        return true;
    }
}
