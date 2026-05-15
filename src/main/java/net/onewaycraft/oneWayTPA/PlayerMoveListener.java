package net.onewaycraft.oneWayTPA;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Cancels an in-flight teleport if the teleported player walks away from the
 * block where the warmup started. Only acts when {@code cancel-on-move} is true.
 * <p>
 * Runs at {@link EventPriority#MONITOR} with {@code ignoreCancelled = true} so we
 * never react to movement that another plugin (anti-cheat, frozen-state mod, etc.)
 * has already vetoed. {@link PlayerMoveEvent#hasChangedBlock()} pre-filters head-only
 * camera adjustments and sub-block jitter, keeping the hot path cheap.
 */
public final class PlayerMoveListener implements Listener {

    private final OneWayTPA plugin;

    public PlayerMoveListener(OneWayTPA plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) {
            return;
        }
        TpaService service = plugin.getTpaService();
        if (service == null) {
            return;
        }
        service.handlePlayerMoved(event.getPlayer());
    }
}
