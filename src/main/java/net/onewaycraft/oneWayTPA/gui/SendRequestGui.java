package net.onewaycraft.oneWayTPA.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.onewaycraft.oneWayTPA.RequestType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 27-slot paginated picker of online players. Click a head to send /tpa or
 * /tpahere to that player; the bottom row is navigation (prev / cancel / next).
 */
public final class SendRequestGui extends BaseInventoryHolder {

    private static final int PAGE_SIZE = 18;

    private final UUID openerId;
    private final RequestType type;
    private int page;

    public SendRequestGui(Player opener, RequestType type) {
        this.openerId = opener.getUniqueId();
        this.type = type;
        Component title = Component.text(
                type == RequestType.TPA ? "Send /tpa to..." : "Send /tpahere to...",
                NamedTextColor.GOLD);
        this.inventory = Bukkit.createInventory(this, 27, title);
        render(0);
    }

    private void render(int newPage) {
        clearHandlers();
        inventory.clear();

        List<Player> candidates = candidates();
        int totalPages = Math.max(1, (int) Math.ceil(candidates.size() / (double) PAGE_SIZE));
        this.page = Math.max(0, Math.min(newPage, totalPages - 1));

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, candidates.size());

        int slot = 0;
        for (int i = start; i < end; i++) {
            Player candidate = candidates.get(i);
            String name = candidate.getName();
            inventory.setItem(slot, GuiItems.playerHead(candidate,
                    Component.text(name, NamedTextColor.AQUA),
                    List.of(
                            Component.text("Click to send ", NamedTextColor.GRAY)
                                    .append(Component.text(type == RequestType.TPA ? "/tpa" : "/tpahere",
                                            NamedTextColor.YELLOW))
                                    .append(Component.text(" " + name, NamedTextColor.GRAY))
                    )));
            onClick(slot, e -> dispatch(e, name));
            slot++;
        }

        for (int i = 18; i < 27; i++) {
            inventory.setItem(i, GuiItems.filler());
        }

        if (page > 0) {
            inventory.setItem(18, GuiItems.named(Material.ARROW,
                    Component.text("Previous page", NamedTextColor.YELLOW), List.of()));
            onClick(18, e -> render(page - 1));
        }

        inventory.setItem(22, GuiItems.named(Material.BARRIER,
                Component.text("Cancel", NamedTextColor.RED), List.of()));
        onClick(22, e -> ((Player) e.getWhoClicked()).closeInventory());

        if (page < totalPages - 1) {
            inventory.setItem(26, GuiItems.named(Material.ARROW,
                    Component.text("Next page", NamedTextColor.YELLOW), List.of()));
            onClick(26, e -> render(page + 1));
        }
    }

    private List<Player> candidates() {
        List<Player> result = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(openerId)) {
                continue;
            }
            result.add(online);
        }
        return result;
    }

    private void dispatch(InventoryClickEvent event, String targetName) {
        Player clicker = (Player) event.getWhoClicked();
        clicker.closeInventory();
        String command = (type == RequestType.TPA ? "tpa " : "tpahere ") + targetName;
        clicker.performCommand(command);
    }
}
