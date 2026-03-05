# AGENTS

## Scope
This file applies to `TennisTournamentLib`.

## Required Workflow
1. Read `AI_CONTEXT.md` and `CONTINUITY.md` before making changes.
2. Do not commit or push unless the user explicitly asks.
3. If a commit is requested, push immediately after each commit.
4. Update `CONTINUITY.md` after each meaningful implementation/review change.

## Lib-Specific Rules
- Run library tests before finishing:
  - `./gradlew.bat test --no-daemon`
- Prefer additive, backward-compatible API changes when possible.
- If changing generation/progression/scoring semantics, add or update tests first.

## Cross-Repo Rules
- Backend integration path is `../TennisTournamentBackend`.
- If lib public contracts change (phase config, seeding, score behavior), validate backend build/tests too.
