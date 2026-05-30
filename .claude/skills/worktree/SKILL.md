---
name: worktree
description: Set up a fresh git worktree from latest main for isolated implementation work. Use before build or PR workflows to prevent local agent clashing.
---

# Worktree

Use this skill first when starting implementation work.

## Workflow

1. `git fetch origin main`.
2. Pick a short kebab-case branch name, for example `add-profile-validation`.
3. Create worktree: `git worktree add -b <branch-name> ./.claude/worktrees/loot-lock-<branch-name> origin/main`.
4. Perform all coding, validation, commit, and PR steps from inside `./.claude/worktrees/loot-lock-<branch-name>`.

## Conventions

- Always branch from latest `main`.
- Use a fresh worktree per branch, do not reuse active worktrees.
- Worktree path format: `./.claude/worktrees/loot-lock-<branch-name>`.
- Keep branch names concise and kebab-case.
