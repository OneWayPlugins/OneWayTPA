package net.onewaycraft.oneWayTPA;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Immutable, validated view of {@code config.yml} for the rest of the plugin to consume.
 * Built once per (re)load by {@link #fromConfig(FileConfiguration)}.
 */
public final class PluginSettings {

    private final long requestExpireSeconds;
    private final long teleportDelaySeconds;
    private final List<CooldownGroup> cooldownGroups;
    private final Map<String, String> commandAliases;

    public PluginSettings(long requestExpireSeconds,
                          long teleportDelaySeconds,
                          List<CooldownGroup> cooldownGroups,
                          Map<String, String> commandAliases) {
        this.requestExpireSeconds = Math.max(0L, requestExpireSeconds);
        this.teleportDelaySeconds = Math.max(0L, teleportDelaySeconds);
        this.cooldownGroups = List.copyOf(cooldownGroups);
        this.commandAliases = Map.copyOf(commandAliases);
    }

    public static PluginSettings fromConfig(FileConfiguration config) {
        long requestExpire = Math.max(0L, config.getLong("request-expire-seconds", 60L));
        long teleportDelay = Math.max(0L, config.getLong("teleport-delay-seconds", 5L));
        List<CooldownGroup> groups = loadCooldownGroups(config.getConfigurationSection("cooldown-groups"));
        Map<String, String> aliases = loadCommandAliases(config.getConfigurationSection("command-aliases"));
        return new PluginSettings(requestExpire, teleportDelay, groups, aliases);
    }

    public long requestExpireSeconds() {
        return requestExpireSeconds;
    }

    public long teleportDelaySeconds() {
        return teleportDelaySeconds;
    }

    public List<CooldownGroup> cooldownGroups() {
        return cooldownGroups;
    }

    public Map<String, String> commandAliases() {
        return commandAliases;
    }

    private static List<CooldownGroup> loadCooldownGroups(ConfigurationSection section) {
        if (section == null) {
            return List.of(new CooldownGroup("default", "", 30L));
        }

        List<CooldownGroup> groups = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            String path = key + ".";
            String permission = section.getString(path + "permission", "");
            long seconds = Math.max(0L, section.getLong(path + "seconds", 0L));
            groups.add(new CooldownGroup(key, permission, seconds));
        }

        if (groups.isEmpty()) {
            groups.add(new CooldownGroup("default", "", 30L));
        }
        return groups;
    }

    private static Map<String, String> loadCommandAliases(ConfigurationSection section) {
        if (section == null) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (String alias : section.getKeys(false)) {
            String canonical = section.getString(alias);
            if (canonical == null || canonical.isBlank()) {
                continue;
            }
            String normalizedAlias = alias.toLowerCase(Locale.ROOT);
            String normalizedCanonical = canonical.toLowerCase(Locale.ROOT);
            result.put(normalizedAlias, normalizedCanonical);
        }
        return result;
    }
}
