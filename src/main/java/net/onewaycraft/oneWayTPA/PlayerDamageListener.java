package net.onewaycraft.oneWayTPA;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Cancels {@link EntityDamageEvent} for players who are inside their post-teleport
 * invulnerability window. Runs at {@link EventPriority#HIGH} with
 * {@code ignoreCancelled = true} so our cancellation overrides plugins that would
 * deal damage, while leaving already-cancelled events alone.
 * <p>
 * Cancels all damage sources — PvP, mob attacks, fall damage, lava, void, etc.
 * This matches the Essentials behaviour and keeps the rule predictable.
 */
public final class PlayerDamageListener implements Listener {

    private final OneWayTPA plugin;

    public PlayerDamageListener(OneWayTPA plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        TpaService service = plugin.getTpaService();
        if (service == null) {
            return;
        }
        if (service.invulnerabilityService().isInvulnerable(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
