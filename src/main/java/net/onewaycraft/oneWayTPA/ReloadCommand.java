package net.onewaycraft.oneWayTPA;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Executor for {@code /tpareload}. Delegates to {@link OneWayTPA#reload()} and
 * responds to the sender via {@link MessageService}. Equivalent to
 * {@code /onewaytpa reload}; kept for users who prefer the short command.
 */
public final class ReloadCommand implements CommandExecutor {

    private final OneWayTPA plugin;

    public ReloadCommand(OneWayTPA plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        plugin.reload();
        plugin.getMessages().send(sender, "reload-success");
        return true;
    }
}
