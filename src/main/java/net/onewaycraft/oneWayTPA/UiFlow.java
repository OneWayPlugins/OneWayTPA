package net.onewaycraft.oneWayTPA;

import java.util.Locale;

/**
 * Per-flow UI mode preference: each value identifies a player-facing interaction
 * that can be rendered as either an inventory GUI or the legacy chat prompts.
 */
public enum UiFlow {
    RECEIVE,
    SEND,
    PENDING;

    public String configKey() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static UiFlow fromKey(String key) {
        if (key == null) {
            return null;
        }
        try {
            return UiFlow.valueOf(key.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
