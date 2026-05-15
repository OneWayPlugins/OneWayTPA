# Contributing to OneWayTPA

Thanks for considering a contribution! This document covers the development workflow, code style, and submission checklist.

## Development environment

- **JDK 21+** (Temurin recommended). The build is configured for Java 21 source/target but newer JDKs work as long as Gradle understands them.
- **Gradle 9.x** — handled by the wrapper (`./gradlew`). No global install needed.
- A clone of this repository:

  ```bash
  git clone https://github.com/lucas-santoro/OneWayTPA.git
  cd OneWayTPA
  ```

## Build & run

- Build the JAR: `./gradlew build` (output in `build/libs/`).
- Run a local Paper server with the plugin loaded: `./gradlew runServer`. This downloads a matching Paper JAR, places the plugin into its `plugins/` folder, and starts it. Use `stop` to exit.
- Run on Folia: replace the dev server JAR with a Folia build and copy the plugin manually — the plugin is Folia-compatible (`GlobalRegionScheduler` + `teleportAsync`).

## Code style

- **Java 21**, records and `var` allowed where they clarify intent.
- **One responsibility per service.** New behavior should slot into an existing service in `net.onewaycraft.oneWayTPA` or come with its own focused class.
- **No new fields without javadoc.** Every public class has a class-level javadoc; every public method on the API has its own. Internal helpers can be terse but should still be readable.
- **No magic numbers.** Tick conversion uses the `TICKS_PER_SECOND` constant in `TpaService` / `TeleportService`. Add similar constants when introducing new unit conversions.
- **No mutable global state.** Service instances are constructed in `OneWayTPA#onEnable` and re-built on reload; the only static state is the API service-locator (`OneWayTpaProvider`).

## Public API stability

`OneWayTpaApi` is the contract external plugins write against. **Adding methods is fine; removing or changing existing signatures is a breaking change** and requires a major version bump.

When you add a new behavior:

1. Add the method to `OneWayTpaApi` with javadoc explaining cancellation semantics, side effects, and which lang keys are notified.
2. Implement in `OneWayTpaApiImpl`, delegating to the relevant service.
3. If the behavior fires an event, prefer adding a **new event class** in `api/event/` over overloading the meaning of an existing event.

## Commit style

This project follows [Conventional Commits](https://www.conventionalcommits.org/) — the existing history uses prefixes like `feat:`, `fix:`, `docs:`, `chore:`, `build:`. Keep the title under 70 characters; put detail in the body.

## Submitting a pull request

1. Branch from `master` (or whatever the active development branch is).
2. Make your changes. Verify locally: `./gradlew build`, then run a quick end-to-end test via `./gradlew runServer`.
3. Update `CHANGELOG.md` under the `[Unreleased]` section.
4. If your change touches lang keys, update `lang.yml` and document any new placeholders in the message comment.
5. Open the PR with a description of *why* the change is needed and what it does. Reference any related issue.

## Reporting issues

Open an issue at https://github.com/lucas-santoro/OneWayTPA/issues with:

- Server type (Paper / Folia) and version
- OneWayTPA version (visible via `/onewaytpa version`)
- Steps to reproduce
- Relevant log lines (set `log-events: true` in `config.yml` to capture flow events)

## Code of conduct

Be respectful. Discuss design decisions, not people.
