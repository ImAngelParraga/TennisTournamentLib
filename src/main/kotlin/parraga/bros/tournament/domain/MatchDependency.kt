package parraga.bros.tournament.domain

data class MatchDependency(
    val requiredMatchId: Int,
    val requiredOutcome: Outcome
)

enum class Outcome { WINNER, LOSER }