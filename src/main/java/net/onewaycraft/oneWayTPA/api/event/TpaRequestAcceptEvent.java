package net.onewaycraft.oneWayTPA.api.event;

import net.onewaycraft.oneWayTPA.TeleportRequest;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired before a TPA request is accepted.
 * Cancelling prevents the teleport from being scheduled.
 * The request is still consumed (removed from pending).
 */
public class TpaRequestAcceptEvent extends Event implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final TeleportRequest request;
    private final Player receiver;
    private boolean cancelled;

    public TpaRequestAcceptEvent(TeleportRequest request, Player receiver) {
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
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
