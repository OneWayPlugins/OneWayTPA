package net.onewaycraft.oneWayTPA.api.event;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Fired after a successful teleport.
 * This event is informational and cannot be cancelled.
 * May fire asynchronously (from teleportAsync callback).
 */
public class TpaTeleportCompleteEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Player teleportedPlayer;
    private final UUID requesterId;
    private final UUID receiverId;
    private final Location destination;

    public TpaTeleportCompleteEvent(Player teleportedPlayer, UUID requesterId,
                                    UUID receiverId, Location destination) {
        super(!Bukkit.isPrimaryThread());
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
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
