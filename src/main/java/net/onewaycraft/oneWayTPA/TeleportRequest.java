package net.onewaycraft.oneWayTPA;

import java.util.UUID;

public record TeleportRequest(
        long id,
        RequestType type,
        UUID senderId,
        String senderName,
        UUID receiverId,
        String receiverName
) {
}
