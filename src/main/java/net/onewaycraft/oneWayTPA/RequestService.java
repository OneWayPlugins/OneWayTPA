package net.onewaycraft.oneWayTPA;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class RequestService {

    private final AtomicLong requestIdCounter = new AtomicLong();
    private final Map<UUID, Map<UUID, TeleportRequest>> pendingRequests = new ConcurrentHashMap<>();

    public TeleportRequest create(Player sender, Player receiver, RequestType type) {
        TeleportRequest request = new TeleportRequest(
                requestIdCounter.incrementAndGet(),
                type,
                sender.getUniqueId(),
                sender.getName(),
                receiver.getUniqueId(),
                receiver.getName()
        );

        pendingRequests
                .computeIfAbsent(receiver.getUniqueId(), ignored -> new ConcurrentHashMap<>())
                .put(sender.getUniqueId(), request);
        return request;
    }

    public TeleportRequest createVirtualAcceptedTpa(Player sender, Player receiver) {
        return new TeleportRequest(
                requestIdCounter.incrementAndGet(),
                RequestType.TPA,
                sender.getUniqueId(),
                sender.getName(),
                receiver.getUniqueId(),
                receiver.getName()
        );
    }

    public TeleportRequest removeBySenderName(UUID receiverId, String senderName) {
        Map<UUID, TeleportRequest> receiverRequests = pendingRequests.get(receiverId);
        if (receiverRequests == null || receiverRequests.isEmpty()) {
            return null;
        }

        for (Map.Entry<UUID, TeleportRequest> entry : receiverRequests.entrySet()) {
            TeleportRequest request = entry.getValue();
            if (!request.senderName().equalsIgnoreCase(senderName)) {
                continue;
            }
            if (!receiverRequests.remove(entry.getKey(), request)) {
                continue;
            }
            cleanupIfEmpty(receiverId, receiverRequests);
            return request;
        }
        return null;
    }

    public boolean expire(TeleportRequest request) {
        Map<UUID, TeleportRequest> receiverRequests = pendingRequests.get(request.receiverId());
        if (receiverRequests == null) {
            return false;
        }
        if (!receiverRequests.remove(request.senderId(), request)) {
            return false;
        }
        cleanupIfEmpty(request.receiverId(), receiverRequests);
        return true;
    }

    public List<String> pendingSenders(UUID receiverId) {
        Map<UUID, TeleportRequest> receiverRequests = pendingRequests.get(receiverId);
        if (receiverRequests == null || receiverRequests.isEmpty()) {
            return List.of();
        }

        List<String> names = new ArrayList<>();
        for (TeleportRequest request : receiverRequests.values()) {
            names.add(request.senderName());
        }
        names.sort(Comparator.comparing(String::toLowerCase));
        return names;
    }

    public boolean hasPendingRequest(UUID receiverId, UUID senderId) {
        Map<UUID, TeleportRequest> receiverRequests = pendingRequests.get(receiverId);
        return receiverRequests != null && receiverRequests.containsKey(senderId);
    }

    public void removeByPlayer(UUID playerId) {
        pendingRequests.remove(playerId);

        for (Map.Entry<UUID, Map<UUID, TeleportRequest>> entry : pendingRequests.entrySet()) {
            Map<UUID, TeleportRequest> receiverRequests = entry.getValue();
            receiverRequests.remove(playerId);
            if (receiverRequests.isEmpty()) {
                pendingRequests.remove(entry.getKey(), receiverRequests);
            }
        }
    }

    public void clear() {
        pendingRequests.clear();
    }

    private void cleanupIfEmpty(UUID receiverId, Map<UUID, TeleportRequest> receiverRequests) {
        if (receiverRequests.isEmpty()) {
            pendingRequests.remove(receiverId, receiverRequests);
        }
    }
}
