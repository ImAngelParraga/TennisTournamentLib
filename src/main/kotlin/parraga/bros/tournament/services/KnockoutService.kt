package parraga.bros.tournament.services

import parraga.bros.tournament.domain.Match
import parraga.bros.tournament.domain.MatchDependency
import parraga.bros.tournament.domain.MatchStatus
import parraga.bros.tournament.domain.Outcome
import parraga.bros.tournament.domain.SeedingStrategy
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.pow

object KnockoutService : PhaseService {
    override fun startPhase(playerIds: List<Int>): List<Match> =
        startPhase(
            playerIds = playerIds,
            qualifiers = 1,
            thirdPlacePlayoff = false,
            seedingStrategy = SeedingStrategy.INPUT_ORDER,
            seededPlayerCount = 0
        )

    fun startPhase(
        playerIds: List<Int>,
        qualifiers: Int,
        thirdPlacePlayoff: Boolean = false,
        seedingStrategy: SeedingStrategy = SeedingStrategy.INPUT_ORDER,
        seededPlayerCount: Int = 0
    ): List<Match> {
        require(playerIds.size >= 2) { "Tournament must have at least 2 players" }
        if (thirdPlacePlayoff) {
            require(qualifiers == 1) { "Third-place playoff requires qualifiers to be 1" }
            require(playerIds.size >= 4) { "Third-place playoff requires at least 4 players" }
        }
        when (seedingStrategy) {
            SeedingStrategy.PARTIAL_SEEDED -> {
                require(seededPlayerCount in 1 until playerIds.size) {
                    "Partial seeding requires seededPlayerCount between 1 and ${playerIds.size - 1}"
                }
            }

            else -> {
                require(seededPlayerCount == 0) {
                    "seededPlayerCount is only supported with PARTIAL_SEEDED strategy"
                }
            }
        }

        val totalRounds = ceil(log2(playerIds.size.toDouble())).toInt()
        val roundsToPlay = computeRounds(playerIds.size, qualifiers)
        if (roundsToPlay == 0) return emptyList()

        val nextPowerOfTwo = 2.0.pow(totalRounds).toInt()
        val numByes = nextPowerOfTwo - playerIds.size

        val allMatches = mutableListOf<Match>()
        var matchId = 0

        val playerPairs = groupPlayerIdsIntoPairs(playerIds, numByes, seedingStrategy, seededPlayerCount)
        playerPairs.forEach {
            val newMatch = Match(
                id = matchId++,
                round = 1,
                player1Id = it.first,
                player2Id = it.second,
                winnerId = if (it.second == null) it.first else null,
                // Byes are always set for the second player
                status = if (it.second == null) MatchStatus.WALKOVER else MatchStatus.SCHEDULED,
                dependencies = emptyList()
            )

            allMatches.add(newMatch)
        }

        var currentMatches = allMatches

        for (currentRound in 2 .. roundsToPlay) {
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

        if (thirdPlacePlayoff) {
            val semifinalRound = roundsToPlay - 1
            val semifinalMatches = allMatches.filter { it.round == semifinalRound }
            require(semifinalMatches.size == 2) { "Third-place playoff requires exactly 2 semifinal matches" }

            val thirdPlaceMatch = Match(
                id = matchId++,
                round = roundsToPlay,
                player1Id = null,
                player2Id = null,
                status = MatchStatus.SCHEDULED,
                dependencies = semifinalMatches.map { MatchDependency(it.id, Outcome.LOSER) }
            )
            allMatches.add(thirdPlaceMatch)
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

                val isFinished = depMatch.status == MatchStatus.COMPLETED || depMatch.status == MatchStatus.WALKOVER
                require(isFinished && depMatch.winnerId != null) { "Match ${depMatch
                    .id} is not completed or has no winner" }

                depMatch
            }

            match.setPlayerIdsByPreviousMatches(depMatches)
            match
        }
    }

    private fun groupPlayerIdsIntoPairs(
        playerIds: List<Int>,
        numByes: Int,
        seedingStrategy: SeedingStrategy,
        seededPlayerCount: Int
    ): List<Pair<Int, Int?>> {
        return when (seedingStrategy) {
            SeedingStrategy.INPUT_ORDER -> {
                val pairs = mutableListOf<Pair<Int, Int?>>()
                for (i in 0 until numByes) {
                    pairs.add(playerIds[i] to null)
                }

                val remaining = playerIds.drop(numByes)
                remaining.chunked(2) { pair -> pairs.add(pair[0] to pair[1]) }
                pairs
            }

            SeedingStrategy.RANDOM -> {
                val shuffledPlayerIds = playerIds.shuffled()
                val pairs = mutableListOf<Pair<Int, Int?>>()
                for (i in 0 until numByes) {
                    pairs.add(shuffledPlayerIds[i] to null)
                }

                val remaining = shuffledPlayerIds.drop(numByes)
                remaining.chunked(2) { pair -> pairs.add(pair[0] to pair[1]) }
                pairs.shuffled()
            }

            SeedingStrategy.PARTIAL_SEEDED -> {
                val seededPlayers = playerIds.take(seededPlayerCount)
                val unseededPlayers = playerIds.drop(seededPlayerCount).shuffled().toMutableList()
                val pairs = mutableListOf<Pair<Int, Int?>>()

                val byesForSeeded = minOf(numByes, seededPlayers.size)
                for (i in 0 until byesForSeeded) {
                    pairs.add(seededPlayers[i] to null)
                }

                repeat(numByes - byesForSeeded) {
                    val byePlayer = unseededPlayers.removeAt(0)
                    pairs.add(byePlayer to null)
                }

                val seededQueue = ArrayDeque(seededPlayers.drop(byesForSeeded))
                while (seededQueue.isNotEmpty()) {
                    val seed = seededQueue.removeFirst()
                    val opponent = when {
                        unseededPlayers.isNotEmpty() -> unseededPlayers.removeAt(0)
                        seededQueue.isNotEmpty() -> seededQueue.removeFirst()
                        else -> null
                    }
                    pairs.add(seed to opponent)
                }

                unseededPlayers.chunked(2) { pair ->
                    if (pair.size == 2) {
                        pairs.add(pair[0] to pair[1])
                    } else {
                        pairs.add(pair[0] to null)
                    }
                }
                pairs
            }
        }
    }

    fun computeRounds(playerCount: Int, qualifiers: Int): Int {
        require(qualifiers >= 1) { "Qualifiers must be at least 1" }
        require(qualifiers <= playerCount) { "Qualifiers ($qualifiers) cannot exceed player count ($playerCount)" }
        require(isPowerOfTwo(qualifiers)) { "Qualifiers ($qualifiers) must be a power of two" }

        val totalRounds = ceil(log2(playerCount.toDouble())).toInt()
        val qualifierRounds = log2(qualifiers.toDouble()).toInt()
        return (totalRounds - qualifierRounds).coerceAtLeast(0)
    }

    private fun isPowerOfTwo(value: Int): Boolean = value > 0 && (value and (value - 1)) == 0
}
