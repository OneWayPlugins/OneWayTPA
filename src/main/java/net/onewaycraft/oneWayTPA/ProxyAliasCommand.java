package net.onewaycraft.oneWayTPA;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;

import java.util.List;

/**
 * Runtime-registered alias command that proxies execution to a canonical
 * {@link PluginCommand} declared in {@code plugin.yml}. Used to power
 * {@code command-aliases} entries from {@code config.yml} without requiring
 * the JAR's {@code plugin.yml} to declare aliases statically.
 * <p>
 * The proxy mirrors the canonical command's permission, description, usage,
 * and tab completer. Tab completion is delegated to the canonical command's
 * {@link TabCompleter}, if present.
 * <p>
 * Lifecycle: created and registered by {@link AliasRegistrar} during
 * {@code onEnable()}; unregistered during reload and {@code onDisable()}.
 */
final class ProxyAliasCommand extends Command {

    private final PluginCommand canonical;

    ProxyAliasCommand(String aliasName, PluginCommand canonical) {
        super(aliasName);
        this.canonical = canonical;
        setPermission(canonical.getPermission());
        setDescription("Alias for /" + canonical.getName());
        setUsage(canonical.getUsage());
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!testPermission(sender)) {
            return true;
        }
        return canonical.execute(sender, label, args);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        TabCompleter completer = canonical.getTabCompleter();
        if (completer == null) {
            return List.of();
        }
        List<String> result = completer.onTabComplete(sender, canonical, alias, args);
        return result == null ? List.of() : result;
    }
}
