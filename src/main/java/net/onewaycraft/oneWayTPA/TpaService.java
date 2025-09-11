package net.onewaycraft.oneWayTPA;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.onewaycraft.oneWayTPA.api.event.TpaRequestAcceptEvent;
import net.onewaycraft.oneWayTPA.api.event.TpaRequestDenyEvent;
import net.onewaycraft.oneWayTPA.api.event.TpaRequestEvent;
import net.onewaycraft.oneWayTPA.api.event.TpaRequestExpireEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TpaService {

    private final OneWayTPA plugin;
    private final MessageService messages;
    private final PluginSettings settings;
    private final CooldownService cooldowns;
    private final PreferenceService preferences;
    private final RequestService requests;
    private final TeleportService teleports;

    public TpaService(OneWayTPA plugin, MessageService messages, PluginSettings settings) {
        this.plugin = plugin;
        this.messages = messages;
        this.settings = settings;
        this.cooldowns = new CooldownService(settings.cooldownGroups());
        this.preferences = new PreferenceService(plugin);
        this.requests = new RequestService();
        this.teleports = new TeleportService(plugin, messages, settings.teleportDelaySeconds());
    }

    public boolean sendRequest(Player sender, Player receiver, RequestType type) {
        long remainingCooldown = cooldowns.remainingSeconds(sender);
        if (remainingCooldown > 0L) {
            messages.send(sender, "cooldown-active", Map.of("seconds", String.valueOf(remainingCooldown)));
            return false;
        }

        if (preferences.isIgnoring(receiver, sender)) {
            messages.send(sender, "request-ignored", Map.of("player", receiver.getName()));
            return false;
        }

        if (requests.hasPendingRequest(receiver.getUniqueId(), sender.getUniqueId())) {
            messages.send(sender, "request-already-pending", Map.of("player", receiver.getName()));
            return false;
        }

        TpaRequestEvent requestEvent = new TpaRequestEvent(sender, receiver, type);
        Bukkit.getPluginManager().callEvent(requestEvent);
        if (requestEvent.isCancelled()) {
            return false;
        }

        if (type == RequestType.TPA && preferences.shouldAutoAccept(receiver, sender)) {
            cooldowns.markRequest(sender);
            TeleportRequest virtualRequest = requests.createVirtualAcceptedTpa(sender, receiver);

            TpaRequestAcceptEvent autoAcceptEvent = new TpaRequestAcceptEvent(virtualRequest, receiver);
            Bukkit.getPluginManager().callEvent(autoAcceptEvent);
            if (autoAcceptEvent.isCancelled()) {
                return true;
            }

            messages.send(sender, "request-auto-accepted", Map.of("player", receiver.getName()));
            messages.send(receiver, "request-auto-accepted-receiver", Map.of("player", sender.getName()));
            teleports.schedule(virtualRequest, sender.getUniqueId(), sender.getName(), receiver.getLocation().clone());
            return true;
        }

        cooldowns.markRequest(sender);
        TeleportRequest request = requests.create(sender, receiver, type);
        sendRequestMessages(sender, receiver, type);

        if (settings.requestExpireSeconds() > 0L) {
            plugin.getServer().getGlobalRegionScheduler().runDelayed(
                    plugin,
                    task -> expireRequest(request),
                    settings.requestExpireSeconds() * 20L
            );
        }
        return true;
    }

    public boolean acceptRequest(Player receiver, String senderName) {
        TeleportRequest request = requests.removeBySenderName(receiver.getUniqueId(), senderName);
        if (request == null) {
            return false;
        }

        Player sender = Bukkit.getPlayer(request.senderId());
        if (sender == null || !sender.isOnline()) {
            messages.send(receiver, "player-offline", Map.of("player", request.senderName()));
            return true;
        }

        TpaRequestAcceptEvent acceptEvent = new TpaRequestAcceptEvent(request, receiver);
        Bukkit.getPluginManager().callEvent(acceptEvent);
        if (acceptEvent.isCancelled()) {
            return true;
        }

        messages.send(receiver, "request-accepted-self", Map.of("player", request.senderName()));
        messages.send(sender, "request-accepted-sender", Map.of("player", receiver.getName()));

        if (request.type() == RequestType.TPA) {
            teleports.schedule(request, request.senderId(), request.senderName(), receiver.getLocation().clone());
            return true;
        }

        teleports.schedule(request, request.receiverId(), request.receiverName(), sender.getLocation().clone());
        return true;
    }

    public boolean denyRequest(Player receiver, String senderName) {
        TeleportRequest request = requests.removeBySenderName(receiver.getUniqueId(), senderName);
        if (request == null) {
            return false;
        }

        TpaRequestDenyEvent denyEvent = new TpaRequestDenyEvent(request, receiver);
        Bukkit.getPluginManager().callEvent(denyEvent);

        messages.send(receiver, "request-denied-self", Map.of("player", request.senderName()));
        Player sender = Bukkit.getPlayer(request.senderId());
        if (sender != null && sender.isOnline()) {
            messages.send(sender, "request-denied-sender", Map.of("player", receiver.getName()));
        }
        return true;
    }

    public boolean cancelPendingTeleport(Player actor) {
        return teleports.cancelByParticipant(actor);
    }

    public boolean toggleAutoAcceptAll(Player player) {
        return preferences.toggleAutoAcceptAll(player);
    }

    public boolean toggleAutoAcceptPlayer(Player player, Player target) {
        return preferences.toggleAutoAcceptPlayer(player, target);
    }

    public boolean toggleIgnoreAll(Player player) {
        return preferences.toggleIgnoreAll(player);
    }

    public boolean toggleIgnorePlayer(Player player, Player target) {
        return preferences.toggleIgnorePlayer(player, target);
    }

    public List<String> pendingSenderNames(Player receiver) {
        return requests.pendingSenders(receiver.getUniqueId());
    }

    CooldownService cooldownService() {
        return cooldowns;
    }

    PreferenceService preferenceService() {
        return preferences;
    }

    RequestService requestService() {
        return requests;
    }

    PluginSettings pluginSettings() {
        return settings;
    }

    public void handlePlayerQuit(UUID playerId) {
        teleports.cancelByParticipantSilently(playerId);
        requests.removeByPlayer(playerId);
        cooldowns.removePlayer(playerId);
    }

    public void shutdown() {
        teleports.shutdown();
        requests.clear();
        cooldowns.clear();
    }

    private void sendRequestMessages(Player sender, Player receiver, RequestType type) {
        String sentKey = type == RequestType.TPA ? "request-sent-tpa" : "request-sent-tpahere";
        String receivedKey = type == RequestType.TPA ? "request-received-tpa" : "request-received-tpahere";
        messages.send(sender, sentKey, Map.of("player", receiver.getName()));
        messages.send(receiver, receivedKey, Map.of("player", sender.getName()));
        sendClickableDecision(receiver, sender.getName());
    }

    private void sendClickableDecision(Player receiver, String senderName) {
        Map<String, String> placeholders = Map.of("player", senderName);
        Component acceptButton = messages.noPrefix("click-accept", placeholders)
                .clickEvent(ClickEvent.runCommand("/tpaccept " + senderName))
                .hoverEvent(HoverEvent.showText(messages.noPrefix("click-accept-hover", placeholders)));

        Component denyButton = messages.noPrefix("click-deny", placeholders)
                .clickEvent(ClickEvent.runCommand("/tpadeny " + senderName))
                .hoverEvent(HoverEvent.showText(messages.noPrefix("click-deny-hover", placeholders)));

        Component line = messages.prefix()
                .append(messages.noPrefix("click-instruction", placeholders))
                .append(Component.space())
                .append(acceptButton)
                .append(Component.space())
                .append(denyButton);
        receiver.sendMessage(line);
    }

    private void expireRequest(TeleportRequest request) {
        if (!requests.expire(request)) {
            return;
        }

        TpaRequestExpireEvent expireEvent = new TpaRequestExpireEvent(request);
        Bukkit.getPluginManager().callEvent(expireEvent);

        Player sender = Bukkit.getPlayer(request.senderId());
        if (sender != null && sender.isOnline()) {
            messages.send(sender, "request-expired-sender", Map.of("player", request.receiverName()));
        }

        Player receiver = Bukkit.getPlayer(request.receiverId());
        if (receiver != null && receiver.isOnline()) {
            messages.send(receiver, "request-expired-receiver", Map.of("player", request.senderName()));
        }
    }
}
