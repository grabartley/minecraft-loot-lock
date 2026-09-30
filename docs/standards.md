# Engineering Standards

These standards apply to every mod in this family: Dogs Unleashed, Loot Lock, More Doors, Not Enough Arrows, Teleport Effects, and Too Many Chests. They exist because the same decisions kept being re-made from memory.

Dogs Unleashed is the reference implementation, and Too Many Chests is the reference for build, CI, and tooling. When this document and those codebases disagree, the codebases are probably right and this document needs updating. The sections below state the family rule first, then how Loot Lock applies it where that is not obvious.

## Multiplayer First

Every mod is designed for a dedicated server with many players, and single player is treated as the special case where the server happens to have one client. This is the opposite of the usual assumption and it changes design constantly.

Practical consequences:

- **The server owns all state that matters.** Anything a modified client could lie about must be decided server-side. In Loot Lock, every pickup decision is made on the server, and the client only ever sends intent. See [ADR 0001](adr/0001-pickup-filtering-runs-on-the-server.md).
- **Clients receive state, they do not compute it.** A client-side value exists for rendering and prediction. It never drives an enforcement decision. Loot Lock's client holds a copy of the player's profiles that the server overwrites with an authoritative sync after every mutation, accepted or not.
- **Two players are always the test case.** Ask what happens when two players do the same thing to the same object in the same tick, and write a test for it. Loot Lock's gametests check that one player's rules never affect another player standing over the same drops.
- **Assume different players see different things.** Two players over the same drop can get different outcomes. Broadcast accordingly, and never assume the acting player is the only one who needs telling. A server policy change is synced to every connected player, not just the operator who made it.
- **Side separation is structural, not incidental.** Client-only code lives in the client source set. A dedicated server must start without loading a single client class, and the repository's side-safety verification enforces this.

## Configuration Through An OP-Gated CLI

**Every option a mod supports must be configurable from the command line by a server operator.** Not most options. Every option.

A server owner without a client mod, working over SSH on a headless box, must be able to configure the mod completely. Graphical configuration screens are a convenience layered on top, never the only way in.

Family rules:

- Server configuration is persisted as JSON in the world save directory, so configuration is per world rather than global.
- It loads from the `LevelStorage.Session` before the server is constructed, because some values are needed earlier than server start.
- A missing file, or one that is not valid JSON, falls back to defaults rather than preventing the server starting.
- Mutating commands require **OP permission level 2**.
- Configuration changes sync to connected clients, so client-side UI reflects live server state.
- Every setting reachable from a configuration screen is also reachable from the command tree, and new settings share their validation between the two rather than reimplementing it.

How Loot Lock applies them:

- Server policy lives in `<world>/lootlock/server-policy.json` and currently holds one field, `allowDeleteRejectedItems`, which defaults to `true`.
- A `LevelStorage` mixin opens a `WorldSession` as soon as a level storage session is created, before the server exists. The session loads the server policy and creates the per-player store and the pickup guard for that world, and writes nothing to disk until the first player data save. `SERVER_STARTED` opens the session only if no session is open for that world yet, and `SERVER_STOPPED` closes it.
- A missing policy file, a file that is not valid JSON, or a file where `allowDeleteRejectedItems` is not `true` or `false` yields defaults. The file is only written when an operator changes the policy.
- Per-player data lives beside it in `<world>/lootlock/players/<uuid>.json`. A player file that cannot be read is moved aside as `<uuid>.broken.<timestamp>.json` rather than overwritten, and the player starts from defaults. See [ADR 0002](adr/0002-per-player-json-store.md).
- `/lootlock policy` and `/lootlock player <target> ...` require permission level 2, and so does the policy packet the in-game panel sends. Players manage their own profiles without operator permission.
- Everything that affects filtering has a `/lootlock` command: the on and off switch, profiles, the active profile, mode, rejected-item action, rules, share codes, and the server policy. A player on a vanilla client can therefore be fully managed by an operator. Profile rename and profile colour are panel-only, since neither changes what gets picked up.

## Code Structure

- **Single Responsibility Principle.** One class, one concern. Extract a collaborator rather than growing a class sideways.
- **No class exceeds 700 lines.** A class approaching the limit is split along responsibility seams, into small extracted helpers.
- **Unit tests map one to one onto classes.** A test exercising `PickupGuard` is named `PickupGuardTest` and lives in the matching package. A test named after a scenario rather than a class is a test nobody can find.
- **Logic worth testing has no Minecraft dependency.** Rule matching, share code decoding, mutation validation, and save debouncing are plain logic over plain data, so they are unit testable without a running game. Where this is possible it is not optional.

## Build And Source Layout

Loom splits the mod into environment source sets:

