package net.onewaycraft.oneWayTPA.api.event;

import net.onewaycraft.oneWayTPA.TeleportRequest;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a TPA request expires due to timeout.
 * This event is informational and cannot be cancelled.
 */
public class TpaRequestExpireEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final TeleportRequest request;

    public TpaRequestExpireEvent(TeleportRequest request) {
        this.request = request;
    }

    public TeleportRequest getRequest() {
        return request;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
