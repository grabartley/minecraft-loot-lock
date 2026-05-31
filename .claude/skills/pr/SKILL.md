---
name: pr
description: Create a PR for the minecraft-loot-lock repo using a git worktree branched from latest main. Use for final branch, commit, push, and PR flow.
---

# PR

Use this skill for branch strategy, final validation, commit, push, and PR creation.
This skill is used directly by humans and also as a handoff step from the build skill.

## Workflow

1. Stage: `git add <files>`
2. Format: `./gradlew spotlessApply`
3. Test: `./gradlew test`
4. Build: `./gradlew clean build`
5. Commit: `git commit -m "<type>: <short lowercase descriptive present tense message>"`
- Types: feat, fix, refactor, test, docs, chore
6. Push: `git push origin <branch-name>`
7. Create PR with a body file to preserve markdown formatting and avoid shell interpolation issues:
	- Write body markdown to a temp file (example: `.claude/tmp/pr-body.md`) and include real newlines.
	- Create PR: `gh pr create --repo grabartley/minecraft-loot-lock --base main --head <branch-name> --title "<title>" --body-file .claude/tmp/pr-body.md`
	- If updating an existing PR body, use: `gh pr edit <pr-number> --repo grabartley/minecraft-loot-lock --body-file .claude/tmp/pr-body.md`
	- Title: `<type>: <description>` (same style as commit message)
	- Body: one-liner summary, then "**What's included:**" with bullet points
	- Wrap class names, commands, and identifiers in backticks inside the markdown file, not inline shell args
	- Always include a closing reference like `Closes #<issue-number>` so the PR Development section is linked to the issue being worked on
8. After merge, clean up: `cd ../loot-lock && git worktree remove ./.claude/worktrees/loot-lock-<branch-name>`

## Conventions

- Always branch from latest `main`, never from other branches
- Use a fresh worktree per PR, don't reuse worktrees across branches
- Worktree path: `./.claude/worktrees/loot-lock-<branch-name>` (sibling directory)
- Branch names: kebab-case (e.g. `fix-profile-save`, `add-command-permission-check`)
- Commit messages: `<type>: <lowercase description>`, no period at end
- Types: feat, fix, refactor, test, docs, chore
- PR descriptions: bullet points under "What's included:" header
- Always link the PR Development section to the active issue via `Closes #<issue-number>` in the PR body
- Pre-commit must complete successfully: format, test, build
- Run `./gradlew spotlessApply` before staging so CI `spotlessCheck` stays green
- No emoji in commit messages or PR titles

## Related Skills

- worktree, used first for fresh branch and isolated directory setup

## Build Skill Integration

- If invoked after build work, confirm the linked issue is in `QA testing` before final handoff.
- Do not move issue to `Done`, that is reserved for human QA completion.
