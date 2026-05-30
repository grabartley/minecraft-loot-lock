# Side-Safety Guardrails

LootLock supports dedicated server mode, so `src/main` must stay server-safe.

## Rules

- `src/main/java` must not reference `net.minecraft.client.*`.
- `src/main/java` must not reference `com.grahambartley.client.*`.
- Client UI, keybinds, and rendering logic belong in `src/client/java`.

## Validation Commands

- Run side-safety checks: `./gradlew verifyMainSourceSideSafety`
- Run full local verification: `./gradlew check`
- Run dedicated server boot smoke check: `timeout 120s ./gradlew runServer --no-daemon --stacktrace --args="nogui" || test $? -eq 124`

## CI Coverage

GitHub Actions runs all of the following:

- `./gradlew check` (includes `verifyMainSourceSideSafety`)
- Dedicated server smoke boot command

If the server process is still healthy at timeout, the smoke check exits with `124` and is treated as a pass.
