package net.onewaycraft.oneWayTPA;

import net.onewaycraft.oneWayTPA.api.OneWayTpaApiImpl;
import net.onewaycraft.oneWayTPA.api.OneWayTpaProvider;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class OneWayTPA extends JavaPlugin {

    private MessageService messages;
    private TpaService tpaService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.messages = new MessageService(this);
        PluginSettings settings = PluginSettings.fromConfig(getConfig());
        this.tpaService = new TpaService(this, messages, settings);

        registerApi();
        registerCommands();

        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
    }

    @Override
    public void onDisable() {
        OneWayTpaProvider.unregister();
        if (tpaService != null) {
            tpaService.shutdown();
        }
    }

    public void reload() {
        OneWayTpaProvider.unregister();
        reloadConfig();

        this.messages = new MessageService(this);
        PluginSettings settings = PluginSettings.fromConfig(getConfig());

        if (tpaService != null) {
            tpaService.shutdown();
        }
        this.tpaService = new TpaService(this, messages, settings);

        registerApi();
        registerCommands();
    }

    public TpaService getTpaService() {
        return tpaService;
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
        registerCommand("tpaauto", commandHandler);
        registerCommand("tpaignore", commandHandler);
        registerCommand("tpacancel", commandHandler);

        ReloadCommand reloadHandler = new ReloadCommand(this);
        PluginCommand reloadCmd = getCommand("tpareload");
        if (reloadCmd != null) {
            reloadCmd.setExecutor(reloadHandler);
        } else {
            getLogger().severe("Command missing in plugin.yml: tpareload");
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
