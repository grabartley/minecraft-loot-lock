---
name: create-issue
description: Create GitHub issues for minecraft-loot-lock and link them to the project board. Use when asked to create, draft, scope, or file an issue in this repo.
---

# Create Issue

Use this skill when the user wants a new GitHub issue created for `grabartley/minecraft-loot-lock`.

## Defaults

1. Assign every new issue to `grabartley` unless the user says otherwise.
2. Add every new issue to project `grabartley` project `2`, `Loot Lock`.
3. Set the project status to `Ready` unless the user says otherwise.
4. Keep issue titles brief and concise.
5. Write issue bodies so the work can be implemented from the issue alone.
6. Treat every issue as public. Do not expose local file paths, private notes, or machine-specific details.

## Public Repo Rules

- Never mention local paths like `~/Downloads/...`, `/Users/...`, or other workstation-specific locations.
- Never mention that a file existed only on a local machine, in a temp folder, or in private notes.
- If design artifacts or reference docs exist outside the repo, refer to them by stable artifact name only, and summarize the needed details directly in the issue body.
- Do not rely on "see local mock" or "see attached file on disk" as the only implementation guidance.
- If an issue references behavior in code, include the relevant repo path and summarize the behavior in plain language.

## Research First

Before writing the issue body, decide whether the request is already well scoped.

Use the request directly when it already includes:
- Clear goal
- Exact scope
- Acceptance criteria
- Enough implementation detail to work from

Research the codebase first when any of these are missing:
- The concrete class, screen, command, packet, or subsystem is unclear
- The request mentions a bug but not the root area of code
- The issue needs file paths, current behavior, or technical constraints
- The issue should call out risks, dependencies, or out-of-scope boundaries

Research tools to use:
- `glob` to find likely files
- `grep` to find symbols, strings, or call sites
- `read` to inspect the exact implementation and behavior
- `gh issue list --state all --limit 200 --json number,title` to avoid duplicate issues when needed

Do the research before authoring the issue so the final description is self-contained.

## Title Guidance

- Keep titles short.
- Match the repo's existing prefix style when appropriate.
- Prefer formats like:
- `[Build] <short feature or implementation target>`
- `[Bug] <short bug summary>`
- `[Docs] <short docs task>`
- `[Research] <short investigation target>`
- `[Tracking] <short umbrella or planning topic>`
- Do not cram acceptance criteria or implementation notes into the title.

## Body Standards

Every issue body should be public-safe and detailed enough that someone can implement the work from the issue alone.

Include the sections that fit the task:
- `## Goal` or `## Context`
- `## Files` when repo locations are known and useful
- `## Current Behavior` when describing a bug or refactor target
- `## Target Behavior` or `## Scope`
- `## Specific Changes` or `## Tasks`
- `## Acceptance Criteria`
- `## Out Of Scope`

Body rules:
- Use repo-relative paths only, never local absolute paths.
- Summarize any external mock or design details directly in the issue.
- State defaults, ordering, labels, and behavioral constraints explicitly.
- Include enough acceptance criteria to verify the work.
- If implementation should preserve existing behavior in some area, say that explicitly.
- If the request is about code changes, include the likely file path or tell the implementer how to find it.

## Creation Workflow

1. Capture the request.
2. Research the codebase if needed so the issue can stand on its own.
3. Draft a concise title.
4. Draft the body in markdown.
5. Write the body to a temp file to preserve formatting and avoid shell quoting problems.
6. Create the issue with `gh issue create`.
7. Add the issue to project `2`.
8. Set project status to `Ready` unless the user asked for another status.
9. Return the issue URL and the status that was applied.

## GitHub Commands

Create the issue:

```bash
gh issue create \
--repo grabartley/minecraft-loot-lock \
--title "<brief title>" \
--body-file .claude/tmp/issue-body.md \
--assignee grabartley
```

Add it to the project:

```bash
gh project item-add 2 --owner grabartley --url <issue-url>
```

Fetch project field metadata when needed:

```bash
gh project field-list 2 --owner grabartley --format json
```

Current project metadata for fast use:
- Project id: `PVT_kwHOAQYbq84BZQEC`
- Status field id: `PVTSSF_lAHOAQYbq84BZQECzhUQDzY`
- `Ready` option id: `61e4505c`
- `In progress` option id: `47fc9ee4`
- `QA testing` option id: `df73e18b`
- `Done` option id: `98236657`

Look up the added project item for the issue, then set the status:

```bash
gh api graphql -f query='query($owner:String!, $repo:String!, $number:Int!) { repository(owner:$owner, name:$repo) { issue(number:$number) { projectItems(first:20) { nodes { id project { id } } } } } }' -f owner=grabartley -f repo=minecraft-loot-lock -F number=<issue-number>

gh project item-edit \
--id <project-item-id> \
--project-id PVT_kwHOAQYbq84BZQEC \
--field-id PVTSSF_lAHOAQYbq84BZQECzhUQDzY \
--single-select-option-id 61e4505c
```

If the user asked for a status other than `Ready`, use the matching option id instead.

## Final Checks

Before creating the issue, confirm:
- The title is concise.
- The description is public-safe.
- The body contains enough detail to implement the work without local context.
- Repo paths are relative and accurate.
- External references are summarized in the body.

After creating the issue, confirm:
- The issue is assigned to `grabartley` unless told otherwise.
- The issue was added to project `Loot Lock`.
- The project status is `Ready` unless told otherwise.
- The returned URL opens the created issue.

## Related Skills

- `build`, use this before implementation when build work does not already have a GitHub issue
- `pr`, use after implementation is complete and a linked issue already exists
