package net.onewaycraft.oneWayTPA;

import net.onewaycraft.oneWayTPA.gui.SettingsGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Opens the OneWayTPA settings GUI. Players-only.
 */
public final class TpaMenuCommand implements CommandExecutor {

    private final MessageService messages;
    private final TpaService service;

    public TpaMenuCommand(MessageService messages, TpaService service) {
        this.messages = messages;
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return true;
        }
        new SettingsGui(player, service).open(player);
        return true;
    }
}
