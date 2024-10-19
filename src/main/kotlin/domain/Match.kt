package domain

/**
 * @param matchId ID of the match for the round. Starts from 0. Winner of the match 0 will play against winner of
 *  match 1 in the next round, and so on.
 */
@Suppress("unused")
data class Match(
	val matchId: Int,
	val player1: Player,
	val player2: Player,
	var sets: List<SetResult> = emptyList(),
	var winner: Player? = null
) {
	fun determineWinner() {
		require(sets.isNotEmpty()) { "Match must have sets" }

		val player1Wins = sets.count {
			when (it) {
				is SetResult.RegularSet -> it.player1Games > it.player2Games
				is SetResult.MatchTiebreak -> it.player1Points > it.player2Points
			}
		}

		val player2Wins = sets.size - player1Wins

		winner = if (player1Wins > player2Wins) player1 else player2
	}

	fun setSets(sets: List<SetResult>) {
		require(sets.size % 2 == 1) { "Sets number must be odd" }

		this.sets = sets
	}
}
