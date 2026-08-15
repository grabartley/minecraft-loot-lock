---
name: automated-qa
description: Programmatically drive the game client with a temporary in-process QA driver, capture framebuffer screenshots of the feature under test, verify them, and attach the evidence to the PR. Use for any change with a visible or interactive surface BEFORE handing off to manual QA.
---

# Automated QA

Drive the real game client programmatically, capture framebuffer screenshots of the feature under
test, verify them, and attach the evidence to the PR. Use this after implementing any change with a
visible or interactive surface (screens, the inventory panel, toasts, HUD notices, rendering,
in-world interactions) BEFORE handing off to manual QA. Manual QA then confirms feel and edge cases
instead of discovering basics.

Why in-process instead of OS automation: macOS input automation (System Events, cliclick) needs
accessibility permissions the agent shell usually lacks, and OS-level clicks are brittle against
window focus and Retina scaling. A temporary in-process driver has full deterministic control over
the client, needs no OS permissions, and captures pixel-exact framebuffer screenshots.

## The Temp Driver Pattern

All driver code is TEMPORARY and must never be committed. It exists only in the worktree during QA.

1. Create `src/client/java/com/grahambartley/lootlock/client/<Feature>QaDriver.java` from
`templates/QaDriver.java` in this skill directory. It is a tick-driven state machine registered
on `ClientTickEvents.END_CLIENT_TICK`.
2. Register it with one line at the end of `LootLockClient.onInitializeClient()`:
`<Feature>QaDriver.register();`
3. After QA passes, revert both:
`git checkout -- src/client/java/com/grahambartley/lootlock/client/LootLockClient.java`
`rm src/client/java/com/grahambartley/lootlock/client/<Feature>QaDriver.java`

## Capabilities Toolbox

- **World loading**: from the title screen call
`client.createIntegratedServerLoader().start("dev", () -> {})` once
`client.currentScreen instanceof TitleScreen` and ~60 ticks have passed (resources settled).
Do NOT bother with loom `programArgs "--quickPlaySingleplayer", ...` — it is not picked up.
The dev `run/saves/dev` world exists in every worktree because the `worktree` skill copies `run/`.
- **Server-side setup**: `client.getServer().execute(() -> ...)` reaches the integrated server.
Reach the player with `server.getPlayerManager().getPlayerList().get(0)`, seed state through
`LootLock.PLAYER_DATA_MANAGER` (profiles, rules, active profile, revision), then push it with the
same `ServerToClientPackets.sendAuthoritativeSync(player)` gameplay uses. Drive notices through
the real send path too. This tests the real network round trip, not a mock, and the client-side
`ClientLootLockState` ends up in exactly the state a live server would produce.
- **Opening the surfaces under test**: the inventory panel attaches to vanilla `InventoryScreen`
via the client mixins and `ScreenEvents.AFTER_INIT`, so `client.setScreen(new
InventoryScreen(client.player))` gets the real panel, not a stub. Standalone screens
(`LootLockScreen`, `LootLockClientPrefsScreen`, `ProfileImportScreen`) can be set directly.
Give the screen a tick or two to `init()` before clicking or screenshotting.
- **Clicks and keys**: call `client.currentScreen.mouseClicked(sx, sy, 0)` / `keyPressed(...)`
directly with SCALED screen coordinates. No cursor movement needed; this drives the same code
path as a real click and real C2S packets flow. Tab switches, profile pills, rule rows, and the
search field are all reachable this way.
- **Hover states**: hover rendering reads the real OS cursor, so move it with
`GLFW.glfwSetCursorPos` plus the iterative settle loop in the template. Never trust a single
set call: GLFW cursor space vs framebuffer size differs per display (Retina), so converge with
the multiplicative feedback loop until `client.mouse` derives to the target scaled position.
When the window is unfocused and the OS refuses the cursor move, fall back to the forced-render
wrapper in the template: subclass the screen under test and override `render(...)` with fixed
mouse coordinates. Identical render path, no OS involvement.
- **Screenshots**: `ScreenshotRecorder.saveScreenshot(client.runDirectory, name + ".png",
client.getFramebuffer(), text -> {})` writes to `run/screenshots/`. Log the derived mouse
position alongside each shot so a failed hover is diagnosable from the log.
- **GUI scale sweeps**: `client.setScreen(null); client.options.getGuiScale().setValue(n);
client.onResolutionChanged();` then re-trigger the screen. Capture at least scales 1, 2, and 4
for anything with custom rendering. The panel docks beside the vanilla inventory, so scale
sweeps are the fastest way to catch clipping and off-screen layout. Expect the docked panel to
collapse at high scales where `LootLockInventoryPanel.canDock` fails, which is real behaviour,
not a driver fault: the entry button routes to the standalone screen instead.
- **Beware stale state reads**: layout-dependent getters (`fitsOnScreen`, `getCurrentHeight`,
panel geometry) are only meaningful after the first render frame following `setScreen`, because
the mixin lays the panel out during `drawBackground`. Logging them on the tick you open the
screen reports defaults, and a driver that asserts on them there prints a green log over an
empty frame. Assert on the tick you screenshot, never earlier.
- **Lifecycle**: print `[QA]`-prefixed markers for every step, a final `[QA] DONE`, catch every
exception into `[QA] ERROR`, and end with `client.scheduleStop()` so the run terminates itself.

