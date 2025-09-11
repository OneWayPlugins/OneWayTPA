package net.onewaycraft.oneWayTPA;

import org.bukkit.Location;

import java.util.UUID;

public record PendingTeleport(
        long id,
        UUID requesterId,
        String requesterName,
        UUID receiverId,
        String receiverName,
        UUID teleportedPlayerId,
        String teleportedPlayerName,
        Location destination
) {
    public boolean involves(UUID playerId) {
        return requesterId.equals(playerId) || receiverId.equals(playerId) || teleportedPlayerId.equals(playerId);
    }
}
