package parraga.bros.tournament.domain


data class Match(
    val id: Int,
    val round: Int,
    val groupId: Int? = null,
    var player1Id: Int? = null,
    var player2Id: Int? = null,
    var winnerId: Int? = null,
    val score: TennisScore? = null,
    var status: MatchStatus,
    val dependencies: List<MatchDependency> = emptyList()
) {
    fun setPlayerIdsByPreviousMatches(previousMatches: List<Match>) {
        dependencies.forEachIndexed { index, dependency ->
            val match = previousMatches.find {
                it.id == dependency.requiredMatchId
            }

            require(match != null) { "Previous matches don't contain required match id " +
                    "[${dependency.requiredMatchId}] from this match (id: $id) dependencies." }
            require(match.status == MatchStatus.COMPLETED) { "Match [${match.id} is not completed." }
            require(match.winnerId != null) { "WinnerId for match ${match.id} is null when match is completed." }
            require(match.player1Id != null) { "Player1 id is null." }
            require(match.player2Id != null) { "Player2 id is null." }

            val playerFromOutcome = when (dependency.requiredOutcome) {
                Outcome.WINNER -> match.winnerId
                Outcome.LOSER -> if (match.player1Id == match.winnerId) player2Id!! else player1Id!!
            }

            if (index == 0) {
                player1Id = playerFromOutcome
            } else if (index == 1) {
                player2Id = playerFromOutcome
            }
        }
    }

}

data class TennisScore(
    val sets: List<SetScore>
)

data class SetScore(
    val player1Games: Int,
    val player2Games: Int,
    val tiebreak: TiebreakScore?
)

data class GameScore(
    val player1Points: Int,
    val player2Points: Int
)

data class TiebreakScore(
    val player1Points: Int,
    val player2Points: Int
)

enum class MatchStatus { SCHEDULED, LIVE, COMPLETED, WALKOVER }