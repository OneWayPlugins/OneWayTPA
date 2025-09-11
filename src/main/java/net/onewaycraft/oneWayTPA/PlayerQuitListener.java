package net.onewaycraft.oneWayTPA;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

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
