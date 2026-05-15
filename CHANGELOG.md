# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [2.3.0] - 2026-05-15

### Added

- `accept-most-recent-without-args` config flag (default `true`). When true, `/tpaccept` and `/tpadeny` without a player argument target the most recent pending request received by the player. When false, the player argument is required.
- Clickable `[CANCEL]` button appended to the `teleport-delay` warmup notification. Runs `/tpacancel` for whoever clicks. Rendered only when `tpacancel.cancel-warmup` is true so a disabled command never produces a non-functional button.
- `click-cancel` and `click-cancel-hover` lang keys for the new button's label and tooltip.
- `no-pending-requests` lang key shown when a player runs `/tpaccept` or `/tpadeny` with no argument and has nothing pending.
- `RequestService.mostRecentSenderName(UUID)` and `TpaService.mostRecentPendingSenderName(Player)` for resolving the most recent pending sender via the existing `TeleportRequest.id` ordering.

### Changed

- `TpaCommand.handleAccept` and `TpaCommand.handleDeny` now share a `resolveTargetSender` helper that branches on argument count and the new toggle.
- `teleport-delay` lang key shortened from `Teleporting in {seconds}s. Use {command} to cancel.` to `Teleporting in {seconds}s.`. The `{command}` placeholder is no longer expanded.
- `TeleportService.schedule` now calls a dedicated `notifyTeleportDelay` helper instead of the generic `notifyParticipants`, so it can build an Adventure Component with prefix + text + optional clickable button.

### Migrating from 2.2

- `/tpaccept` and `/tpadeny` keep accepting an explicit `<player>` argument; the no-argument shortcut is purely additive when the flag is on.
- Operators who want the strict 2.2 behaviour should set `accept-most-recent-without-args: false`.
- The `teleport-delay` text changed; servers with a translated `lang.yml` on disk will continue to read their old wording (with the now-unused `{command}` placeholder rendering verbatim) until they delete or update the file.

## [2.2.0] - 2026-05-15

### Added

- `cancel-on-move` config flag (default `true`). When true, the teleport is aborted if the player being teleported walks to a different block during the warmup. Head/camera movement and sub-block jitter are ignored.
- `teleport-cancelled-move` lang key with `{player}` placeholder, sent to both participants when movement aborts a warmup.
- New `PlayerMoveListener` registered at `EventPriority.MONITOR` with `ignoreCancelled = true`. Uses `PlayerMoveEvent.hasChangedBlock()` as a fast pre-filter so most events return immediately.

### Changed

- `PendingTeleport` gained a `startLocation` field that records the teleported player's position when the warmup begins; used by `cancel-on-move`.
- `TeleportService.schedule` signature now requires the start location. All four call sites in `TpaService` pass the teleported player's current location.
- `TeleportService.cancelIfMovedFromStart(Player)` and `TpaService.handlePlayerMoved(Player)` added to glue the listener into the existing pending-teleport map.
- A teleport is also cancelled if the player changes worlds during the warmup (the world check is part of the move comparison).

### Removed

- Portuguese aliases (`tpaaqui`, `tparecusar`, `tpaignorar`, `tpacancelar`) removed from the default `command-aliases` in `config.yml`. Existing installs keep their on-disk values; only fresh installs are affected. The runtime alias system is unchanged — administrators can still add any aliases they like.

### Migrating from 2.1

- `cancel-on-move: true` is the new default. Servers that want the 2.1 behaviour (movement does not affect a warmup) should add `cancel-on-move: false` to their `config.yml`.
- Existing `config.yml` files keep the Portuguese aliases until the operator edits them out; only fresh installs lose them.
- The cooldown is not refunded on a move-cancel (matches the existing `/tpacancel` warmup behaviour).

## [2.1.0] - 2026-05-15

### Added

- `reset-cooldown-on-accept` config flag (default `true`). When true, the sender's cooldown is cleared after their request is accepted manually or via auto-accept. A completed flow no longer penalises the sender with a leftover cooldown.
- `reset-cooldown-on-deny` config flag (default `true`). When true, the sender's cooldown is cleared after their request is denied. A denied sender can immediately try another player.
- `auto-accept-includes-tpahere` config flag (default `false`). When true, both the global `/tpauto` toggle and per-player `/tpauto <player>` also auto-accept incoming `/tpahere` summons. Default off because `/tpahere` is more invasive than `/tpa`.
- Default `command-aliases` now includes `tpaauto: tpauto` so the legacy command name keeps working.

### Changed

- **`/tpauto` and `/tpaauto` merged into a single `/tpauto [player]` command.** With no args it toggles global auto-accept; with a player arg it toggles per-player auto-accept. `/tpaauto Alice` still works via the runtime alias system.
- `RequestService.createVirtualAcceptedTpa(Player, Player)` renamed to `createVirtualAccepted(Player, Player, RequestType)` so auto-accept can produce the correct request type for `/tpahere` summons.
- `TpaService.sendRequest` now branches the auto-accept schedule call by request type, so an auto-accepted `/tpahere` actually teleports the receiver to the sender (as a manual accept would).
- `usage-tpauto` lang key updated to `"Usage: /tpauto [player]"`.
- `tpauto-enabled` / `tpauto-disabled` wording generalised to "Global auto-accept enabled/disabled."
- `tpaauto-enabled` / `tpaauto-disabled` wording dropped the "/tpa"-specific phrasing in favour of "Auto-accept from {player} enabled/disabled."