## Environment Prep (once per worktree)

- `run/options.txt`: set `pauseOnLostFocus:false` (MANDATORY — the client runs unfocused in the
background and singleplayer would otherwise sit on the pause screen, which also blocks any
screen-open path that checks `currentScreen == null`). Pin `guiScale` to a known value so the
first screenshots are deterministic.
- These are `run/` files, untracked; no cleanup needed beyond not committing them.

## Run Protocol

1. **Absolute paths only.** Every command runs with an explicit
`cd /path/to/.claude/worktrees/loot-lock-<branch> && ...`. The classic failure mode is the
shell cwd silently resetting to the main checkout, which launches the OLD mod without the
driver and "does nothing". If a run produces zero `[QA]` log lines, check cwd first.
2. Compile and PROVE the driver is in the build before launching:
`./gradlew compileClientJava` then confirm
`ls build/classes/java/client/com/grahambartley/lootlock/client/<Feature>QaDriver.class` exists
and
`javap -c -classpath build/classes/java/client com.grahambartley.lootlock.client.LootLockClient | grep -c QaDriver`
returns non-zero. Do not launch until both check out.
3. Launch in the background with output to a log file:
`./gradlew runClient > /tmp/qa-run.log 2>&1` (background).
4. Wait on the sentinels, never on time:
`until grep -qE '\[QA\] (DONE|ERROR)|BUILD FAILED' /tmp/qa-run.log; do sleep 3; done`
5. Read the `[QA]` log lines, then Read every captured PNG and actually LOOK at it: panel
alignment, text legibility, state highlights, scale behaviour, truncation. A green log with
wrong pixels is a failed QA.

## Publishing Evidence to the PR

GitHub's `user-attachments` uploads are not available via `gh`, so the evidence lives on the
dedicated orphan `images` branch, stored by PR number. NEVER commit screenshots to the PR branch
or main; binary evidence must not enter main's history.

NEVER force-push or delete the `images` branch: every PR description across the repo hot-links its
evidence from this branch by raw URL, so rewriting its history breaks images on every past PR at
once. Always add on top with normal commits; to replace a PR's evidence, overwrite the files in its
`pr-<number>/` directory in a new commit, which only ever affects that one PR.

1. Downscale the keeper screenshots: `sips -Z 1000 run/screenshots/qa_*.png --out <staging-dir>/`
2. Check out the `images` branch in a temporary worktree
(`git worktree add <tmp-path> images`; if the branch does not exist yet, create it orphan with
`git worktree add --orphan -b images <tmp-path>`), copy the screenshots into `pr-<number>/`,
commit, push `origin images`, then `git worktree remove` the temp path. Use `--no-verify` if a
pre-commit hook is installed locally, since it would try to run `./gradlew`, which does not
exist on the codeless orphan branch.
3. Embed them in the PR body via raw URLs:
`https://raw.githubusercontent.com/grabartley/minecraft-loot-lock/images/pr-<number>/<name>.png`
placed next to the paragraph each illustrates, then `gh pr edit <num> --body-file ...`.
4. If a later run replaces the screenshots, overwrite the same file names in `pr-<number>/` and
push again; the raw URLs track the images branch head, so the body only needs editing when the
prose changes.

## Checklist Before Handoff to Manual QA

- [ ] Driver ran to `[QA] DONE` with zero `[QA] ERROR`
- [ ] Every screenshot visually verified by reading the PNG, at multiple GUI scales for rendering
- [ ] Server-side state assertions logged and correct (e.g. profile/rule values after a click)
- [ ] Temp driver + initializer hook reverted; `git status` shows only intended files
- [ ] Evidence pushed to the `images` branch under `pr-<number>/` and embedded in the PR body;
	nothing image-related committed to the PR branch

## Related Skills

- `build` — requires this skill before manual QA handoff
- `run-game-client` — plain manual launch, used when a human is driving
- `run-tests` — unit coverage, which this skill complements rather than replaces
- `worktree` — provides the isolated `run/` directory this skill relies on