| Source set | Holds | Loaded on |
|---|---|---|
| `src/main` | Server-safe code: data model, config, networking, commands, the mixins, the lang files, and the gametests | Both sides |
| `src/client` | Screens, HUD, keybinds, client networking senders, and client mixins, all under `com.grahambartley.lootlock.client` | Client only |
| `src/test` | Unit tests for `src/main` | Test runs |
| `src/clientTest` | Unit tests for `src/client` | Test runs |

`src/main` must never reference `net.minecraft.client.*` or `com.grahambartley.lootlock.client.*`. The `verifyMainSourceSideSafety` task fails the build if it does, and the CI smoke boot proves it on a real dedicated server.

Everything targets Java 21, pinned in `.java-version`. The mod version in `gradle.properties` is a local fallback. CI passes the real version with `-Pversion`.

## Testing

- Any new behavioural code ships with unit tests in the same pull request. Documentation-only and configuration-only changes are exempt.
- `./gradlew check` runs formatting, the side-safety check, the no-comments Checkstyle gate, unit tests, client tests, and the JaCoCo coverage report.
- Gametests cover behaviour that only exists in a running world: pickups, networking, persistence, and anything involving more than one player. They live in `src/main/java/com/grahambartley/lootlock/gametest`, are named after the class they exercise with a `GameTest` suffix (`ItemEntityMixinGameTest`), and are registered under the `fabric-gametest` entrypoint in `fabric.mod.json`. Structure templates live in `src/main/resources/data/loot-lock/gametest/structure`.
- `./gradlew runGametest` runs them on a headless server in `build/gametest` and writes `build/gametest-results.xml`.
- Anything that moves or destroys items gets a test asserting where every item ended up: in the inventory, on the ground, or deliberately deleted. Absence of an exception is not evidence of correctness.

## Continuous Integration And Release

Every pull request and every push to `main` runs `.github/workflows/cicd.yml`:

1. Validate the Gradle wrapper and set up JDK 21.
2. `./gradlew spotlessCheck`, then `./gradlew check`.
3. `./gradlew runGametest`. A failing gametest fails the build.
4. `.github/scripts/server-smoke-boot.sh`, which accepts the EULA, boots a real dedicated server, waits for `Done (`, sends `stop`, and fails unless the server exits cleanly and logs Loot Lock's initialization line.
5. Publish the unit and client test report and the gametest report, and upload the coverage report.
6. On failure, upload `build/gametest/`, `run/logs/`, and `run/crash-reports/`.
7. Build a snapshot jar versioned `1.0.0-SNAPSHOT-<sha>` and upload it as an artifact.

A release is a manual `workflow_dispatch` run with a bump (patch, minor, or major) and a release type (alpha, beta, or stable). It runs the same checks, then:

1. Finds the most recently published GitHub release with `gh release list`, validates its tag as semver, and bumps it.
2. Builds the release jar, generates release notes from GitHub, tags `v<version>`, and creates the GitHub release. Alpha and beta releases are marked as prereleases.
3. Publishes the jar to Modrinth with the matching version type.
4. Creates or reuses a `v<version>` milestone, applies it to every issue closed by a pull request merged since the previous release, and closes it. This step never fails the release.

## Compatibility

- **Work with vanilla wherever possible.** A feature that only works on the mod's own content is worth much less than one that works on what players already have. Loot Lock filters any item or item tag by id, modded ones included.
- **Vanilla clients are first-class.** Filtering runs entirely on the server, so a player without the mod is still filtered, and an operator can manage their profiles through `/lootlock player <target>`.
- **Optional integrations degrade cleanly.** Mod Menu, JEI, and REI are suggested dependencies. The mod loads and runs correctly without them, and no class referencing their types loads when they are absent. Each integration is a single class under `client/compat` that the other mod loads through its own entrypoint, and shared logic lives in `RecipeViewerBridge`, which references none of their types.
- **Do not assume the player's video settings.** Feedback that only makes sense with particles or a specific GUI scale is invisible to some players.

## Translations

Every player-visible string goes through a translation key in `LootLockLang`. The lang files live in `src/main/resources/assets/loot-lock/lang/` so that text the server sends resolves too.

- `en_us.json` is the source of truth. `LootLockLangTest` fails the build if a key constant has no English value, or an English value has no key constant.
- A new key lands in every lang file in the same pull request, currently `en_us.json` and `pt_br.json`. Nothing tests this yet, so review checks it.

## Documentation

- Every change updates the documentation it invalidates, in the same pull request.
- Reasoning behind an architectural decision belongs in an architecture decision record under [`docs/adr/`](adr/README.md), not in a comment and not in a commit message.
- **The source carries no comments at all.** Not explanatory ones, not javadoc, not "why" ones. Naming and structure carry the meaning, and anything that genuinely needs explaining is either a decision record or a sign the code should be reshaped until it does not. Checkstyle enforces this rather than memory: a single `MatchXpath` rule in `config/checkstyle/checkstyle.xml` fails `./gradlew check` at `file:line` on any comment in any source set, javadoc included, and never edits source to do it.
