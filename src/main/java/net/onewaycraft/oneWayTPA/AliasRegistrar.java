package net.onewaycraft.oneWayTPA;

import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Bookkeeping helper that registers and unregisters {@link ProxyAliasCommand}
 * instances against the server's {@link CommandMap} based on the
 * {@code command-aliases} section of {@code config.yml}.
 * <p>
 * Tracks registered alias names so that a subsequent reload can fully clean
 * them up — including the {@code onewaytpa:}-namespaced form that Paper's
 * {@code SimpleCommandMap} adds automatically when registering with a
 * fallback prefix.
 */
final class AliasRegistrar {

    private static final String FALLBACK_PREFIX = "onewaytpa";

    private final JavaPlugin plugin;
    private final List<String> registeredAliases = new ArrayList<>();

    AliasRegistrar(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    void register(Map<String, String> aliasToCanonical) {
        if (aliasToCanonical.isEmpty()) {
            return;
        }
        CommandMap commandMap = plugin.getServer().getCommandMap();

        for (Map.Entry<String, String> entry : aliasToCanonical.entrySet()) {
            String alias = entry.getKey();
            String canonicalName = entry.getValue();

            PluginCommand canonical = plugin.getServer().getPluginCommand(canonicalName);
            if (canonical == null) {
                plugin.getLogger().warning("command-aliases: cannot map '" + alias
                        + "' -> '" + canonicalName + "': unknown canonical command.");
                continue;
            }

            Command existing = commandMap.getCommand(alias);
            if (existing != null) {
                plugin.getLogger().warning("command-aliases: alias '" + alias
                        + "' is already registered by another plugin or by the server; skipping.");
                continue;
            }

            ProxyAliasCommand proxy = new ProxyAliasCommand(alias, canonical);
            commandMap.register(FALLBACK_PREFIX, proxy);
            registeredAliases.add(alias);
        }
    }

    void unregisterAll() {
        if (registeredAliases.isEmpty()) {
            return;
        }
        CommandMap commandMap = plugin.getServer().getCommandMap();
        Map<String, Command> knownCommands = commandMap.getKnownCommands();

        for (String alias : registeredAliases) {
            String normalized = alias.toLowerCase(Locale.ROOT);
            Command removed = knownCommands.remove(normalized);
            knownCommands.remove(FALLBACK_PREFIX + ":" + normalized);
            if (removed != null) {
                removed.unregister(commandMap);
            }
        }
        registeredAliases.clear();
    }

    int registeredCount() {
        return registeredAliases.size();
    }
}
