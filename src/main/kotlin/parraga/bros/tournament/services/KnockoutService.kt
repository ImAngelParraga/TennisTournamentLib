package parraga.bros.tournament.services

import parraga.bros.tournament.domain.Match
import parraga.bros.tournament.domain.MatchDependency
import parraga.bros.tournament.domain.MatchStatus
import parraga.bros.tournament.domain.Outcome
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.pow

object KnockoutService : PhaseService {
    override fun startPhase(playerIds: List<Int>): List<Match> {
        require(playerIds.size >= 2) { "Tournament must have at least 2 players" }

        val totalRounds = ceil(log2(playerIds.size.toDouble())).toInt()
        val nextPowerOfTwo = 2.0.pow(totalRounds).toInt()
        val numByes = nextPowerOfTwo - playerIds.size

        val allMatches = mutableListOf<Match>()
        var matchId = 0

        val playerPairs = groupPlayerIdsIntoPairs(playerIds, numByes)
        playerPairs.forEach {
            val newMatch = Match(
                id = matchId++,
                round = 1,
                player1Id = it.first,
                player2Id = it.second,
                // Byes are always set for the second player
                status = if (it.second == null) MatchStatus.WALKOVER else MatchStatus.SCHEDULED,
                dependencies = emptyList()
            )

            allMatches.add(newMatch)
        }

        var currentMatches = allMatches

        for (currentRound in 2 .. totalRounds) {
            val nextRoundMatches = mutableListOf<Match>()

            currentMatches.chunked(2) { parentMatches ->
                val newMatch = Match(
                    id = matchId++,
                    round = currentRound,
                    player1Id = null,
                    player2Id = null,
                    status = MatchStatus.SCHEDULED,
                    dependencies = parentMatches.map {
                        MatchDependency(it.id, Outcome.WINNER)
                    }
                )
                nextRoundMatches.add(newMatch)
            }

            allMatches.addAll(nextRoundMatches)
            currentMatches = nextRoundMatches
        }

        return allMatches
    }

    override fun startNextRound(nextRoundMatches: List<Match>, previousRoundMatches: List<Match>): List<Match> {
        if (previousRoundMatches.isEmpty()) return emptyList()

        return nextRoundMatches.map { match ->
            require(match.dependencies.size == 2) { "Each knockout match must have exactly 2 dependencies" }

            val depMatches = match.dependencies.map { dependency ->
                val depMatch = previousRoundMatches.find { it.id == dependency.requiredMatchId }
                    ?: throw IllegalStateException("Dependent match ${dependency.requiredMatchId} not found")

                require(depMatch.status == MatchStatus.COMPLETED && depMatch.winnerId != null) { "Match ${depMatch
                    .id} is not completed or has no winner" }

                depMatch
            }

            match.setPlayerIdsByPreviousMatches(depMatches)
            match
        }
    }

    private fun groupPlayerIdsIntoPairs(playerIds: List<Int>, numByes: Int): List<Pair<Int, Int?>> {
        val shuffledPlayerIds = playerIds.shuffled()

        val pairs = mutableListOf<Pair<Int, Int?>>()
        for (i in 0 until numByes) {
            pairs.add(shuffledPlayerIds[i] to null)
        }

        val remaining = shuffledPlayerIds.drop(numByes)
        remaining.chunked(2) { pair -> pairs.add(pair[0] to pair[1]) }

        return pairs.shuffled()
    }
}