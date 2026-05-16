package net.onewaycraft.oneWayTPA.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.onewaycraft.oneWayTPA.PreferenceService;
import net.onewaycraft.oneWayTPA.TpaService;
import net.onewaycraft.oneWayTPA.UiFlow;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * 36-slot settings menu opened by {@code /tpamenu}. Exposes the global
 * auto-accept and ignore toggles, the per-flow UI mode toggles, a reset
 * button, and a shortcut to open the pending-requests GUI.
 */
public final class SettingsGui extends BaseInventoryHolder {

    private static final int SLOT_AUTO_ACCEPT = 11;
    private static final int SLOT_IGNORE = 15;
    private static final int SLOT_FLOW_RECEIVE = 19;
    private static final int SLOT_FLOW_SEND = 22;
    private static final int SLOT_FLOW_PENDING = 25;
    private static final int SLOT_RESET = 29;
    private static final int SLOT_PENDING_OPEN = 31;
    private static final int SLOT_CLOSE = 33;

    private final TpaService service;
    private final UUID openerId;

    public SettingsGui(Player opener, TpaService service) {
        this.service = service;
        this.openerId = opener.getUniqueId();
        this.inventory = Bukkit.createInventory(this,
                36, Component.text("OneWayTPA Settings", NamedTextColor.GOLD));
        render();
    }

    private void render() {
        clearHandlers();
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, GuiItems.filler());
        }

        PreferenceService prefs = service.preferenceService();

        boolean autoAccept = prefs.isAutoAcceptingAll(openerId);
        inventory.setItem(SLOT_AUTO_ACCEPT, toggleItem(
                Material.LEVER,
                "Global auto-accept",
                autoAccept,
                "Auto-accept every incoming /tpa"));
        onClick(SLOT_AUTO_ACCEPT, e -> {
            Player p = (Player) e.getWhoClicked();
            service.toggleAutoAcceptAll(p);
            render();
        });

        boolean ignore = prefs.isIgnoringAll(openerId);
        inventory.setItem(SLOT_IGNORE, toggleItem(
                Material.LEVER,
                "Global ignore",
                ignore,
                "Reject every incoming request"));
        onClick(SLOT_IGNORE, e -> {
            Player p = (Player) e.getWhoClicked();
            service.toggleIgnoreAll(p);
            render();
        });

        addFlowToggle(SLOT_FLOW_RECEIVE, UiFlow.RECEIVE, "Receive prompt");
        addFlowToggle(SLOT_FLOW_SEND, UiFlow.SEND, "Send picker");
        addFlowToggle(SLOT_FLOW_PENDING, UiFlow.PENDING, "Pending list");

        inventory.setItem(SLOT_RESET, GuiItems.named(Material.BARRIER,
                Component.text("Reset UI preferences", NamedTextColor.RED),
                List.of(Component.text("Revert to server defaults", NamedTextColor.GRAY))));
        onClick(SLOT_RESET, e -> {
            service.preferenceService().resetGuiPreferences(openerId);
            render();
        });

        inventory.setItem(SLOT_PENDING_OPEN, GuiItems.named(Material.CHEST,
                Component.text("Pending requests", NamedTextColor.YELLOW),
                List.of(Component.text("Open the pending-requests GUI", NamedTextColor.GRAY))));
        onClick(SLOT_PENDING_OPEN, e -> {
            Player p = (Player) e.getWhoClicked();
            p.closeInventory();
            new PendingRequestsGui(p, service).open(p);
        });

        inventory.setItem(SLOT_CLOSE, GuiItems.named(Material.EMERALD,
                Component.text("Close", NamedTextColor.GREEN), List.of()));
        onClick(SLOT_CLOSE, e -> ((Player) e.getWhoClicked()).closeInventory());
    }

    private void addFlowToggle(int slot, UiFlow flow, String label) {
        boolean gui = service.preferenceService().isGuiEnabled(openerId, flow);
        Material material = gui ? Material.LIME_DYE : Material.GRAY_DYE;
        NamedTextColor stateColor = gui ? NamedTextColor.GREEN : NamedTextColor.GRAY;
        inventory.setItem(slot, GuiItems.named(material,
                Component.text(label + ": ", NamedTextColor.AQUA)
                        .append(Component.text(gui ? "GUI" : "chat", stateColor)),
                List.of(Component.text("Click to switch to " + (gui ? "chat" : "GUI"),
                        NamedTextColor.GRAY))));
        onClick(slot, e -> {
            service.preferenceService().setGui(openerId, flow, !gui);
            render();
        });
    }

    private ItemStack toggleItem(Material material, String label, boolean on, String description) {
        NamedTextColor stateColor = on ? NamedTextColor.GREEN : NamedTextColor.GRAY;
        return GuiItems.named(material,
                Component.text(label + ": ", NamedTextColor.AQUA)
                        .append(Component.text(on ? "ON" : "OFF", stateColor)),
                List.of(Component.text(description, NamedTextColor.GRAY)));
    }
}
