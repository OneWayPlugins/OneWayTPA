package net.onewaycraft.oneWayTPA.api.event;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Fired just before a teleport is executed via teleportAsync.
 * Cancelling prevents the teleport from happening.
 */
public class TpaTeleportEvent extends Event implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Player teleportedPlayer;
    private final UUID requesterId;
    private final UUID receiverId;
    private final Location destination;
    private boolean cancelled;

    public TpaTeleportEvent(Player teleportedPlayer, UUID requesterId,
                            UUID receiverId, Location destination) {
        this.teleportedPlayer = teleportedPlayer;
        this.requesterId = requesterId;
        this.receiverId = receiverId;
        this.destination = destination;
    }

    public Player getTeleportedPlayer() {
        return teleportedPlayer;
    }

    public UUID getRequesterId() {
        return requesterId;
    }

    public UUID getReceiverId() {
        return receiverId;
    }

    public Location getDestination() {
        return destination;
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
