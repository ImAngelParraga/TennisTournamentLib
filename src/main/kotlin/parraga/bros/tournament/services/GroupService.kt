package parraga.bros.tournament.services

import parraga.bros.tournament.domain.Format
import parraga.bros.tournament.domain.Match
import parraga.bros.tournament.domain.MatchStatus
import parraga.bros.tournament.domain.Phase
import parraga.bros.tournament.domain.PhaseConfiguration
import parraga.bros.tournament.domain.SeededParticipant

object GroupService : PhaseService {
    override fun startPhase(playerIds: List<Int>): List<Match> {
        val phase = Phase(
            order = 1,
            format = Format.GROUP,
            rounds = 1,
            configuration = PhaseConfiguration.GroupConfig(
                groupCount = 1,
                teamsPerGroup = playerIds.size,
                advancingPerGroup = 1
            ),
            matches = emptyList()
        )
        return startPhase(phase, playerIds.map { SeededParticipant(it) })
    }

    override fun startNextRound(
        nextRoundMatches: List<Match>,
        previousRoundMatches: List<Match>
    ): List<Match> {
        return nextRoundMatches
    }

    fun startPhase(phase: Phase, participants: List<SeededParticipant>): List<Match> {
        val config = phase.configuration as? PhaseConfiguration.GroupConfig
            ?: throw IllegalArgumentException("Group phase requires GroupConfig configuration")
        require(config.groupCount > 0) { "groupCount must be greater than 0" }
        require(config.teamsPerGroup > 1) { "teamsPerGroup must be greater than 1" }
        require(config.advancingPerGroup in 1..config.teamsPerGroup) {
            "advancingPerGroup must be between 1 and teamsPerGroup"
        }
        require(participants.size == config.groupCount * config.teamsPerGroup) {
            "Configured groups require exactly ${config.groupCount * config.teamsPerGroup} participants but got ${participants.size}"
        }

        var nextMatchId = 0
        val matches = mutableListOf<Match>()
        participants.chunked(config.teamsPerGroup).forEachIndexed { index, groupParticipants ->
            val groupId = index + 1
            val (groupMatches, nextId) = createRoundRobinMatches(
                groupId = groupId,
                playerIds = groupParticipants.map { it.playerId },
                startMatchId = nextMatchId
            )
            matches += groupMatches
            nextMatchId = nextId
        }

        return matches
    }

    fun startNextRound(phase: Phase): List<Match> {
        val completedRounds = phase.matches
            .groupBy { it.round }
            .filterValues { roundMatches ->
                roundMatches.all { it.status == MatchStatus.COMPLETED || it.status == MatchStatus.WALKOVER }
            }
            .keys
        val lastCompletedRound = completedRounds.maxOrNull()
            ?: throw IllegalStateException("No completed rounds available")
        val nextRound = lastCompletedRound + 1
        return phase.matches.filter { it.round == nextRound }
    }

    private fun createRoundRobinMatches(
        groupId: Int,
        playerIds: List<Int>,
        startMatchId: Int
    ): Pair<List<Match>, Int> {
        val rotatedPlayers = playerIds.map { it as Int? }.toMutableList()
        if (rotatedPlayers.size % 2 != 0) {
            rotatedPlayers += null
        }

        val rounds = rotatedPlayers.size - 1
        var nextMatchId = startMatchId
        val matches = mutableListOf<Match>()

        repeat(rounds) { roundIndex ->
            for (pairIndex in 0 until rotatedPlayers.size / 2) {
                val player1 = rotatedPlayers[pairIndex]
                val player2 = rotatedPlayers[rotatedPlayers.lastIndex - pairIndex]
                if (player1 != null && player2 != null) {
                    matches += Match(
                        id = nextMatchId++,
                        round = roundIndex + 1,
                        groupId = groupId,
                        player1Id = player1,
                        player2Id = player2,
                        status = MatchStatus.SCHEDULED
                    )
                }
            }

            val lastPlayer = rotatedPlayers.removeLast()
            rotatedPlayers.add(1, lastPlayer)
        }

        return matches to nextMatchId
    }
}
