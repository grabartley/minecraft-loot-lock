---
name: build
description: Build or implement a feature for the LootLock mod, optionally from a GitHub issue. Use when asked to build, implement, or ship scoped work and keep project board status updated.
---

# Build

## Critical Rules

1. Always tie build work to a GitHub issue.
2. Run the `worktree` skill first before any issue moves, coding, or validation.
3. Keep issue project status in sync during execution.
4. Run the pr skill as part of build after validation passes.
5. Move issue to `QA testing` only after PR is opened and CI is running.
6. After PR creation and `QA testing` transition, always provide a detailed manual QA checklist to the developer.
7. Stop at QA testing, human performs final verification and moves to Done.

## Workflow

1. Run the `worktree` skill to create a fresh isolated branch worktree, then perform all implementation and validation work inside that worktree.
2. Capture scope from the request.
3. If an issue number or URL is provided, read it first with gh:
   - `gh issue view <number> --repo grabartley/minecraft-loot-lock`
   - Extract acceptance criteria, constraints, and references.
4. If no issue is provided, create one before coding:
   - Create a scoped issue with context, task list, and acceptance criteria.
   - Add it to the project board.
   - Use this issue as the tracking artifact for all subsequent status moves.
5. Move the issue to `In progress`.
6. Implement the feature.
7. Run relevant automated tests and a local validation pass for changed behavior.
8. Run manual validation via run-game-client when gameplay behavior changes.
9. Invoke the pr skill for branch strategy, final checks, commit, push, and PR creation.
10. Wait for CI to start on the PR and report status.
11. Move issue to `QA testing` when the PR is ready for human verification.
12. Provide a detailed manual QA checklist that the developer can run step by step.

## Board Status Policy

- Use these exact status values from project `grabartley/projects/2`:
  - `Backlog`: issue created, not started
  - `Ready`: scoped and ready to start
  - `In progress`: active implementation
  - `QA testing`: implementation complete, awaiting human validation
  - `Done`: human-only final move after QA signoff

- Required transitions for build flow:
  - Start work: set to `In progress`
  - After PR creation and QA handoff: set to `QA testing`
  - Do not move to `Done` inside this skill

## Issue Creation Template (when issue not provided)

Title format:
- `[Build] <short capability or feature name>`

Body minimum:
- Context: why this change is needed
- Scope: exact implementation boundaries
- Tasks: checklist of concrete coding and validation steps
- Acceptance Criteria: testable outcomes
- Out of Scope: explicit exclusions

After creating the issue, add it to the project board and start tracking status transitions immediately.

## Related Skills

- worktree, required first step for isolated branch setup
- pr, required for branch, commit, push, and PR creation during build flow
- run-game-client, use for manual validation before handoff
