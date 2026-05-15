package net.onewaycraft.oneWayTPA;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player rate limiter for outgoing /tpa and /tpahere requests.
 * <p>
 * Each player resolves to the duration of the first {@link CooldownGroup} whose
 * permission they hold. A group with an empty permission string is the fallback
 * applied when no other group matches. Group order is therefore significant —
 * see {@code config.yml} for details.
 * <p>
 * State is held in memory and not persisted across restarts.
 */
public final class CooldownService {

    private final List<CooldownGroup> groups;
    private final Map<UUID, Long> lastRequestAt = new ConcurrentHashMap<>();

    public CooldownService(List<CooldownGroup> groups) {
        this.groups = groups;
    }

    public long remainingSeconds(Player sender) {
        long cooldown = cooldownFor(sender);
        if (cooldown <= 0L) {
            return 0L;
        }

        Long last = lastRequestAt.get(sender.getUniqueId());
        if (last == null) {
            return 0L;
        }

        long remainingMs = (cooldown * 1000L) - (System.currentTimeMillis() - last);
        if (remainingMs <= 0L) {
            return 0L;
        }
        return (remainingMs + 999L) / 1000L;
    }

    public void markRequest(Player sender) {
        lastRequestAt.put(sender.getUniqueId(), System.currentTimeMillis());
    }

    public void removePlayer(UUID playerId) {
        lastRequestAt.remove(playerId);
    }

    public void clear() {
        lastRequestAt.clear();
    }

    private long cooldownFor(Player player) {
        long fallback = 0L;
        for (CooldownGroup group : groups) {
            String permission = group.permission();
            if (permission == null || permission.isBlank()) {
                fallback = Math.max(0L, group.seconds());
                continue;
            }
            if (player.hasPermission(permission)) {
                return Math.max(0L, group.seconds());
            }
        }
        return fallback;
    }
}
