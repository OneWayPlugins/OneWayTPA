package net.onewaycraft.oneWayTPA;

import net.onewaycraft.oneWayTPA.api.event.TpaTeleportCompleteEvent;
import net.onewaycraft.oneWayTPA.api.event.TpaTeleportEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class TeleportService {

    private final Plugin plugin;
    private final MessageService messages;
    private final PluginSettings settings;
    private final long delaySeconds;
    private final AtomicLong teleportIdCounter = new AtomicLong();
    private final Map<Long, PendingTeleport> pendingTeleports = new ConcurrentHashMap<>();

    public TeleportService(Plugin plugin, MessageService messages, PluginSettings settings) {
        this.plugin = plugin;
        this.messages = messages;
        this.settings = settings;
        this.delaySeconds = Math.max(0L, settings.teleportDelaySeconds());
    }

    public void schedule(TeleportRequest request,
                         UUID teleportedPlayerId,
                         String teleportedPlayerName,
                         UUID destinationOwnerId,
                         Location destination) {
        PendingTeleport pending = new PendingTeleport(
                teleportIdCounter.incrementAndGet(),
                request.senderId(),
                request.senderName(),
                request.receiverId(),
                request.receiverName(),
                teleportedPlayerId,
                teleportedPlayerName,
                destinationOwnerId,
                destination.clone()
        );

        if (delaySeconds <= 0L) {
            execute(pending);
            return;
        }

        pendingTeleports.put(pending.id(), pending);
        notifyParticipants(
                pending,
                "teleport-delay",
                Map.of("seconds", String.valueOf(delaySeconds), "command", "/tpacancel")
        );

        plugin.getServer().getGlobalRegionScheduler().runDelayed(
                plugin,
                task -> runScheduledTeleport(pending.id()),
                delaySeconds * 20L
        );
    }

    public boolean cancelByParticipant(Player actor) {
        UUID actorId = actor.getUniqueId();
        for (Map.Entry<Long, PendingTeleport> entry : pendingTeleports.entrySet()) {
            PendingTeleport pending = entry.getValue();
            if (!pending.involves(actorId)) {
                continue;
            }
            if (!pendingTeleports.remove(entry.getKey(), pending)) {
                continue;
            }
            notifyParticipants(pending, "teleport-cancelled-by", Map.of("player", actor.getName()));
            return true;
        }
        return false;
    }

    public boolean isInvolvedInPendingTeleport(UUID playerId) {
        for (PendingTeleport pending : pendingTeleports.values()) {
            if (pending.involves(playerId)) {
                return true;
            }
        }
        return false;
    }

    public void cancelByParticipantSilently(UUID playerId) {
        for (Map.Entry<Long, PendingTeleport> entry : pendingTeleports.entrySet()) {
            PendingTeleport pending = entry.getValue();
            if (pending.involves(playerId)) {
                if (pendingTeleports.remove(entry.getKey(), pending)) {
                    notifyParticipants(pending, "teleport-cancelled-offline", Map.of("player", pending.teleportedPlayerName()));
                }
            }
        }
    }

    public void shutdown() {
        pendingTeleports.clear();
    }

    private void runScheduledTeleport(long pendingId) {
        PendingTeleport pending = pendingTeleports.remove(pendingId);
        if (pending == null) {
            return;
        }
        execute(pending);
    }

    private void execute(PendingTeleport pending) {
        Player teleported = Bukkit.getPlayer(pending.teleportedPlayerId());
        if (teleported == null || !teleported.isOnline()) {
            notifyParticipants(pending, "teleport-cancelled-offline", Map.of("player", pending.teleportedPlayerName()));
            return;
        }

        Location destination = resolveDestination(pending);
        if (destination == null) {
            notifyParticipants(pending, "teleport-failed-destination-offline",
                    Map.of("player", pending.teleportedPlayerName()));
            return;
        }

        TpaTeleportEvent teleportEvent = new TpaTeleportEvent(
                teleported, pending.requesterId(), pending.receiverId(), destination);
        Bukkit.getPluginManager().callEvent(teleportEvent);
        if (teleportEvent.isCancelled()) {
            notifyParticipants(pending, "teleport-failed", Map.of("player", pending.teleportedPlayerName()));
            return;
        }

        Location finalDestination = destination;
        teleported.teleportAsync(finalDestination).whenComplete((success, throwable) -> {
            if (throwable != null || !Boolean.TRUE.equals(success)) {
                notifyParticipants(pending, "teleport-failed", Map.of("player", pending.teleportedPlayerName()));
                return;
            }

            TpaTeleportCompleteEvent completeEvent = new TpaTeleportCompleteEvent(
                    teleported, pending.requesterId(), pending.receiverId(), finalDestination);
            completeEvent.callEvent();

            sendSuccessMessage(pending.requesterId(), pending.teleportedPlayerId(), pending.teleportedPlayerName());
            if (!pending.receiverId().equals(pending.requesterId())) {
                sendSuccessMessage(pending.receiverId(), pending.teleportedPlayerId(), pending.teleportedPlayerName());
            }
        });
    }

    private Location resolveDestination(PendingTeleport pending) {
        if (!settings.teleportToLiveLocation()) {
            return pending.destination();
        }
        Player owner = Bukkit.getPlayer(pending.destinationOwnerId());
        if (owner == null || !owner.isOnline()) {
            return null;
        }
        return owner.getLocation().clone();
    }

    private void sendSuccessMessage(UUID recipientId, UUID teleportedId, String teleportedName) {
        Player recipient = Bukkit.getPlayer(recipientId);
        if (recipient == null || !recipient.isOnline()) {
            return;
        }
        if (recipientId.equals(teleportedId)) {
            messages.send(recipient, "teleport-success-self");
            return;
        }
        messages.send(recipient, "teleport-success-other", Map.of("player", teleportedName));
    }

    private void notifyParticipants(PendingTeleport pending, String key, Map<String, String> placeholders) {
        Player requester = Bukkit.getPlayer(pending.requesterId());
        if (requester != null && requester.isOnline()) {
            messages.send(requester, key, placeholders);
        }

        Player receiver = Bukkit.getPlayer(pending.receiverId());
        if (receiver != null && receiver.isOnline() && !receiver.getUniqueId().equals(pending.requesterId())) {
            messages.send(receiver, key, placeholders);
        }
    }
}
