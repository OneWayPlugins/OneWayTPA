# OneWayTPA

OneWayTPA is a Paper plugin that provides a one-way teleport request system with cooldowns, request expiration, delayed teleport execution, cancellation, auto-accept, ignore lists, and a public API for integrations.

## Features

- `/tpa` and `/tpahere` request flow
- Clickable accept/deny messages
- Per-group request cooldowns by permission
- Request expiration timeout
- Delayed teleport with `/tpacancel`
- Auto-accept and ignore preferences (global or per-player)
- Reload command for config/messages
- Public API and custom events for external plugins
- Folia-compatible scheduling (`GlobalRegionScheduler`)

## Requirements

- Java 21
- Paper `1.21.x` (project configured for `paper-api:1.21.11-R0.1-SNAPSHOT`)

## Build

```bash
./gradlew build
```

On Windows:

```powershell
.\gradlew.bat build
```

## Installation

1. Build the plugin jar with Gradle.
2. Copy the generated jar from `build/libs/` to your server `plugins/` folder.
3. Start or restart the server.

## Commands

- `/tpa <player>`: Request to teleport to another player.
- `/tpahere <player>`: Request to bring another player to you.
- `/tpaccept <player>`: Accept a pending request from a player.
- `/tpadeny <player>`: Deny a pending request from a player.
- `/tpauto`: Toggle global auto-accept for `/tpa` requests.
- `/tpaauto <player>`: Toggle auto-accept for `/tpa` from a specific player.
- `/tpaignore [player]`: Toggle global ignore or ignore a specific player.
- `/tpacancel`: Cancel a pending delayed teleport.
- `/tpareload`: Reload plugin configuration and language messages.

## Permissions

- `onewaytpa.use`: Basic user access bundle.
- `onewaytpa.*`: Full plugin access (default: op).
- `onewaytpa.tpa`
- `onewaytpa.tpahere`
- `onewaytpa.tpaccept`
- `onewaytpa.tpadeny`
- `onewaytpa.tpauto`
- `onewaytpa.tpaauto`
- `onewaytpa.tpaignore`
- `onewaytpa.tpacancel`
- `onewaytpa.reload` (default: op)

Cooldown group example permissions:

- `onewaytpa.cooldown.vip`
- `onewaytpa.cooldown.membro`

## Configuration

`config.yml`:

- `request-expire-seconds`: Pending request lifetime.
- `teleport-delay-seconds`: Delay before teleport executes.
- `cooldown-groups`: Ordered cooldown groups with permission + seconds.

`lang.yml`:

- Message keys used by command and service flows.
- Supports placeholders like `{player}`, `{seconds}`, `{command}`.
- Supports `&` color codes.

## API Usage

Get the API:

```java
import net.onewaycraft.oneWayTPA.api.OneWayTpaApi;
import net.onewaycraft.oneWayTPA.api.OneWayTpaProvider;

OneWayTpaApi api = OneWayTpaProvider.get();
```

Example send request:

```java
api.sendRequest(sender, receiver, RequestType.TPA);
```

### Exposed Events

- `TpaRequestEvent` (cancellable)
- `TpaRequestAcceptEvent` (cancellable)
- `TpaRequestDenyEvent`
- `TpaRequestExpireEvent`
- `TpaTeleportEvent` (cancellable)
- `TpaTeleportCompleteEvent`

## Notes

- Add `depend: [OneWayTPA]` or `softdepend: [OneWayTPA]` in external plugin `plugin.yml` when integrating with the API.
- A leftover local folder `.claude/` is not part of runtime behavior and is currently untracked.
