package net.onewaycraft.oneWayTPA;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Persistent per-player preferences: auto-accept lists (all / per-player),
 * ignore lists (all / per-player), and per-flow UI mode overrides. Backed
 * by {@code preferences.yml} in the plugin data folder, saved synchronously
 * on every mutation.
 */
public final class PreferenceService {

    private final File file;
    private final JavaPlugin plugin;
    private final PluginSettings settings;

    private final Set<UUID> autoAcceptAll = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Set<UUID>> autoAcceptByPlayer = new ConcurrentHashMap<>();

    private final Set<UUID> ignoreAll = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Set<UUID>> ignoreByPlayer = new ConcurrentHashMap<>();

    private final Map<UUID, EnumSet<UiFlow>> guiFlows = new ConcurrentHashMap<>();

    public PreferenceService(JavaPlugin plugin, PluginSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
        this.file = new File(plugin.getDataFolder(), "preferences.yml");
        load();
    }

    public boolean shouldAutoAccept(Player receiver, Player sender) {
        UUID receiverId = receiver.getUniqueId();
        UUID senderId = sender.getUniqueId();
        if (autoAcceptAll.contains(receiverId)) {
            return true;
        }
        Set<UUID> allowed = autoAcceptByPlayer.get(receiverId);
        return allowed != null && allowed.contains(senderId);
    }

    public boolean isIgnoring(Player receiver, Player sender) {
        UUID receiverId = receiver.getUniqueId();
        UUID senderId = sender.getUniqueId();
        if (ignoreAll.contains(receiverId)) {
            return true;
        }
        Set<UUID> ignored = ignoreByPlayer.get(receiverId);
        return ignored != null && ignored.contains(senderId);
    }

    public boolean toggleAutoAcceptAll(Player player) {
        UUID id = player.getUniqueId();
        if (autoAcceptAll.contains(id)) {
            autoAcceptAll.remove(id);
            save();
            return false;
        }
        autoAcceptAll.add(id);
        save();
        return true;
    }

    public boolean toggleAutoAcceptPlayer(Player player, Player target) {
        UUID playerId = player.getUniqueId();
        UUID targetId = target.getUniqueId();

        Set<UUID> set = autoAcceptByPlayer.computeIfAbsent(playerId, ignored -> ConcurrentHashMap.newKeySet());
        if (set.contains(targetId)) {
            set.remove(targetId);
            if (set.isEmpty()) {
                autoAcceptByPlayer.remove(playerId, set);
            }
            save();
            return false;
        }

        set.add(targetId);
        save();
        return true;
    }

    public boolean toggleIgnoreAll(Player player) {
        UUID id = player.getUniqueId();
        if (ignoreAll.contains(id)) {
            ignoreAll.remove(id);
            save();
            return false;
        }
        ignoreAll.add(id);
        save();
        return true;
    }

    public boolean toggleIgnorePlayer(Player player, Player target) {
        UUID playerId = player.getUniqueId();
        UUID targetId = target.getUniqueId();

        Set<UUID> set = ignoreByPlayer.computeIfAbsent(playerId, ignored -> ConcurrentHashMap.newKeySet());
        if (set.contains(targetId)) {
            set.remove(targetId);
            if (set.isEmpty()) {
                ignoreByPlayer.remove(playerId, set);
            }
            save();
            return false;
        }

        set.add(targetId);
        save();
        return true;
    }

    public boolean isAutoAcceptingAll(UUID playerId) {
        return autoAcceptAll.contains(playerId);
    }

    public boolean isAutoAccepting(UUID receiverId, UUID senderId) {
        if (autoAcceptAll.contains(receiverId)) {
            return true;
        }
        Set<UUID> allowed = autoAcceptByPlayer.get(receiverId);
        return allowed != null && allowed.contains(senderId);
    }

    public boolean isIgnoringAll(UUID playerId) {
        return ignoreAll.contains(playerId);
    }

    public boolean isIgnoring(UUID receiverId, UUID senderId) {
        if (ignoreAll.contains(receiverId)) {
            return true;
        }
        Set<UUID> ignored = ignoreByPlayer.get(receiverId);
        return ignored != null && ignored.contains(senderId);
    }