### Deprecated

- `onewaytpa.tpaauto` permission node — now a backward-compat shim that grants `onewaytpa.tpauto` via permission inheritance. Servers using LuckPerms with literal `onewaytpa.tpaauto` grants continue to work; prefer `onewaytpa.tpauto` for new setups.

### Removed

- `/tpaauto` command from `plugin.yml` (handled by `/tpauto <player>` and the alias system).
- `usage-tpaauto` lang key (covered by `usage-tpauto`).
- `onewaytpa.tpaauto` entry from the `onewaytpa.*` and `onewaytpa.use` permission bundles (redundant — both already grant `onewaytpa.tpauto`).

### Migrating from 2.0

- `/tpaauto Alice` keeps working unchanged via the new `tpaauto: tpauto` runtime alias.
- LuckPerms grants of `onewaytpa.tpaauto` keep working via permission inheritance.
- Server admins who want the 2.0 cooldown semantics back can set `reset-cooldown-on-accept: false` and `reset-cooldown-on-deny: false`.

## [2.0.0] - 2026-05-15

### Added

- `/onewaytpa` console-friendly admin meta-command with `reload`, `version`, `cancel <player>`, and `info <player>` subcommands. Fine-grained permissions: `onewaytpa.admin.reload`, `.version`, `.cancel`, `.info`, all bundled under `onewaytpa.admin`.
- `teleport-to-live-location` config flag. When true, the destination is re-read at teleport-execute time instead of being snapshotted at accept time. Default false (preserves 1.x behavior).
- `tpacancel.retract-pending` config flag (default true). When true, the sender can use `/tpacancel` to retract all their still-pending outgoing requests, Essentials-style.
- `tpacancel.cancel-warmup` config flag (default true). When false, players cannot use `/tpacancel` to interrupt the warmup of an in-flight teleport.
- `command-aliases` section in `config.yml`. Admins can map any alias to any canonical command without rebuilding the JAR. Defaults preserve the four Portuguese aliases that previously lived in `plugin.yml`.
- `log-events: false` config flag. When true, OneWayTPA logs an admin-facing INFO line for each flow event (request sent, accepted, denied, retracted, expired).
- `TpaRequestRetractEvent` (informational, non-cancellable) — fires when a sender retracts their own pending request.
- `OneWayTpaApi.retractPendingRequests(Player sender): int` — programmatic access to the new retract behavior.
- `PlayerInfoSnapshot` record + `TpaService.getPlayerInfo(Player): PlayerInfoSnapshot` for the `/onewaytpa info` subcommand.
- `LICENSE` (MIT), `CONTRIBUTING.md`, and this `CHANGELOG.md`.

### Changed

- **All user-facing strings translated to English**: `lang.yml` messages, `plugin.yml` descriptions/usages/permissions, and the hard-coded `/tpareload` message in `ReloadCommand.java`.
- `plugin.yml` no longer declares `aliases:` — aliases are now driven by `command-aliases` in `config.yml`.
- `plugin.yml`'s `version:` is now `${version}`, populated by Gradle's `processResources` filter. The build's `build.gradle` `version` field is the single source of truth.
- `PendingTeleport` gained a `destinationOwnerId` field to support live-destination resolution.
- `TeleportService` constructor now takes a `PluginSettings` instead of a bare `delaySeconds`.
- `ReloadCommand` is wired through `MessageService` instead of a hard-coded chat string.
- Magic `* 20L` tick conversions replaced with a named `TICKS_PER_SECOND` constant in `TpaService` and `TeleportService`.
- Class-level javadocs added to all services, listeners, commands, and the public API.
- Gradle wrapper bumped to 9.0 to support Java 25 toolchains.

### Removed

- Hard-coded Portuguese reload message from `ReloadCommand.java` (replaced by the `reload-success` lang key).
- Static `aliases:` blocks from `plugin.yml` (relocated to `command-aliases` in `config.yml`).

### Migrating from 1.x

- Existing `config.yml` and `lang.yml` files are not auto-rewritten. Back them up and let the plugin regenerate the defaults to pick up the English versions and the new config keys.
- All new config keys have defaults that preserve 1.x behavior — upgrading without touching config is safe.
- External plugins that depend on `TpaRequestDenyEvent` for "request torn down" detection should also subscribe to the new `TpaRequestRetractEvent` to catch sender-side retracts.

## [1.0.0] - initial release

- Initial `/tpa` and `/tpahere` workflow with cooldowns, expiration, warmup, click-to-accept prompts, auto-accept, ignore lists, public API, and Folia support.
