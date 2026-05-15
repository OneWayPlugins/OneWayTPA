package net.onewaycraft.oneWayTPA;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Cleans up per-player state when a player disconnects: cancels any in-flight
 * teleport they participate in, removes pending requests sent to or by them,
 * and clears their cooldown entry. Mutation happens via
 * {@link TpaService#handlePlayerQuit(java.util.UUID)}.
 */
public final class PlayerQuitListener implements Listener {

    private final OneWayTPA plugin;

    public PlayerQuitListener(OneWayTPA plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        TpaService service = plugin.getTpaService();
        if (service != null) {
            service.handlePlayerQuit(event.getPlayer().getUniqueId());
        }
    }
}
