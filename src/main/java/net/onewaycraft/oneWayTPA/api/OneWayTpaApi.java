package net.onewaycraft.oneWayTPA.api;

import net.onewaycraft.oneWayTPA.RequestType;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * Public API for the OneWayTPA plugin.
 * <p>
 * Obtain an instance via {@link OneWayTpaProvider#get()}.
 * <p>
 * External plugins should add {@code depend: [OneWayTPA]} or
 * {@code softdepend: [OneWayTPA]} in their plugin.yml.
 */
public interface OneWayTpaApi {

    // ========================
    //  Requests
    // ========================

    /**
     * Programmatically sends a TPA or TPA_HERE request from sender to receiver.
     * Both players must be online. Respects cooldowns, ignore lists, auto-accept,
     * and fires {@link net.onewaycraft.oneWayTPA.api.event.TpaRequestEvent} (cancellable).
     *
     * @param sender   the player requesting to teleport
     * @param receiver the target player
     * @param type     {@link RequestType#TPA} or {@link RequestType#TPA_HERE}
     * @return {@code true} if the request was created (or auto-accepted),
     *         {@code false} if blocked by cooldown, ignore list, duplicate, or cancelled event
     */
    boolean sendRequest(Player sender, Player receiver, RequestType type);

    /**
     * Accepts the pending request sent by {@code senderName} to the receiver.
     * Fires {@link net.onewaycraft.oneWayTPA.api.event.TpaRequestAcceptEvent} (cancellable).
     *
     * @param receiver   the player accepting the request
     * @param senderName the name of the player who sent the request
     * @return {@code true} if a matching request was found and processed
     */
    boolean acceptRequest(Player receiver, String senderName);

    /**
     * Denies the pending request sent by {@code senderName} to the receiver.
     * Fires {@link net.onewaycraft.oneWayTPA.api.event.TpaRequestDenyEvent}.
     *
     * @param receiver   the player denying the request
     * @param senderName the name of the player who sent the request
     * @return {@code true} if a matching request was found and denied
     */
    boolean denyRequest(Player receiver, String senderName);

    /**
     * Cancels a pending teleport that involves the given player
     * (as sender or receiver).
     *
     * @param player the player whose teleport should be cancelled
     * @return {@code true} if a pending teleport was found and cancelled
     */
    boolean cancelPendingTeleport(Player player);

    /**
     * Retracts all still-pending outgoing teleport requests from the given sender.
     * Fires {@link net.onewaycraft.oneWayTPA.api.event.TpaRequestRetractEvent} once per
     * retracted request. Notifies online receivers via the {@code request-retracted-by-sender}
     * lang key.
     * <p>
     * Unlike {@link #cancelPendingTeleport}, this only touches requests that have not been
     * accepted yet — it does not interrupt an in-flight teleport during the warmup.
     *
     * @param sender the player retracting their own pending requests
     * @return the number of requests that were retracted (0 if none were pending)
     */
    int retractPendingRequests(Player sender);

    // ========================
    //  Query
    // ========================

    /**
     * Returns the names of players who have pending requests targeting the receiver.
     *
     * @param receiver the player to check
     * @return sorted list of sender names, or empty list
     */
    List<String> getPendingRequestSenders(Player receiver);

    /**
     * Returns the names of players who have pending requests targeting the receiver.
     *
     * @param receiverId the UUID of the receiver
     * @return sorted list of sender names, or empty list
     */
    List<String> getPendingRequestSenders(UUID receiverId);

    /**
     * Checks whether {@code senderId} has a pending request to {@code receiverId}.
     *
     * @param receiverId the UUID of the receiver
     * @param senderId   the UUID of the sender
     * @return {@code true} if a pending request exists
     */
    boolean hasPendingRequest(UUID receiverId, UUID senderId);

    // ========================
    //  Preferences
    // ========================

    /**
     * Checks if the player is auto-accepting all TPA requests.
     *
     * @param playerId the UUID of the player
     * @return {@code true} if auto-accept-all is enabled
     */
    boolean isAutoAcceptingAll(UUID playerId);

    /**
     * Checks if the receiver is auto-accepting TPA from a specific sender.
     *
     * @param receiverId the UUID of the receiver
     * @param senderId   the UUID of the sender
     * @return {@code true} if auto-accept is enabled for this pair
     */
    boolean isAutoAccepting(UUID receiverId, UUID senderId);

    /**
     * Checks if the player is ignoring all TPA requests.
     *
     * @param playerId the UUID of the player
     * @return {@code true} if ignore-all is enabled
     */
    boolean isIgnoringAll(UUID playerId);

    /**
     * Checks if the receiver is ignoring a specific sender.
     *
     * @param receiverId the UUID of the receiver
     * @param senderId   the UUID of the sender
     * @return {@code true} if the sender is being ignored
     */
    boolean isIgnoring(UUID receiverId, UUID senderId);

    /**
     * Toggles auto-accept-all for the player.
     *
     * @param player the player to toggle
     * @return the new state ({@code true} = enabled)
     */
    boolean toggleAutoAcceptAll(Player player);

    /**
     * Toggles auto-accept for a specific player pair.
     *
     * @param player the player configuring auto-accept
     * @param target the target player to auto-accept from
     * @return the new state ({@code true} = enabled)
     */
    boolean toggleAutoAcceptPlayer(Player player, Player target);

    /**
     * Toggles ignore-all for the player.
     *
     * @param player the player to toggle
     * @return the new state ({@code true} = enabled)
     */
    boolean toggleIgnoreAll(Player player);

    /**
     * Toggles ignore for a specific player pair.
     *
     * @param player the player configuring ignore
     * @param target the target player to ignore
     * @return the new state ({@code true} = enabled)
     */
    boolean toggleIgnorePlayer(Player player, Player target);

    // ========================
    //  Cooldowns
    // ========================

    /**
     * Returns the remaining cooldown in seconds for the given player.
     *
     * @param player the player to check
     * @return remaining seconds, or 0 if no cooldown is active
     */
    long getCooldownRemaining(Player player);

    // ========================
    //  Settings
    // ========================

    /**
     * Returns the configured request expiration time in seconds.
     *
     * @return expiration time in seconds
     */
    long getRequestExpireSeconds();

    /**
     * Returns the configured teleport delay in seconds.
     *
     * @return delay time in seconds
     */
    long getTeleportDelaySeconds();
}
