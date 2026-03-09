package parraga.bros.tournament.services

import kotlin.math.ceil
import kotlin.math.log2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import parraga.bros.tournament.domain.Format
import parraga.bros.tournament.domain.Match
import parraga.bros.tournament.domain.MatchStatus
import parraga.bros.tournament.domain.Phase
import parraga.bros.tournament.domain.PhaseConfiguration
import parraga.bros.tournament.domain.SeededParticipant
import parraga.bros.tournament.domain.SetScore
import parraga.bros.tournament.domain.TennisScore

class SwissServiceTest {

    @Test
    fun `startPhase creates only round one matches and assigns a bye when needed`() {
        val phase = swissPhase(rounds = 3)

        val matches = SwissService.startPhase(phase, (1..5).map { SeededParticipant(it) })

        assertEquals(3, matches.size)
        assertTrue(matches.all { it.round == 1 })
        assertEquals(1, matches.count { it.status == MatchStatus.WALKOVER })
    }

    @Test
    fun `startNextRound pairs players by cumulative points`() {
        val phase = swissPhase(
            rounds = 3,
            matches = listOf(
                completedSwissMatch(id = 0, round = 1, player1Id = 1, player2Id = 2, winnerId = 1),
                completedSwissMatch(id = 1, round = 1, player1Id = 3, player2Id = 4, winnerId = 4),
                Match(id = 2, round = 1, player1Id = 5, player2Id = null, winnerId = 5, status = MatchStatus.WALKOVER)
            )
        )

        val nextRound = SwissService.startNextRound(phase)

        assertEquals(3, nextRound.size)
        assertEquals(2, nextRound.first().round)
        assertEquals(listOf(1, 4), listOf(nextRound[0].player1Id, nextRound[0].player2Id))
        assertEquals(listOf(5, 2), listOf(nextRound[1].player1Id, nextRound[1].player2Id))
        assertEquals(3, nextRound[2].player1Id)
        assertEquals(MatchStatus.WALKOVER, nextRound[2].status)
    }

    @Test
    fun `startNextRound returns empty when swiss phase already reached configured rounds`() {
        val phase = swissPhase(
            rounds = 1,
            matches = listOf(
                completedSwissMatch(id = 0, round = 1, player1Id = 1, player2Id = 2, winnerId = 1)
            )
        )

        assertTrue(SwissService.startNextRound(phase).isEmpty())
    }

    @Test
    fun `startPhase rejects non-positive points per win`() {
        val phase = Phase(
            order = 1,
            format = Format.SWISS,
            rounds = ceil(log2(4.0)).toInt(),
            configuration = PhaseConfiguration.SwissConfig(pointsPerWin = 0, advancingCount = null),
            matches = emptyList()
        )

        assertFailsWith<IllegalArgumentException> {
            SwissService.startPhase(phase, (1..4).map { SeededParticipant(it) })
        }
    }

    private fun swissPhase(rounds: Int, matches: List<Match> = emptyList()): Phase = Phase(
        order = 1,
        format = Format.SWISS,
        rounds = rounds,
        configuration = PhaseConfiguration.SwissConfig(pointsPerWin = 1, advancingCount = null),
        matches = matches
    )

    private fun completedSwissMatch(
        id: Int,
        round: Int,
        player1Id: Int,
        player2Id: Int,
        winnerId: Int
    ): Match = Match(
        id = id,
        round = round,
        player1Id = player1Id,
        player2Id = player2Id,
        winnerId = winnerId,
        score = TennisScore(listOf(SetScore(6, 4, null), SetScore(6, 4, null))),
        status = MatchStatus.COMPLETED
    )
}
