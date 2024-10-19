package services

import domain.Match
import domain.Player
import kotlin.math.ln

@Suppress("unused")
class KnockoutService {
	fun startStage(players: List<Player>): List<Match> {
		val playersEven = addByeIfNecessary(players)
		return getRoundFromPlayers(playersEven)
	}

	fun getNextRound(matches: List<Match>): List<Match> {
		val nextRoundMatches = mutableListOf<Match>()
		val winners = getWinnersByMatchId(matches)

		for ((matchId, i) in (0..<winners.size step 2).withIndex()) {
			if (winners[i] == null || winners[i + 1] == null)
				throw IllegalStateException("Match must have winner. Round is not over yet.")

			nextRoundMatches.add(Match(matchId, winners[i]!!, winners[i + 1]!!))
		}

		return nextRoundMatches
	}

	private fun getWinnersByMatchId(matches: List<Match>): Map<Int, Player> {
		val winners = mutableMapOf<Int, Player>()
		matches.forEach {
			if (it.winner == null) throw IllegalStateException("Match must have winner. Round is not over yet.")
			winners[it.matchId] = it.winner!!
		}

		return winners
	}

	private fun getRoundFromPlayers(players: MutableList<Player>): List<Match> {
		players.shuffle()
		val matches = mutableListOf<Match>()
		for ((matchId, i) in (players.indices step 2).withIndex()) {
			matches.add(Match(matchId, players[i], players[i + 1]))
		}

		return matches
	}

	private fun addByeIfNecessary(players: List<Player>): MutableList<Player> {
		val playersWithBye = players.toMutableList()
		val nextPowerOfTwo = nextPowerOfTwo(players.size)

		while (playersWithBye.size < nextPowerOfTwo) {
			playersWithBye.add(createPlayerBye())
		}

		return playersWithBye
	}

	private fun nextPowerOfTwo(number: Int): Int {
		var power = 1
		while (power < number) {
			power *= 2
		}
		return power
	}


	private fun calculateTotalRounds(players: Int): Int {
		require(players > 1 && players and (players - 1) == 0) { "Players number must be even" }
		return (ln(players.toDouble()) / ln(2.0)).toInt()
	}

	private fun createPlayerBye(): Player = Player(-1, "Bye")
}