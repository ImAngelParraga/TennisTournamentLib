# AI Context: TennisTournamentLib

## Read First
1. `AGENTS.md`
2. `CONTINUITY.md`
3. `../TennisTournamentBackend/AI_CONTEXT.md` (cross-repo integration context)

## Project Role
- Tournament engine library used by backend for generation/progression semantics.
- Backend path: `C:\Users\ranki\IdeaProjects\TennisTournamentBackend`

## Current Functional Focus
- Knockout format is actively used and tested.
- Group/Swiss services still exist but are TODO/incomplete.
- Match scoring helper (`Match.applyScore`) is used by backend score flow.

## Seeding Status
- Seeding strategies: `INPUT_ORDER`, `RANDOM`, `PARTIAL_SEEDED`.
- Explicit participant model exists:
  - `SeededParticipant(playerId, seed?)`
- Participant-based API is available:
  - `TournamentService.startPhaseWithParticipants(...)`
- Knockout consumes seed-aware participants.
- Group/Swiss currently accept participant input but do not yet implement seed-specific behavior.

## Testing
- Primary command: `./gradlew.bat test --no-daemon`
- If contract changed, also run backend tests in `../TennisTournamentBackend`.

## User Workflow Preferences
- Do not commit/push unless user explicitly asks.
- If commit is requested, push after each commit.
- Keep `CONTINUITY.md` updated.
