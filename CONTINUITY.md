# CONTINUITY

Last Updated: 2026-03-09
Repository: TennisTournamentLib

## Update Rule
Update this file after each meaningful implementation/review/change in this repo.
Include: branch, uncommitted state, what changed, and compatibility impact on backend.

## Current State
- Branch: `master`
- Local implementation changes (not committed yet):
  - added: `AGENTS.md`, `AI_CONTEXT.md`
  - modified: `CONTINUITY.md`

## Recent Completed Work
- (uncommitted in current session) Implemented Group and Swiss support in the tournament engine:
  - added `GroupService` single round-robin generation with group-aware match ids/rounds
  - added `SwissService` round-one generation plus standings-based next-round generation
  - extended Swiss config with explicit `advancingCount`; omitted means all players advance
  - refactored `TournamentService` dispatch so Group/Swiss receive full phase/config state
  - added lib tests for group scheduling and Swiss progression behavior
  - validated with:
    - `./gradlew.bat test --no-daemon --tests "parraga.bros.tournament.services.GroupServiceTest"` (pass)
    - `./gradlew.bat test --no-daemon --tests "parraga.bros.tournament.services.SwissServiceTest"` (pass)
    - `./gradlew.bat test --no-daemon` (pass; Kotlin daemon fell back but build succeeded)
- (uncommitted in current session) Added AI operating docs:
  - `AGENTS.md` with workflow rules
  - `AI_CONTEXT.md` for fast onboarding and cross-repo awareness
- `d2b1747` Refactor seeding model to `SeededParticipant` for cross-format usage
- `804c298` Add partial seeded seeding mode
- `5bdced5` Add deterministic seeding strategy
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
  - partial seeding (top seeded participants fixed, remaining participants randomized).
  - explicit participant input (`SeededParticipant(playerId, seed?)`) for seed-driven generation.
- Qualifier-aware round computation exists (`computeRounds`).
- Third-place playoff generation is supported (with constraints).
- Match score application exists (`Match.applyScore`).

## Known Gaps
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
1. Expand test coverage for edge-case scoring and progression invariants across all formats.
2. Decide whether Group/Swiss need richer seeding semantics beyond stable participant order.
3. Complete backend wiring for explicit participant seeds and consider deprecating `seededPlayerCount`.
