package net.onewaycraft.oneWayTPA.api.event;

import net.onewaycraft.oneWayTPA.TeleportRequest;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a sender retracts their own still-pending, not-yet-accepted teleport request
 * via {@code /tpacancel} (with {@code tpacancel.retract-pending} enabled) or via
 * {@link net.onewaycraft.oneWayTPA.api.OneWayTpaApi#retractPendingRequests(Player)}.
 * <p>
 * This event is informational and cannot be cancelled — by the time it fires, the
 * underlying request has already been removed from the pending map.
 * <p>
 * Distinct from {@link TpaRequestDenyEvent}, which is fired when the <em>receiver</em>
 * denies the request. External plugins filtering on "request torn down by user action"
 * should listen to both.
 */
public class TpaRequestRetractEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final TeleportRequest request;
    private final Player sender;

    public TpaRequestRetractEvent(TeleportRequest request, Player sender) {
        this.request = request;
        this.sender = sender;
    }

    public TeleportRequest getRequest() {
        return request;
    }

    public Player getSender() {
        return sender;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
