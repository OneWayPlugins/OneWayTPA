package net.onewaycraft.oneWayTPA;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.function.LongBinaryOperator;

/**
 * Resolves per-player overrides for behaviors that have a server-wide default in
 * {@link PluginSettings}.
 * <p>
 * Two override styles are supported:
 * <ul>
 *   <li><b>Boolean bypass</b> (e.g. {@code onewaytpa.bypass.move}): the holder
 *       skips a config-enforced restriction.</li>
 *   <li><b>Scalar override</b> (e.g. {@code onewaytpa.warmup.<seconds>}): the
 *       suffix is parsed as a non-negative integer and aggregated across all
 *       matching nodes — {@code min} for "lower-is-better" metrics (warmup),
 *       {@code max} for "higher-is-better" metrics (invulnerability).</li>
 * </ul>
 * All overrides are gated by the {@code permission-bypasses-enabled} config flag;
 * when false, this class always returns the config default.
 */
public final class PermissionResolver {

    private static final String WARMUP_PREFIX = "onewaytpa.warmup.";
    private static final String INVULN_PREFIX = "onewaytpa.invulnerability.";
    private static final String MOVE_BYPASS = "onewaytpa.bypass.move";

    private final PluginSettings settings;

    public PermissionResolver(PluginSettings settings) {
        this.settings = settings;
    }

    public boolean canBypassMoveCancel(Player player) {
        return settings.permissionBypassesEnabled() && player.hasPermission(MOVE_BYPASS);
    }

    public long resolveWarmupSeconds(Player player) {
        long fallback = settings.teleportDelaySeconds();
        if (!settings.permissionBypassesEnabled()) {
            return fallback;
        }
        Long min = scanIntegerSuffix(player, WARMUP_PREFIX, Math::min);
        return min != null ? min : fallback;
    }

    public long resolveInvulnerabilitySeconds(Player player) {
        long fallback = settings.postTeleportInvulnerabilitySeconds();
        if (!settings.permissionBypassesEnabled()) {
            return fallback;
        }
        Long max = scanIntegerSuffix(player, INVULN_PREFIX, Math::max);
        return max != null ? max : fallback;
    }

    private Long scanIntegerSuffix(Player player, String prefix, LongBinaryOperator picker) {
        Long acc = null;
        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            if (!info.getValue()) {
                continue;
            }
            String node = info.getPermission();
            if (!node.startsWith(prefix)) {
                continue;
            }
            try {
                long value = Long.parseLong(node.substring(prefix.length()));
                if (value < 0L) {
                    continue;
                }
                acc = (acc == null) ? value : picker.applyAsLong(acc, value);
            } catch (NumberFormatException ignored) {
            }
        }
        return acc;
    }
}
