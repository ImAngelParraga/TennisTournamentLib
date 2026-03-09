package parraga.bros.tournament.services

import parraga.bros.tournament.domain.Format
import parraga.bros.tournament.domain.Match
import parraga.bros.tournament.domain.MatchStatus
import parraga.bros.tournament.domain.Phase
import parraga.bros.tournament.domain.PhaseConfiguration
import parraga.bros.tournament.domain.SeededParticipant
import kotlin.math.ceil
import kotlin.math.log2

object SwissService : PhaseService {
    override fun startPhase(playerIds: List<Int>): List<Match> {
        val rounds = ceil(log2(playerIds.size.toDouble())).toInt().coerceAtLeast(1)
        val phase = Phase(
            order = 1,
            format = Format.SWISS,
            rounds = rounds,
            configuration = PhaseConfiguration.SwissConfig(pointsPerWin = 1, advancingCount = null),
            matches = emptyList()
        )
        return startPhase(phase, playerIds.map { SeededParticipant(it) })
    }

    override fun startNextRound(
        nextRoundMatches: List<Match>,
        previousRoundMatches: List<Match>
    ): List<Match> {
        throw UnsupportedOperationException("Swiss next round generation requires full phase state")
    }

    fun startPhase(phase: Phase, participants: List<SeededParticipant>): List<Match> {
        val config = phase.configuration as? PhaseConfiguration.SwissConfig
            ?: throw IllegalArgumentException("Swiss phase requires SwissConfig configuration")
        require(config.pointsPerWin > 0) { "pointsPerWin must be greater than 0" }
        require(config.advancingCount == null || config.advancingCount >= 2) {
            "advancingCount must be at least 2 when provided"
        }
        require(config.advancingCount == null || config.advancingCount <= participants.size) {
            "advancingCount cannot exceed participant count"
        }
        require(phase.rounds > 0) { "Swiss phase rounds must be greater than 0" }
        require(participants.size >= 2) { "Swiss phase requires at least 2 players" }

        return createRoundMatches(
            round = 1,
            orderedPlayerIds = participants.map { it.playerId },
            startMatchId = 0
        )
    }

    fun startNextRound(phase: Phase): List<Match> {
        val config = phase.configuration as? PhaseConfiguration.SwissConfig
            ?: throw IllegalArgumentException("Swiss phase requires SwissConfig configuration")
        require(config.pointsPerWin > 0) { "pointsPerWin must be greater than 0" }

        val completedRounds = phase.matches
            .groupBy { it.round }
            .filterValues { roundMatches ->
                roundMatches.all { it.status == MatchStatus.COMPLETED || it.status == MatchStatus.WALKOVER }
            }
            .keys
        val lastCompletedRound = completedRounds.maxOrNull()
            ?: throw IllegalStateException("No completed rounds available")
        if (lastCompletedRound >= phase.rounds) return emptyList()

        val nextRound = lastCompletedRound + 1
        if (phase.matches.any { it.round == nextRound }) {
            return phase.matches.filter { it.round == nextRound }
        }

        val pointsByPlayer = mutableMapOf<Int, Int>().withDefault { 0 }
        phase.matches
            .filter { it.round <= lastCompletedRound }
            .forEach { match ->
                val winnerId = match.winnerId ?: return@forEach
                pointsByPlayer[winnerId] = (pointsByPlayer[winnerId] ?: 0) + config.pointsPerWin
            }

        val tiebreakOrder = phase.matches
            .filter { it.round == 1 }
            .sortedBy { it.id }
            .flatMap { listOfNotNull(it.player1Id, it.player2Id) }
            .distinct()
            .withIndex()
            .associate { it.value to it.index }

        val orderedPlayers = phase.matches
            .flatMap { listOfNotNull(it.player1Id, it.player2Id) }
            .distinct()
            .sortedWith(
                compareByDescending<Int> { pointsByPlayer[it] ?: 0 }
                    .thenBy { tiebreakOrder[it] ?: Int.MAX_VALUE }
                    .thenBy { it }
            )

        return createRoundMatches(
            round = nextRound,
            orderedPlayerIds = orderedPlayers,
            startMatchId = (phase.matches.maxOfOrNull { it.id } ?: -1) + 1
        )
    }

    private fun createRoundMatches(round: Int, orderedPlayerIds: List<Int>, startMatchId: Int): List<Match> {
        val matches = mutableListOf<Match>()
        var nextMatchId = startMatchId
        var index = 0

        while (index < orderedPlayerIds.size) {
            val player1 = orderedPlayerIds[index]
            val player2 = orderedPlayerIds.getOrNull(index + 1)

            matches += Match(
                id = nextMatchId++,
                round = round,
                player1Id = player1,
                player2Id = player2,
                winnerId = if (player2 == null) player1 else null,
                status = if (player2 == null) MatchStatus.WALKOVER else MatchStatus.SCHEDULED
            )

            index += 2
        }

        return matches
    }
}
