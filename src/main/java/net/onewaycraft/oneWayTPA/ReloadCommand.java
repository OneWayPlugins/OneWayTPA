package net.onewaycraft.oneWayTPA;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class ReloadCommand implements CommandExecutor {

    private final OneWayTPA plugin;

    public ReloadCommand(OneWayTPA plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        plugin.reload();
        sender.sendMessage("§a[OneWayTPA] Configuração e mensagens recarregadas.");
        return true;
    }
}
