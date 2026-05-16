package net.onewaycraft.oneWayTPA;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.onewaycraft.oneWayTPA.api.event.TpaRequestAcceptEvent;
import net.onewaycraft.oneWayTPA.api.event.TpaRequestDenyEvent;
import net.onewaycraft.oneWayTPA.api.event.TpaRequestEvent;
import net.onewaycraft.oneWayTPA.api.event.TpaRequestExpireEvent;
import net.onewaycraft.oneWayTPA.api.event.TpaRequestRetractEvent;
import net.onewaycraft.oneWayTPA.gui.ReceiveRequestGui;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestrator service that coordinates pending teleport requests, cooldowns,
 * player preferences, and scheduled teleports.
 * <p>
 * Public entry points correspond 1:1 to the player-facing commands
 * (see {@link TpaCommand}) and the public API (see
 * {@code net.onewaycraft.oneWayTPA.api.OneWayTpaApi}).
 * <p>
 * This class is intentionally the single funnel for state mutations so the rest
 * of the plugin (commands, API, listeners) can stay thin and side-effect-free.
 */
public final class TpaService {

    private static final long TICKS_PER_SECOND = 20L;

    private final OneWayTPA plugin;
    private final MessageService messages;
    private final PluginSettings settings;
    private final PermissionResolver permissions;
    private final CooldownService cooldowns;
    private final PreferenceService preferences;
    private final RequestService requests;
    private final InvulnerabilityService invulnerability;
    private final TeleportService teleports;

    public TpaService(OneWayTPA plugin, MessageService messages, PluginSettings settings,
                      PermissionResolver permissions) {
        this.plugin = plugin;
        this.messages = messages;
        this.settings = settings;
        this.permissions = permissions;
        this.cooldowns = new CooldownService(settings.cooldownGroups());
        this.preferences = new PreferenceService(plugin, settings);
        this.requests = new RequestService();
        this.invulnerability = new InvulnerabilityService();
        this.teleports = new TeleportService(plugin, messages, settings, permissions, invulnerability);
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

        boolean autoAcceptEligible = type == RequestType.TPA
                || (type == RequestType.TPA_HERE && settings.autoAcceptIncludesTpahere());
        if (autoAcceptEligible && preferences.shouldAutoAccept(receiver, sender)) {
            cooldowns.markRequest(sender);
            TeleportRequest virtualRequest = requests.createVirtualAccepted(sender, receiver, type);

            TpaRequestAcceptEvent autoAcceptEvent = new TpaRequestAcceptEvent(virtualRequest, receiver);
            Bukkit.getPluginManager().callEvent(autoAcceptEvent);
            if (autoAcceptEvent.isCancelled()) {
                return true;
            }

            messages.send(sender, "request-auto-accepted", Map.of("player", receiver.getName()));
            messages.send(receiver, "request-auto-accepted-receiver", Map.of("player", sender.getName()));
            if (type == RequestType.TPA) {
                teleports.schedule(virtualRequest,
                        sender.getUniqueId(), sender.getName(),
                        receiver.getUniqueId(), receiver.getLocation().clone(),
                        sender.getLocation().clone());
            } else {
                teleports.schedule(virtualRequest,
                        receiver.getUniqueId(), receiver.getName(),
                        sender.getUniqueId(), sender.getLocation().clone(),
                        receiver.getLocation().clone());
            }
            maybeResetCooldownForAccept(sender.getUniqueId());
            logEvent(() -> "Request auto-accepted: " + sender.getName() + " -> " + receiver.getName() + " (" + type + ")");
            return true;
        }

        cooldowns.markRequest(sender);
        TeleportRequest request = requests.create(sender, receiver, type);
        sendRequestMessages(sender, receiver, type);

        if (settings.requestExpireSeconds() > 0L) {
            plugin.getServer().getGlobalRegionScheduler().runDelayed(
                    plugin,
                    task -> expireRequest(request),
                    settings.requestExpireSeconds() * TICKS_PER_SECOND
            );
        }
        logEvent(() -> "Request created: " + sender.getName() + " -> " + receiver.getName() + " (" + type + ")");
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
            teleports.schedule(request,
                    request.senderId(), request.senderName(),
                    receiver.getUniqueId(), receiver.getLocation().clone(),
                    sender.getLocation().clone());
            maybeResetCooldownForAccept(request.senderId());
            logEvent(() -> "Request accepted: " + request.senderName() + " -> " + receiver.getName() + " (TPA)");
            return true;
        }