    public boolean isGuiEnabled(UUID playerId, UiFlow flow) {
        EnumSet<UiFlow> overrides = guiFlows.get(playerId);
        if (overrides == null) {
            return settings.uiDefaultFor(flow);
        }
        return overrides.contains(flow);
    }

    public boolean setGui(UUID playerId, UiFlow flow, boolean enabled) {
        guiFlows.compute(playerId, (id, set) -> {
            EnumSet<UiFlow> s = (set == null) ? EnumSet.noneOf(UiFlow.class) : set;
            if (enabled) {
                s.add(flow);
            } else {
                s.remove(flow);
            }
            return s;
        });
        save();
        return enabled;
    }

    public boolean hasGuiOverride(UUID playerId) {
        return guiFlows.containsKey(playerId);
    }

    public void resetGuiPreferences(UUID playerId) {
        if (guiFlows.remove(playerId) != null) {
            save();
        }
    }

    public void clear() {
        autoAcceptAll.clear();
        autoAcceptByPlayer.clear();
        ignoreAll.clear();
        ignoreByPlayer.clear();
        guiFlows.clear();
    }

    private void load() {
        if (!file.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        loadUuidSet(config, "auto-accept-all", autoAcceptAll);
        loadUuidMap(config, "auto-accept-by-player", autoAcceptByPlayer);
        loadUuidSet(config, "ignore-all", ignoreAll);
        loadUuidMap(config, "ignore-by-player", ignoreByPlayer);
        loadGuiFlows(config);
    }

    private void save() {
        YamlConfiguration config = new YamlConfiguration();

        saveUuidSet(config, "auto-accept-all", autoAcceptAll);
        saveUuidMap(config, "auto-accept-by-player", autoAcceptByPlayer);
        saveUuidSet(config, "ignore-all", ignoreAll);
        saveUuidMap(config, "ignore-by-player", ignoreByPlayer);
        saveGuiFlows(config);

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save preferences.yml", e);
        }
    }

    private void loadGuiFlows(YamlConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("gui-flows");
        if (section == null) {
            return;
        }
        for (String playerKey : section.getKeys(false)) {
            UUID playerId;
            try {
                playerId = UUID.fromString(playerKey);
            } catch (IllegalArgumentException e) {
                continue;
            }
            EnumSet<UiFlow> flows = EnumSet.noneOf(UiFlow.class);
            for (String value : section.getStringList(playerKey)) {
                UiFlow flow = UiFlow.fromKey(value);
                if (flow != null) {
                    flows.add(flow);
                }
            }
            guiFlows.put(playerId, flows);
        }
    }

    private void saveGuiFlows(YamlConfiguration config) {
        for (Map.Entry<UUID, EnumSet<UiFlow>> entry : guiFlows.entrySet()) {
            List<String> values = new ArrayList<>();
            for (UiFlow flow : entry.getValue()) {
                values.add(flow.name());
            }
            config.set("gui-flows." + entry.getKey(), values);
        }
    }

    private void loadUuidSet(YamlConfiguration config, String key, Set<UUID> target) {
        List<String> list = config.getStringList(key);
        for (String s : list) {
            try {
                target.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void loadUuidMap(YamlConfiguration config, String key, Map<UUID, Set<UUID>> target) {
        ConfigurationSection section = config.getConfigurationSection(key);
        if (section == null) {
            return;
        }
        for (String playerKey : section.getKeys(false)) {
            UUID playerId;
            try {
                playerId = UUID.fromString(playerKey);
            } catch (IllegalArgumentException e) {
                continue;
            }
            Set<UUID> set = ConcurrentHashMap.newKeySet();
            List<String> targets = section.getStringList(playerKey);
            for (String s : targets) {
                try {
                    set.add(UUID.fromString(s));
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (!set.isEmpty()) {
                target.put(playerId, set);
            }
        }
    }

    private void saveUuidSet(YamlConfiguration config, String key, Set<UUID> source) {
        config.set(key, source.stream().map(UUID::toString).toList());
    }

    private void saveUuidMap(YamlConfiguration config, String key, Map<UUID, Set<UUID>> source) {
        for (Map.Entry<UUID, Set<UUID>> entry : source.entrySet()) {
            String path = key + "." + entry.getKey().toString();
            config.set(path, entry.getValue().stream().map(UUID::toString).toList());
        }
    }
}
