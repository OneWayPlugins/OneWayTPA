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
    private final boolean teleportToLiveLocation;
    private final boolean retractPendingEnabled;
    private final boolean cancelWarmupEnabled;
    private final boolean resetCooldownOnAccept;
    private final boolean resetCooldownOnDeny;
    private final boolean logEvents;
    private final List<CooldownGroup> cooldownGroups;
    private final Map<String, String> commandAliases;

    public PluginSettings(long requestExpireSeconds,
                          long teleportDelaySeconds,
                          boolean teleportToLiveLocation,
                          boolean retractPendingEnabled,
                          boolean cancelWarmupEnabled,
                          boolean resetCooldownOnAccept,
                          boolean resetCooldownOnDeny,
                          boolean logEvents,
                          List<CooldownGroup> cooldownGroups,
                          Map<String, String> commandAliases) {
        this.requestExpireSeconds = Math.max(0L, requestExpireSeconds);
        this.teleportDelaySeconds = Math.max(0L, teleportDelaySeconds);
        this.teleportToLiveLocation = teleportToLiveLocation;
        this.retractPendingEnabled = retractPendingEnabled;
        this.cancelWarmupEnabled = cancelWarmupEnabled;
        this.resetCooldownOnAccept = resetCooldownOnAccept;
        this.resetCooldownOnDeny = resetCooldownOnDeny;
        this.logEvents = logEvents;
        this.cooldownGroups = List.copyOf(cooldownGroups);
        this.commandAliases = Map.copyOf(commandAliases);
    }

    public static PluginSettings fromConfig(FileConfiguration config) {
        long requestExpire = Math.max(0L, config.getLong("request-expire-seconds", 60L));
        long teleportDelay = Math.max(0L, config.getLong("teleport-delay-seconds", 5L));
        boolean liveLocation = config.getBoolean("teleport-to-live-location", false);
        boolean retractPending = config.getBoolean("tpacancel.retract-pending", true);
        boolean cancelWarmup = config.getBoolean("tpacancel.cancel-warmup", true);
        boolean resetOnAccept = config.getBoolean("reset-cooldown-on-accept", true);
        boolean resetOnDeny = config.getBoolean("reset-cooldown-on-deny", true);
        boolean logEvents = config.getBoolean("log-events", false);
        List<CooldownGroup> groups = loadCooldownGroups(config.getConfigurationSection("cooldown-groups"));
        Map<String, String> aliases = loadCommandAliases(config.getConfigurationSection("command-aliases"));
        return new PluginSettings(requestExpire, teleportDelay, liveLocation,
                retractPending, cancelWarmup, resetOnAccept, resetOnDeny, logEvents, groups, aliases);
    }

    public long requestExpireSeconds() {
        return requestExpireSeconds;
    }

    public long teleportDelaySeconds() {
        return teleportDelaySeconds;
    }

    public boolean teleportToLiveLocation() {
        return teleportToLiveLocation;
    }

    public boolean retractPendingEnabled() {
        return retractPendingEnabled;
    }

    public boolean cancelWarmupEnabled() {
        return cancelWarmupEnabled;
    }

    public boolean resetCooldownOnAccept() {
        return resetCooldownOnAccept;
    }

    public boolean resetCooldownOnDeny() {
        return resetCooldownOnDeny;
    }

    public boolean logEvents() {
        return logEvents;
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
