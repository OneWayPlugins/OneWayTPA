package net.onewaycraft.oneWayTPA.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.onewaycraft.oneWayTPA.RequestType;
import net.onewaycraft.oneWayTPA.TeleportRequest;
import net.onewaycraft.oneWayTPA.TpaService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * 36-slot GUI listing the player's incoming pending requests. Each request
 * occupies one row: head | filler*3 | accept | filler*3 | deny. After accept
 * or deny the GUI rebuilds so the player can resolve multiple requests
 * without re-opening.
 */
public final class PendingRequestsGui extends BaseInventoryHolder {

    private static final int PAGE_SIZE = 3;

    private final TpaService service;
    private final UUID openerId;
    private int page;

    public PendingRequestsGui(Player opener, TpaService service) {
        this.service = service;
        this.openerId = opener.getUniqueId();
        this.inventory = Bukkit.createInventory(this,
                36, Component.text("Pending TPA Requests", NamedTextColor.GOLD));
        render(0);
    }

    private void render(int newPage) {
        clearHandlers();
        inventory.clear();

        List<TeleportRequest> pending = service.pendingRequestsFor(Bukkit.getPlayer(openerId));
        int totalPages = Math.max(1, (int) Math.ceil(pending.size() / (double) PAGE_SIZE));
        this.page = Math.max(0, Math.min(newPage, totalPages - 1));

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, pending.size());

        for (int rowIndex = 0; rowIndex < PAGE_SIZE; rowIndex++) {
            int row = rowIndex * 9;
            for (int col = 0; col < 9; col++) {
                inventory.setItem(row + col, GuiItems.filler());
            }
            int requestIndex = start + rowIndex;
            if (requestIndex >= end) {
                continue;
            }
            populateRow(row, pending.get(requestIndex));
        }

        for (int i = 27; i < 36; i++) {
            inventory.setItem(i, GuiItems.filler());
        }

        if (page > 0) {
            inventory.setItem(27, GuiItems.named(Material.ARROW,
                    Component.text("Previous page", NamedTextColor.YELLOW), List.of()));
            onClick(27, e -> render(page - 1));
        }

        inventory.setItem(31, GuiItems.named(Material.BARRIER,
                Component.text("Close", NamedTextColor.RED), List.of()));
        onClick(31, e -> ((Player) e.getWhoClicked()).closeInventory());

        if (page < totalPages - 1) {
            inventory.setItem(35, GuiItems.named(Material.ARROW,
                    Component.text("Next page", NamedTextColor.YELLOW), List.of()));
            onClick(35, e -> render(page + 1));
        }
    }

    private void populateRow(int row, TeleportRequest request) {
        String senderName = request.senderName();
        Component intent = request.type() == RequestType.TPA
                ? Component.text("wants to teleport to you", NamedTextColor.GRAY)
                : Component.text("wants you to teleport to them", NamedTextColor.GRAY);

        inventory.setItem(row, GuiItems.playerHead(Bukkit.getOfflinePlayer(request.senderId()),
                Component.text(senderName, NamedTextColor.AQUA),
                List.of(intent)));

        ItemStack accept = GuiItems.named(Material.LIME_CONCRETE,
                Component.text("ACCEPT", NamedTextColor.GREEN),
                List.of(Component.text("Accept request from " + senderName, NamedTextColor.GRAY)));
        inventory.setItem(row + 4, accept);
        onClick(row + 4, e -> resolve(e, "tpaccept " + senderName));

        ItemStack deny = GuiItems.named(Material.RED_CONCRETE,
                Component.text("DENY", NamedTextColor.RED),
                List.of(Component.text("Deny request from " + senderName, NamedTextColor.GRAY)));
        inventory.setItem(row + 8, deny);
        onClick(row + 8, e -> resolve(e, "tpadeny " + senderName));
    }

    private void resolve(org.bukkit.event.inventory.InventoryClickEvent event, String command) {
        Player clicker = (Player) event.getWhoClicked();
        clicker.performCommand(command);
        render(page);
    }
}
