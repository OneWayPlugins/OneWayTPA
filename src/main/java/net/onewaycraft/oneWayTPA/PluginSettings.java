package net.onewaycraft.oneWayTPA;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public final class PluginSettings {

    private final long requestExpireSeconds;
    private final long teleportDelaySeconds;
    private final List<CooldownGroup> cooldownGroups;

    public PluginSettings(long requestExpireSeconds, long teleportDelaySeconds, List<CooldownGroup> cooldownGroups) {
        this.requestExpireSeconds = Math.max(0L, requestExpireSeconds);
        this.teleportDelaySeconds = Math.max(0L, teleportDelaySeconds);
        this.cooldownGroups = List.copyOf(cooldownGroups);
    }

    public static PluginSettings fromConfig(FileConfiguration config) {
        long requestExpire = Math.max(0L, config.getLong("request-expire-seconds", 60L));
        long teleportDelay = Math.max(0L, config.getLong("teleport-delay-seconds", 3L));
        List<CooldownGroup> groups = loadCooldownGroups(config.getConfigurationSection("cooldown-groups"));
        return new PluginSettings(requestExpire, teleportDelay, groups);
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
}
