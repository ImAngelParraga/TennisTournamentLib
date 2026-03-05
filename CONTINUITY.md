# CONTINUITY

Last Updated: 2026-03-05
Repository: TennisTournamentLib

## Update Rule
Update this file after each meaningful implementation/review/change in this repo.
Include: branch, uncommitted state, what changed, and compatibility impact on backend.

## Current State
- Branch: `master`
- Local implementation changes (not committed yet):
  - modified: `Phase.kt`, `KnockoutService.kt`, `TournamentService.kt`, `KnockoutServiceTest.kt`
  - untracked: `SESSION_HANDOFF.md`

## Recent Completed Work
- Unreleased local change: add `PARTIAL_SEEDED` mode with `seededPlayerCount` and validation.
- Unreleased local change: add configurable knockout seeding strategy with deterministic default.
- Unreleased local change: fix stale `-1` bye assertion and extend seeding tests.
- `dd3a556` Add third-place playoff bracket support
- `ef95218` Add qualifiers and scoring helpers
- `a62eccf` Fix knockout loser selection and bye winners
- `ee4f656` Use null instead of -1 for byes
- `2b63e72` Fix bug where a first round match has 2 byes players. Add test.

## Current Functional Baseline
- Knockout bracket generation is implemented.
- Knockout seeding supports:
  - deterministic input-order seeding (default),
  - optional randomized seeding,
  - partial seeding (top N fixed, remaining players randomized).
- Qualifier-aware round computation exists (`computeRounds`).
- Third-place playoff generation is supported (with constraints).
- Match score application exists (`Match.applyScore`).

## Known Gaps
- `GroupService` remains TODO.
- `SwissService` remains TODO.
- Additional scoring/domain validation depth is still limited.

## Integration Notes (Backend)
- Backend consumes this repo through composite build from `../TennisTournamentLib`.
- Compatibility-sensitive areas:
  - match dependency semantics
  - score winner resolution
  - qualifiers/third-place behavior

## Test Commands
- Library tests:
  - `GRADLE_OPTS=-Dkotlin.compiler.execution.strategy=in-process ./gradlew clean test --no-daemon`
- If Kotlin cache lock appears:
  - run `./gradlew --stop` before retrying.

## Next Suggested Actions
1. Decide product direction for Group/Swiss (implement or remove from public contract for now).
2. Expand test coverage for edge-case scoring and progression invariants.
3. Wire backend API/DTO to expose `KnockoutConfig.seedingStrategy` and `seededPlayerCount`.
