# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
