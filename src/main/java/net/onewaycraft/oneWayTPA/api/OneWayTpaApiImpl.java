package net.onewaycraft.oneWayTPA.api;

import net.onewaycraft.oneWayTPA.CooldownService;
import net.onewaycraft.oneWayTPA.PluginSettings;
import net.onewaycraft.oneWayTPA.PreferenceService;
import net.onewaycraft.oneWayTPA.RequestService;
import net.onewaycraft.oneWayTPA.RequestType;
import net.onewaycraft.oneWayTPA.TpaService;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public final class OneWayTpaApiImpl implements OneWayTpaApi {

    private final TpaService tpaService;
    private final CooldownService cooldownService;
    private final PreferenceService preferenceService;
    private final RequestService requestService;
    private final PluginSettings settings;

    public OneWayTpaApiImpl(TpaService tpaService,
                            CooldownService cooldownService,
                            PreferenceService preferenceService,
                            RequestService requestService,
                            PluginSettings settings) {
        this.tpaService = tpaService;
        this.cooldownService = cooldownService;
        this.preferenceService = preferenceService;
        this.requestService = requestService;
        this.settings = settings;
    }

    @Override
    public boolean sendRequest(Player sender, Player receiver, RequestType type) {
        return tpaService.sendRequest(sender, receiver, type);
    }

    @Override
    public boolean acceptRequest(Player receiver, String senderName) {
        return tpaService.acceptRequest(receiver, senderName);
    }

    @Override
    public boolean denyRequest(Player receiver, String senderName) {
        return tpaService.denyRequest(receiver, senderName);
    }

    @Override
    public boolean cancelPendingTeleport(Player player) {
        return tpaService.cancelPendingTeleport(player);
    }

    @Override
    public int retractPendingRequests(Player sender) {
        return tpaService.retractOwnPendingRequests(sender);
    }

    @Override
    public List<String> getPendingRequestSenders(Player receiver) {
        return tpaService.pendingSenderNames(receiver);
    }

    @Override
    public List<String> getPendingRequestSenders(UUID receiverId) {
        return requestService.pendingSenders(receiverId);
    }

    @Override
    public boolean hasPendingRequest(UUID receiverId, UUID senderId) {
        return requestService.hasPendingRequest(receiverId, senderId);
    }

    @Override
    public boolean isAutoAcceptingAll(UUID playerId) {
        return preferenceService.isAutoAcceptingAll(playerId);
    }

    @Override
    public boolean isAutoAccepting(UUID receiverId, UUID senderId) {
        return preferenceService.isAutoAccepting(receiverId, senderId);
    }

    @Override
    public boolean isIgnoringAll(UUID playerId) {
        return preferenceService.isIgnoringAll(playerId);
    }

    @Override
    public boolean isIgnoring(UUID receiverId, UUID senderId) {
        return preferenceService.isIgnoring(receiverId, senderId);
    }

    @Override
    public boolean toggleAutoAcceptAll(Player player) {
        return tpaService.toggleAutoAcceptAll(player);
    }

    @Override
    public boolean toggleAutoAcceptPlayer(Player player, Player target) {
        return tpaService.toggleAutoAcceptPlayer(player, target);
    }

    @Override
    public boolean toggleIgnoreAll(Player player) {
        return tpaService.toggleIgnoreAll(player);
    }

    @Override
    public boolean toggleIgnorePlayer(Player player, Player target) {
        return tpaService.toggleIgnorePlayer(player, target);
    }

    @Override
    public long getCooldownRemaining(Player player) {
        return cooldownService.remainingSeconds(player);
    }

    @Override
    public long getRequestExpireSeconds() {
        return settings.requestExpireSeconds();
    }

    @Override
    public long getTeleportDelaySeconds() {
        return settings.teleportDelaySeconds();
    }
}
