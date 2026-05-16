package net.onewaycraft.oneWayTPA.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.onewaycraft.oneWayTPA.RequestType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

/**
 * 9-slot popup shown to the receiver of a /tpa or /tpahere request when they
 * have opted in to the GUI flow. Layout:
 * <pre>
 *  [0]  head   [1] _   [2] _   [3] ACCEPT [4] ACCEPT [5] _   [6] DENY [7] DENY [8] info
 * </pre>
 */
public final class ReceiveRequestGui extends BaseInventoryHolder {

    public ReceiveRequestGui(OfflinePlayer sender, RequestType type) {
        Component title = Component.text("TPA Request from " + sender.getName(), NamedTextColor.GOLD);
        this.inventory = Bukkit.createInventory(this, 9, title);

        String senderName = sender.getName();
        Component intent = type == RequestType.TPA
                ? Component.text("wants to teleport to you", NamedTextColor.GRAY)
                : Component.text("wants you to teleport to them", NamedTextColor.GRAY);

        inventory.setItem(0, GuiItems.playerHead(sender,
                Component.text(senderName, NamedTextColor.AQUA),
                List.of(intent)));

        inventory.setItem(1, GuiItems.filler());
        inventory.setItem(2, GuiItems.filler());

        inventory.setItem(3, acceptItem(senderName));
        inventory.setItem(4, acceptItem(senderName));
        inventory.setItem(5, GuiItems.filler());
        inventory.setItem(6, denyItem(senderName));
        inventory.setItem(7, denyItem(senderName));

        inventory.setItem(8, GuiItems.named(Material.CLOCK,
                Component.text("Pending request", NamedTextColor.YELLOW),
                List.of(Component.text("Close to ignore", NamedTextColor.GRAY))));

        onClick(3, e -> runAccept(e, senderName));
        onClick(4, e -> runAccept(e, senderName));
        onClick(6, e -> runDeny(e, senderName));
        onClick(7, e -> runDeny(e, senderName));
    }

    private static org.bukkit.inventory.ItemStack acceptItem(String senderName) {
        return GuiItems.named(Material.LIME_CONCRETE,
                Component.text("ACCEPT", NamedTextColor.GREEN),
                List.of(Component.text("Accept request from " + senderName, NamedTextColor.GRAY)));
    }

    private static org.bukkit.inventory.ItemStack denyItem(String senderName) {
        return GuiItems.named(Material.RED_CONCRETE,
                Component.text("DENY", NamedTextColor.RED),
                List.of(Component.text("Deny request from " + senderName, NamedTextColor.GRAY)));
    }

    private void runAccept(InventoryClickEvent event, String senderName) {
        Player clicker = (Player) event.getWhoClicked();
        clicker.closeInventory();
        clicker.performCommand("tpaccept " + senderName);
    }

    private void runDeny(InventoryClickEvent event, String senderName) {
        Player clicker = (Player) event.getWhoClicked();
        clicker.closeInventory();
        clicker.performCommand("tpadeny " + senderName);
    }
}
