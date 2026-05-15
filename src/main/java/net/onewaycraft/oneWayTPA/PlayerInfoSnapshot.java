package net.onewaycraft.oneWayTPA;

/**
 * Read-only snapshot of a player's current OneWayTPA state.
 * Produced by {@link TpaService#getPlayerInfo} and consumed by
 * the {@code /onewaytpa info} admin subcommand.
 */
public record PlayerInfoSnapshot(
        long cooldownRemainingSeconds,
        boolean autoAcceptAll,
        boolean ignoreAll,
        int incomingPendingCount,
        int outgoingPendingCount,
        boolean involvedInFlightTeleport
) {
}
