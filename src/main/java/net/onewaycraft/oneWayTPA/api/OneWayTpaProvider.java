package net.onewaycraft.oneWayTPA.api;

/**
 * Static accessor for the OneWayTPA API.
 * <p>
 * Usage:
 * <pre>{@code
 * OneWayTpaApi api = OneWayTpaProvider.get();
 * api.sendRequest(sender, receiver, RequestType.TPA);
 * }</pre>
 */
public final class OneWayTpaProvider {

    private static OneWayTpaApi instance;

    private OneWayTpaProvider() {
    }

    /**
     * Returns the API instance.
     *
     * @return the API instance
     * @throws IllegalStateException if the plugin is not enabled
     */
    public static OneWayTpaApi get() {
        if (instance == null) {
            throw new IllegalStateException(
                    "OneWayTPA API is not available. Is the plugin enabled?");
        }
        return instance;
    }

    /**
     * Called internally by OneWayTPA on enable.
     * <b>Not for external use.</b>
     *
     * @param api the API implementation to register
     */
    public static void register(OneWayTpaApi api) {
        instance = api;
    }

    /**
     * Called internally by OneWayTPA on disable.
     * <b>Not for external use.</b>
     */
    public static void unregister() {
        instance = null;
    }
}
