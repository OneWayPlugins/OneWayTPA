package net.onewaycraft.oneWayTPA;

import net.onewaycraft.oneWayTPA.api.OneWayTpaApiImpl;
import net.onewaycraft.oneWayTPA.api.OneWayTpaProvider;
import net.onewaycraft.oneWayTPA.gui.GuiListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Entry point for the OneWayTPA plugin.
 * <p>
 * Lifecycle:
 * <ul>
 *   <li>{@code onEnable()} — loads settings, builds the service graph, exposes the public API
 *       via {@link OneWayTpaProvider}, registers the command executors and the
 *       runtime alias proxies from {@code config.yml}.</li>
 *   <li>{@link #reload()} — re-reads {@code config.yml} and {@code lang.yml}, rebuilds the
 *       service graph, re-registers commands and aliases. Note: this clears in-flight teleports
 *       and pending requests (see {@code TpaService.shutdown()}). Document for operators.</li>
 *   <li>{@code onDisable()} — unregisters aliases and shuts the service graph down.</li>
 * </ul>
 */
public final class OneWayTPA extends JavaPlugin {

    private MessageService messages;
    private TpaService tpaService;
    private AliasRegistrar aliasRegistrar;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.messages = new MessageService(this);
        PluginSettings settings = PluginSettings.fromConfig(getConfig());
        PermissionResolver permissions = new PermissionResolver(settings);
        this.tpaService = new TpaService(this, messages, settings, permissions);
        this.aliasRegistrar = new AliasRegistrar(this);

        registerApi();
        registerCommands();
        aliasRegistrar.register(settings.commandAliases());

        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDamageListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(), this);
    }

    @Override
    public void onDisable() {
        OneWayTpaProvider.unregister();
        if (aliasRegistrar != null) {
            aliasRegistrar.unregisterAll();
        }
        if (tpaService != null) {
            tpaService.shutdown();
        }
    }

    public void reload() {
        OneWayTpaProvider.unregister();
        if (aliasRegistrar != null) {
            aliasRegistrar.unregisterAll();
        }
        reloadConfig();

        this.messages = new MessageService(this);
        PluginSettings settings = PluginSettings.fromConfig(getConfig());
        PermissionResolver permissions = new PermissionResolver(settings);

        if (tpaService != null) {
            tpaService.shutdown();
        }
        this.tpaService = new TpaService(this, messages, settings, permissions);

        registerApi();
        registerCommands();
        aliasRegistrar.register(settings.commandAliases());
    }

    public TpaService getTpaService() {
        return tpaService;
    }

    public MessageService getMessages() {
        return messages;
    }

    private void registerApi() {
        OneWayTpaApiImpl api = new OneWayTpaApiImpl(
                tpaService,
                tpaService.cooldownService(),
                tpaService.preferenceService(),
                tpaService.requestService(),
                tpaService.pluginSettings()
        );
        OneWayTpaProvider.register(api);
    }

    private void registerCommands() {
        TpaCommand commandHandler = new TpaCommand(messages, tpaService);
        registerCommand("tpa", commandHandler);
        registerCommand("tpahere", commandHandler);
        registerCommand("tpaccept", commandHandler);
        registerCommand("tpadeny", commandHandler);
        registerCommand("tpauto", commandHandler);
        registerCommand("tpaignore", commandHandler);
        registerCommand("tpacancel", commandHandler);

        ReloadCommand reloadHandler = new ReloadCommand(this);
        PluginCommand reloadCmd = getCommand("tpareload");
        if (reloadCmd != null) {
            reloadCmd.setExecutor(reloadHandler);
        } else {
            getLogger().severe("Command missing in plugin.yml: tpareload");
        }

        OneWayTpaAdminCommand adminHandler = new OneWayTpaAdminCommand(this, messages, tpaService);
        PluginCommand adminCmd = getCommand("onewaytpa");
        if (adminCmd != null) {
            adminCmd.setExecutor(adminHandler);
            adminCmd.setTabCompleter(adminHandler);
        } else {
            getLogger().severe("Command missing in plugin.yml: onewaytpa");
        }

        TpaProfileCommand profileHandler = new TpaProfileCommand(messages, tpaService);
        PluginCommand profileCmd = getCommand("tpaprofile");
        if (profileCmd != null) {
            profileCmd.setExecutor(profileHandler);
            profileCmd.setTabCompleter(profileHandler);
        } else {
            getLogger().severe("Command missing in plugin.yml: tpaprofile");
        }
    }

    private void registerCommand(String commandName, TpaCommand handler) {
        PluginCommand command = getCommand(commandName);
        if (command == null) {
            getLogger().severe("Command missing in plugin.yml: " + commandName);
            return;
        }
        command.setExecutor(handler);
        command.setTabCompleter(handler);
    }
}
