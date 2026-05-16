package net.onewaycraft.oneWayTPA.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Base holder for OneWayTPA inventory GUIs. Subclasses build the {@link Inventory}
 * and register per-slot click handlers via {@link #onClick(int, Consumer)}.
 * {@link GuiListener} routes incoming clicks to {@link #dispatch(InventoryClickEvent)}.
 */
public abstract class BaseInventoryHolder implements InventoryHolder {

    protected Inventory inventory;
    private final Map<Integer, Consumer<InventoryClickEvent>> handlers = new HashMap<>();

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    protected void onClick(int slot, Consumer<InventoryClickEvent> handler) {
        handlers.put(slot, handler);
    }

    public void dispatch(InventoryClickEvent event) {
        Consumer<InventoryClickEvent> handler = handlers.get(event.getRawSlot());
        if (handler != null) {
            handler.accept(event);
        }
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }
}
