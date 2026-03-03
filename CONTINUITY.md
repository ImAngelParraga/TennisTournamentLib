# CONTINUITY

Last Updated: 2026-03-03
Repository: TennisTournamentLib

## Update Rule
Update this file after each meaningful implementation/review/change in this repo.
Include: branch, uncommitted state, what changed, and compatibility impact on backend.

## Current State
- Branch: `master`
- Local state currently includes user-side pending files:
  - staged: `.aiignore`
  - untracked: `SESSION_HANDOFF.md`

## Recent Completed Work
- `dd3a556` Add third-place playoff bracket support
- `ef95218` Add qualifiers and scoring helpers
- `a62eccf` Fix knockout loser selection and bye winners
- `ee4f656` Use null instead of -1 for byes
- `2b63e72` Fix bug where a first round match has 2 byes players. Add test.

## Current Functional Baseline
- Knockout bracket generation is implemented.
- Qualifier-aware round computation exists (`computeRounds`).
- Third-place playoff generation is supported (with constraints).
- Match score application exists (`Match.applyScore`).

## Known Gaps
- `GroupService` remains TODO.
- `SwissService` remains TODO.
- Deterministic seeding strategy is not implemented (currently randomized/shuffled).
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
1. Define and implement deterministic seeding API/behavior.
2. Decide product direction for Group/Swiss (implement or remove from public contract for now).
3. Expand test coverage for edge-case scoring and progression invariants.