        teleports.schedule(request,
                request.receiverId(), request.receiverName(),
                sender.getUniqueId(), sender.getLocation().clone(),
                receiver.getLocation().clone());
        maybeResetCooldownForAccept(request.senderId());
        logEvent(() -> "Request accepted: " + request.senderName() + " -> " + receiver.getName() + " (TPA_HERE)");
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
        if (settings.resetCooldownOnDeny()) {
            cooldowns.removePlayer(request.senderId());
        }
        logEvent(() -> "Request denied: " + request.senderName() + " -> " + receiver.getName());
        return true;
    }

    public boolean cancelPendingTeleport(Player actor) {
        return teleports.cancelByParticipant(actor);
    }

    public int retractOwnPendingRequests(Player sender) {
        List<TeleportRequest> retracted = requests.removeBySender(sender.getUniqueId());
        if (retracted.isEmpty()) {
            return 0;
        }
        for (TeleportRequest request : retracted) {
            Bukkit.getPluginManager().callEvent(new TpaRequestRetractEvent(request, sender));
            Player receiver = Bukkit.getPlayer(request.receiverId());
            if (receiver != null && receiver.isOnline()) {
                messages.send(receiver, "request-retracted-by-sender",
                        Map.of("player", sender.getName()));
            }
        }
        logEvent(() -> "Requests retracted by " + sender.getName() + ": " + retracted.size());
        return retracted.size();
    }

    public PlayerInfoSnapshot getPlayerInfo(Player player) {
        UUID id = player.getUniqueId();
        return new PlayerInfoSnapshot(
                cooldowns.remainingSeconds(player),
                preferences.isAutoAcceptingAll(id),
                preferences.isIgnoringAll(id),
                requests.incomingPendingCount(id),
                requests.outgoingPendingCount(id),
                teleports.isInvolvedInPendingTeleport(id)
        );
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

    public List<TeleportRequest> pendingRequestsFor(Player receiver) {
        return requests.pendingForReceiver(receiver.getUniqueId());
    }

    public String mostRecentPendingSenderName(Player receiver) {
        return requests.mostRecentSenderName(receiver.getUniqueId());
    }

    CooldownService cooldownService() {
        return cooldowns;
    }

    public PreferenceService preferenceService() {
        return preferences;
    }

    RequestService requestService() {
        return requests;
    }

    InvulnerabilityService invulnerabilityService() {
        return invulnerability;
    }

    public PluginSettings pluginSettings() {
        return settings;
    }

    public void handlePlayerQuit(UUID playerId) {
        teleports.cancelByParticipantSilently(playerId);
        requests.removeByPlayer(playerId);
        cooldowns.removePlayer(playerId);
        invulnerability.remove(playerId);
    }

    public void handlePlayerMoved(Player player) {
        if (!settings.cancelOnMove()) {
            return;
        }
        if (permissions.canBypassMoveCancel(player)) {
            return;
        }
        teleports.cancelIfMovedFromStart(player);
    }

    public void shutdown() {
        teleports.shutdown();
        requests.clear();
        cooldowns.clear();
        invulnerability.clear();
    }

    private void sendRequestMessages(Player sender, Player receiver, RequestType type) {
        String sentKey = type == RequestType.TPA ? "request-sent-tpa" : "request-sent-tpahere";
        String receivedKey = type == RequestType.TPA ? "request-received-tpa" : "request-received-tpahere";
        messages.send(sender, sentKey, Map.of("player", receiver.getName()));
        messages.send(receiver, receivedKey, Map.of("player", sender.getName()));
        if (shouldOpenReceiveGui(receiver)) {
            openReceiveGuiOnReceiverRegion(sender, receiver, type);
        } else {
            sendClickableDecision(receiver, sender.getName());
        }
    }

    private boolean shouldOpenReceiveGui(Player receiver) {
        if (!preferences.isGuiEnabled(receiver.getUniqueId(), UiFlow.RECEIVE)) {
            return false;
        }
        return receiver.getOpenInventory().getType() == InventoryType.CRAFTING;
    }

    private void openReceiveGuiOnReceiverRegion(Player sender, Player receiver, RequestType type) {
        receiver.getScheduler().run(plugin,
                task -> {
                    if (receiver.isOnline()) {
                        new ReceiveRequestGui(sender, type).open(receiver);
                    }
                },
                null);
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
        logEvent(() -> "Request expired: " + request.senderName() + " -> " + request.receiverName());
    }

    private void maybeResetCooldownForAccept(UUID senderId) {
        if (settings.resetCooldownOnAccept()) {
            cooldowns.removePlayer(senderId);
        }
    }

    private void logEvent(java.util.function.Supplier<String> message) {
        if (settings.logEvents()) {
            plugin.getLogger().info(message.get());
        }
    }
}
