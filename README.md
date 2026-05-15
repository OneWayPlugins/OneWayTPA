# OneWayTPA

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Version](https://img.shields.io/badge/version-2.4.0-blue.svg)](CHANGELOG.md)
[![Paper](https://img.shields.io/badge/Paper-1.21.x-orange.svg)](https://papermc.io)
[![Folia](https://img.shields.io/badge/Folia-supported-green.svg)](https://papermc.io/software/folia)
[![Java](https://img.shields.io/badge/Java-21-red.svg)](https://adoptium.net)

A Paper/Folia plugin that provides a one-way TPA request system with cooldowns, request expiration, a configurable warmup window, retract-and-cancel semantics, auto-accept and ignore preferences, runtime command aliases, and a public API for downstream integrations.

## Highlights

- **`/tpa` and `/tpahere`** request flow with clickable accept/deny prompts.
- **Per-permission cooldown groups** with priority-ordered resolution.
- **Two independent `/tpacancel` modes** (`retract-pending` and `cancel-warmup`) so you can tune the UX for your server.
- **Snapshot or live destination** — pick whether the teleport lands where the destination player stood at accept time, or where they are when the warmup ends.
- **Auto-accept** (global or per-player) and **ignore** (global or per-player).
- **Console-friendly `/onewaytpa` admin meta-command** with `reload`, `version`, `cancel`, and `info` subcommands.
- **Runtime command aliases** loaded from `config.yml` — admins can map any alias to any canonical command without rebuilding.
- **Public API** with six custom events for downstream plugin integrations.
- **Folia-compatible**: uses `GlobalRegionScheduler` and `Player#teleportAsync` throughout.

## Requirements

- Java 21
- Paper or Folia, `1.21.x` (built against `paper-api:1.21.11-R0.1-SNAPSHOT`)
- Gradle 9.x to build from source (the wrapper handles this for you)

## Install

1. Build (`./gradlew build`) or download a release JAR from your distribution channel.
2. Copy the JAR from `build/libs/OneWayTPA-<version>.jar` into your server's `plugins/` folder.
3. Start the server. `config.yml`, `lang.yml`, and `preferences.yml` (created on demand) live under `plugins/OneWayTPA/`.

### Build from source

```bash
./gradlew build
```

Windows:

```powershell
.\gradlew.bat build
```

The resulting JAR is at `build/libs/OneWayTPA-2.4.0.jar`.

## Commands

| Command | Description | Permission | Player | Console |
|---|---|---|---|---|
| `/tpa <player>` | Request to teleport to a player | `onewaytpa.tpa` | ✓ | — |
| `/tpahere <player>` | Request to bring a player to you | `onewaytpa.tpahere` | ✓ | — |
| `/tpaccept [player]` | Accept a pending request. Without a player arg, accepts the most recent pending (toggle in config) | `onewaytpa.tpaccept` | ✓ | — |
| `/tpadeny [player]` | Deny a pending request. Without a player arg, denies the most recent pending (toggle in config) | `onewaytpa.tpadeny` | ✓ | — |
| `/tpauto [player]` | No args: toggle global auto-accept. With a player arg: toggle per-player auto-accept | `onewaytpa.tpauto` | ✓ | — |
| `/tpaignore [player]` | Toggle global ignore or per-player ignore | `onewaytpa.tpaignore` | ✓ | — |
| `/tpacancel` | Retract own pending request, or cancel an in-flight warmup (see config) | `onewaytpa.tpacancel` | ✓ | — |
| `/tpareload` | Reload `config.yml` and `lang.yml` | `onewaytpa.reload` | ✓ | ✓ |
| `/onewaytpa reload` | Same as `/tpareload`, but available on console | `onewaytpa.admin.reload` | ✓ | ✓ |
| `/onewaytpa version` | Print the plugin version | `onewaytpa.admin.version` | ✓ | ✓ |
| `/onewaytpa cancel <player>` | Admin override: cancel a player's in-flight teleport regardless of config | `onewaytpa.admin.cancel` | ✓ | ✓ |
| `/onewaytpa info <player>` | Snapshot of a player's TPA state (cooldown, prefs, pending counts) | `onewaytpa.admin.info` | ✓ | ✓ |

## Permissions

| Node | Description | Default |
|---|---|---|
| `onewaytpa.use` | Bundle of all per-command player permissions | `true` |
| `onewaytpa.*` | All permissions, including admin and reload | `op` |
| `onewaytpa.tpa`, `.tpahere`, `.tpaccept`, `.tpadeny`, `.tpauto`, `.tpaignore`, `.tpacancel` | Individual command permissions | `true` |
| `onewaytpa.tpaauto` | Legacy node from 1.x / 2.0 — grants `onewaytpa.tpauto` for backward compatibility | `false` |
| `onewaytpa.reload` | Allows `/tpareload` | `op` |
| `onewaytpa.admin` | Bundle of all `/onewaytpa` admin subcommands | `op` |
| `onewaytpa.admin.reload`, `.version`, `.cancel`, `.info` | Per-subcommand admin permissions | `op` |
| `onewaytpa.cooldown.<group>` | Membership in a cooldown group; configured by you in `config.yml` | (server policy) |

## Configuration

### Annotated `config.yml`

```yaml
# How long a pending /tpa or /tpahere request stays valid before expiring.
# Set to 0 to disable expiration.
request-expire-seconds: 60

# Delay between /tpaccept and the actual teleport.
# During this window, /tpacancel can interrupt the teleport (see tpacancel.cancel-warmup below).
# Set to 0 to teleport immediately on accept.
teleport-delay-seconds: 5

# When false (default), the destination is captured at /tpaccept time and is
# unchanged even if the destination player moves during the warmup.
# When true, the destination is re-read at the moment of teleport execution.
# If the destination player is offline at execute time, the teleport fails.
teleport-to-live-location: false

# /tpacancel behaviour. Both flags are independent. When both are off, /tpacancel
# is fully disabled and shows the tpacancel-disabled message.
tpacancel:
  # When true, the sender can /tpacancel to retract all their own still-pending,
  # not-yet-accepted outgoing requests (Essentials-style sender retract).
  retract-pending: true

  # When true, any participant can /tpacancel during the warmup window to cancel
  # the in-flight teleport (original 1.x behaviour).
  cancel-warmup: true

# When true, the sender's cooldown is cleared after their request is accepted
# (manually or via auto-accept). Default true.
reset-cooldown-on-accept: true

# When true, the sender's cooldown is cleared after their request is denied.
# Default true.
reset-cooldown-on-deny: true

# When true, auto-accept (both /tpauto and /tpauto <player>) also applies to
# /tpahere requests, not just /tpa. Default false because /tpahere summons are
# more invasive.
auto-accept-includes-tpahere: false

# When true, the teleport is cancelled if the player being teleported moves to
# a different block during the warmup. Default true (Essentials-style).
cancel-on-move: true

# When true, /tpaccept and /tpadeny without a player argument target the most
# recent pending request. When false, the player argument is required.
# Default true.
accept-most-recent-without-args: true

# Seconds of post-teleport invulnerability. All incoming damage is cancelled
# for the teleported player during this window. 0 disables. Default 0.
post-teleport-invulnerability-seconds: 0

# When true, OneWayTPA logs admin-facing INFO lines for request flow events.
log-events: false

# Cooldown groups for /tpa and /tpahere senders. Order matters: the first group
# whose permission the player has wins. A group with an empty permission acts
# as the fallback for everyone else.
cooldown-groups:
  vip:
    permission: "onewaytpa.cooldown.vip"
    seconds: 10
  member:
    permission: "onewaytpa.cooldown.member"
    seconds: 20
  default:
    permission: ""
    seconds: 30

# Optional command aliases. Each entry maps alias -> canonical command name.
# The default only carries the /tpaauto compatibility shim from the 2.1 merge.
# Add your own entries to provide additional shortcuts (e.g. mytpa: tpa).
command-aliases:
  tpaauto: tpauto
```

### Cooldown groups walkthrough

When a player runs `/tpa`, OneWayTPA walks `cooldown-groups` top-to-bottom:

1. For each group with a non-empty `permission`, check `player.hasPermission(...)`. The first match wins, and its `seconds` value becomes the cooldown.
2. If no permission-gated group matches, the first group with an empty `permission` is used as the fallback.
3. If no groups match at all, no cooldown is applied.

So the example above gives VIPs (10s), members (20s), and everyone else (30s).

### Two-flag `/tpacancel`

| `retract-pending` | `cancel-warmup` | `/tpacancel` behavior |
|---|---|---|
| `true` | `true` | First tries to retract the sender's own pending outgoing requests; if none, cancels the warmup of any teleport the player is involved in. |
| `true` | `false` | Only retracts the sender's pending outgoing requests. Warmups cannot be interrupted by players. |
| `false` | `true` | Only cancels the in-flight warmup. Senders cannot retract their own pending requests. |
| `false` | `false` | The command is disabled and returns the `tpacancel-disabled` message. |

`/onewaytpa cancel <player>` bypasses both flags — admins can always force-cancel.

### Snapshot vs. live destination

The default (`teleport-to-live-location: false`) freezes the destination at the moment the receiver accepts. Even if they walk away during the warmup, the teleporter lands where they stood when accepting. This is the safest behavior for most servers.

Flip to `true` to re-read the destination player's location at execute-time. Useful for combat or escape scenarios where the receiver should genuinely be "where they are now." If the destination player is offline when the warmup ends, the teleport fails with `teleport-failed-destination-offline`.

### Cooldown resets

`reset-cooldown-on-accept` and `reset-cooldown-on-deny` (both default `true`) clear the sender's cooldown timestamp once their request reaches a terminal state. With the defaults, a player who gets accepted or denied can immediately send another `/tpa`; with them off, the configured cooldown still has to elapse.

The reset also applies when a request is auto-accepted via `/tpauto`.

### Auto-accept and `/tpahere`

`/tpauto` and `/tpauto <player>` only affect incoming `/tpa` requests by default. Set `auto-accept-includes-tpahere: true` to extend auto-accept to `/tpahere` summons as well — useful for trusted-buddy setups where you want the other player to be able to pull you to them without a prompt. Off by default because `/tpahere` is more invasive than `/tpa`.

### Cancel on movement

By default (`cancel-on-move: true`), the teleport is aborted if the player being teleported walks to a different block during the warmup. Head/camera movement and sub-block jitter are ignored, so standing still on a slime block or looking around is fine. The cooldown is **not** refunded on a move-cancel — matches the existing `/tpacancel` warmup behaviour. Set the flag to `false` if you want players to be able to wander during the warmup (the 2.1 behaviour).

### Accept / deny without arguments

`/tpaccept` and `/tpadeny` accept an optional `<player>` argument. With the default `accept-most-recent-without-args: true`, calling either command without an argument targets the most recent pending request (the highest-id pending in the receiver's queue). With the flag off, the argument is mandatory and a missing argument prints the usage message.

### Clickable cancel button on the warmup notification

When a teleport's warmup begins, both the requester and the receiver receive the `teleport-delay` message with an appended clickable `[CANCEL]` button that runs `/tpacancel` for the clicker. The button is only rendered when `tpacancel.cancel-warmup` is true, so a disabled command never leaves a non-functional button behind.

### Post-teleport invulnerability

Set `post-teleport-invulnerability-seconds` to any positive integer to grant the teleported player a brief damage-immunity window after the teleport completes. Useful for preventing ambush kills right after a `/tpa` and for letting players load the new area without taking damage. The window covers **all** damage sources — PvP, mobs, fall, lava, void — and is reset to its full duration if the player teleports again before the previous window expires. Defaults to `0` (disabled) to preserve the existing behaviour.

## Public API

External plugins can integrate via `OneWayTpaApi`. Add a `depend: [OneWayTPA]` or `softdepend: [OneWayTPA]` entry to your own `plugin.yml`.

```java
import net.onewaycraft.oneWayTPA.api.OneWayTpaApi;
import net.onewaycraft.oneWayTPA.api.OneWayTpaProvider;
import net.onewaycraft.oneWayTPA.RequestType;

OneWayTpaApi api = OneWayTpaProvider.get();
api.sendRequest(sender, receiver, RequestType.TPA);
int retracted = api.retractPendingRequests(sender);
```

### Custom events

| Event | Cancellable | When it fires |
|---|---|---|
| `TpaRequestEvent` | yes | Just before a new pending request is created |
| `TpaRequestAcceptEvent` | yes | Just before an accepted request schedules a teleport |
| `TpaRequestDenyEvent` | no | After the receiver denies a request |
| `TpaRequestRetractEvent` | no | After the sender retracts a still-pending request |
| `TpaRequestExpireEvent` | no | After a pending request expires |
| `TpaTeleportEvent` | yes | Just before the warmup teleport is executed |
| `TpaTeleportCompleteEvent` | no | After a successful teleport (may fire async) |

## Migrating from 2.3

`2.4.0` is purely additive on top of `2.3.0`:

- **`post-teleport-invulnerability-seconds: 0` is the new default.** Behaviour is unchanged unless you set the value to a positive integer to enable the feature.
- No lang-key or permission changes.

## Migrating from 2.2

`2.3.0` is additive on top of `2.2.0`:

- **`/tpaccept` and `/tpadeny` now accept an optional `<player>` argument.** With the default `accept-most-recent-without-args: true`, calling either with no argument targets the most recent pending request. Set the flag to `false` to restore the 2.2 strictness.
- **The `teleport-delay` warmup notification now ships with a clickable `[CANCEL]` button** when `tpacancel.cancel-warmup` is true. The lang key text shortened — the `Use /tpacancel to cancel.` trailing phrase is gone (the button replaces it). Existing servers with a translated `lang.yml` on disk keep their old wording until they regenerate the file.

## Migrating from 2.1

`2.2.0` is additive on top of `2.1.0` with one default behaviour change:

- **`cancel-on-move: true` is the new default.** Teleports now abort if the player being teleported walks during the warmup. Set the flag to `false` to keep the 2.1 behaviour (movement does nothing).
- **Portuguese aliases (`tpaaqui`, `tparecusar`, `tpaignorar`, `tpacancelar`) are gone from the default `command-aliases`.** Existing servers keep them in their on-disk `config.yml`; only fresh installs are affected. Add them back manually if you need them.
- **`tpaauto: tpauto` is still the only default alias** — the 2.1 `/tpauto` merge compatibility shim stays.

## Migrating from 2.0

`2.1.0` is additive on top of `2.0.0` with one mild breaking change:

- **`/tpaauto` is gone as a standalone command.** Its functionality moved into `/tpauto <player>`. The name still works via the runtime alias system (`tpaauto: tpauto` is in the default `command-aliases`).
- **`onewaytpa.tpaauto` permission node is deprecated** but still recognised — it now grants `onewaytpa.tpauto` via permission inheritance. Update your permissions setup at your convenience.
- **New defaults are player-friendly.** `reset-cooldown-on-accept` and `reset-cooldown-on-deny` default `true`. If you prefer 2.0 semantics, set them to `false`.
- **`auto-accept-includes-tpahere` defaults `false`** — same effective behaviour as 2.0 (auto-accept only affects `/tpa`). Flip to `true` to extend.

## Migrating from 1.x

The `2.0.0` release was mostly additive but contained the following user-visible changes (all still apply):

- **All chat messages, command descriptions, and `config.yml` comments are now in English.** If you keep your old Portuguese `lang.yml`/`config.yml`, OneWayTPA will continue to use them as-is (new keys fall back to a clearly-marked `[missing lang key: ...]` token). To pick up the new English defaults, back up your old files and let the plugin regenerate them on next start.
- **Portuguese aliases moved from `plugin.yml` into `config.yml`** as `command-aliases`. The defaults still include `tpaaqui`, `tparecusar`, `tpaignorar`, `tpacancelar`, so existing players don't lose anything. Edit, remove, or add aliases without rebuilding.
- **`/tpacancel` now does more than before.** With default settings it first tries to retract a sender's pending outgoing requests (new behavior); if there's nothing to retract, it falls back to cancelling the warmup as in 1.x. Set `tpacancel.retract-pending: false` to restore exact 1.x behavior.
- **Existing configs keep working** — every new key has a 1.x-compatible default. The migration is opt-in.

## Troubleshooting

- **Aliases aren't registering.** If another plugin already registers the same alias name, OneWayTPA logs a warning and skips it. The `onewaytpa:alias` namespaced form is always available as a fallback (e.g. `/onewaytpa:tpaaqui`).
- **Reload wiped my players' pending requests.** Yes — reload tears the service graph down and rebuilds it. Avoid `/tpareload` during peak hours, or warn players first.
- **Live mode failed with `teleport-failed-destination-offline`.** Expected behavior: the destination player logged out during the warmup, and there's no destination to teleport to. The teleporter stays where they were.
- **Folia.** OneWayTPA uses `GlobalRegionScheduler` and `teleportAsync` throughout, so it works on Folia without modification. Cross-region teleports are handled by Paper's async teleport API.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for the development workflow, code style, and contribution checklist.

## Changelog

See [CHANGELOG.md](CHANGELOG.md).

## License

Released under the [MIT License](LICENSE).
