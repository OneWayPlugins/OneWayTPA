package net.onewaycraft.oneWayTPA.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

/**
 * Small helpers for building inventory items with Adventure component names and
 * lores. Defaults strip the implicit italic styling Minecraft applies to renamed
 * items so item titles look like plain text.
 */
public final class GuiItems {

    private GuiItems() {
    }

    public static ItemStack named(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(stripItalic(name));
            if (lore != null && !lore.isEmpty()) {
                meta.lore(lore.stream().map(GuiItems::stripItalic).toList());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack playerHead(OfflinePlayer owner, Component name, List<Component> lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(owner);
            meta.displayName(stripItalic(name));
            if (lore != null && !lore.isEmpty()) {
                meta.lore(lore.stream().map(GuiItems::stripItalic).toList());
            }
            head.setItemMeta(meta);
        }
        return head;
    }

    public static ItemStack filler() {
        return named(Material.GRAY_STAINED_GLASS_PANE, Component.empty(), List.of());
    }

    private static Component stripItalic(Component component) {
        if (component == null) {
            return Component.empty();
        }
        return component.decoration(TextDecoration.ITALIC, false);
    }
}
