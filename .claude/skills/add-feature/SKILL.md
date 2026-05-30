---
description: Add a new feature to the Loot Lock Minecraft mod following project conventions. Use when asked to implement, add, or create a new feature.
---

# Add Feature

## Critical Rules

1. **Always create a new branch from up-to-date main**:
```bash
git checkout main
git pull origin main
git checkout -b feat/your-feature-name
```

2. **Always manually test before pushing** - Use the run-game-client skill to verify changes work correctly and wait for developer approval

3. **Always use pr skill** when creating pull requests

## Java 21 Required

Use your preferred version manager before running gradle commands:
- **jenv**: `jenv local 21`
- **SDKMAN**: `sdk use java 21-amzn`
- **Manual**: `export JAVA_HOME=/Library/Java/JavaVirtualMachines/amazon-corretto-21.jdk/Contents/Home`

## Quick Workflow

1. Create branch: `git checkout main && git pull && git checkout -b feat/name`
2. Implement feature
3. Format: `./gradlew spotlessApply`
4. Test: `./gradlew test && ./gradlew runGametestServer`
5. Build: `./gradlew clean build`
6. **Manual test: Run game client and get developer blessing**
7. Commit using **Conventional Commits**: `git commit -m "feat: description"`
8. Push: `git push -u origin feat/name`
9. Create PR using pr skill

## Related Skills

- **pr** - Use this for all pull requests
- **run-game-client** - Use this for manual testing before pushing
