package net.onewaycraft.oneWayTPA.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Single shared listener for OneWayTPA GUIs. Cancels all clicks and drags in
 * any inventory owned by a {@link BaseInventoryHolder} (the GUIs are read-only),
 * then routes the click to the holder's per-slot handler.
 */
public final class GuiListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BaseInventoryHolder holder)) {
            return;
        }
        event.setCancelled(true);
        holder.dispatch(event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof BaseInventoryHolder) {
            event.setCancelled(true);
        }
    }
}
