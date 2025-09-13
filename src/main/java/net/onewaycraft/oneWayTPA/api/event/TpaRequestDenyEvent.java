package net.onewaycraft.oneWayTPA.api.event;

import net.onewaycraft.oneWayTPA.TeleportRequest;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a TPA request is denied.
 * This event is informational and cannot be cancelled.
 */
public class TpaRequestDenyEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final TeleportRequest request;
    private final Player receiver;

    public TpaRequestDenyEvent(TeleportRequest request, Player receiver) {
        this.request = request;
        this.receiver = receiver;
    }

    public TeleportRequest getRequest() {
        return request;
    }

    public Player getReceiver() {
        return receiver;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
