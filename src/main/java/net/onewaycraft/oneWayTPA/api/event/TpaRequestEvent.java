package net.onewaycraft.oneWayTPA.api.event;

import net.onewaycraft.oneWayTPA.RequestType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired before a TPA or TPA_HERE request is created and sent.
 * Cancelling prevents the request from being stored and any messages from being sent.
 */
public class TpaRequestEvent extends Event implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Player sender;
    private final Player receiver;
    private final RequestType type;
    private boolean cancelled;

    public TpaRequestEvent(Player sender, Player receiver, RequestType type) {
        this.sender = sender;
        this.receiver = receiver;
        this.type = type;
    }

    public Player getSender() {
        return sender;
    }

    public Player getReceiver() {
        return receiver;
    }

    public RequestType getType() {
        return type;
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
