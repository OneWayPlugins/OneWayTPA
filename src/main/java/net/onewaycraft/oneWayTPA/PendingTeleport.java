package net.onewaycraft.oneWayTPA;

import org.bukkit.Location;

import java.util.UUID;

/**
 * Snapshot of a scheduled teleport, sitting in {@link TeleportService}'s pending map
 * during the warmup window.
 * <p>
 * {@code destination} holds the location captured at {@code /tpaccept} time. When
 * {@code teleport-to-live-location} is false (the default) it is the final destination
 * used at execute-time. When the flag is true, the destination is re-read from
 * {@code destinationOwnerId}'s online player at execute-time and {@code destination}
 * is only kept as a fallback for event payloads.
 */
public record PendingTeleport(
        long id,
        UUID requesterId,
        String requesterName,
        UUID receiverId,
        String receiverName,
        UUID teleportedPlayerId,
        String teleportedPlayerName,
        UUID destinationOwnerId,
        Location destination
) {
    public boolean involves(UUID playerId) {
        return requesterId.equals(playerId) || receiverId.equals(playerId) || teleportedPlayerId.equals(playerId);
    }
}
