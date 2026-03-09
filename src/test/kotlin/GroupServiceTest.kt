package parraga.bros.tournament.services

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

class GroupServiceTest {

    @Test
    fun `startPhase generates single round robin matches for each group`() {
        val phase = Phase(
            order = 1,
            format = Format.GROUP,
            rounds = 3,
            configuration = PhaseConfiguration.GroupConfig(
                groupCount = 2,
                teamsPerGroup = 4,
                advancingPerGroup = 2
            ),
            matches = emptyList()
        )

        val matches = GroupService.startPhase(phase, (1..8).map { SeededParticipant(it) })

        assertEquals(12, matches.size)
        assertEquals(setOf(1, 2), matches.mapNotNull { it.groupId }.toSet())
        assertEquals(6, matches.count { it.groupId == 1 })
        assertEquals(6, matches.count { it.groupId == 2 })
        assertEquals(setOf(1, 2, 3), matches.map { it.round }.toSet())
        assertTrue(matches.all { it.status == MatchStatus.SCHEDULED })
    }

    @Test
    fun `startPhase supports odd-sized groups by rotating a bye`() {
        val phase = Phase(
            order = 1,
            format = Format.GROUP,
            rounds = 3,
            configuration = PhaseConfiguration.GroupConfig(
                groupCount = 1,
                teamsPerGroup = 3,
                advancingPerGroup = 1
            ),
            matches = emptyList()
        )

        val matches = GroupService.startPhase(phase, (1..3).map { SeededParticipant(it) })

        assertEquals(3, matches.size)
        assertEquals(setOf(1, 2, 3), matches.map { it.round }.toSet())
        assertEquals((1..3).toSet(), matches.flatMap { listOfNotNull(it.player1Id, it.player2Id) }.toSet())
    }

    @Test
    fun `startPhase rejects participant counts that do not fill configured groups`() {
        val phase = Phase(
            order = 1,
            format = Format.GROUP,
            rounds = 3,
            configuration = PhaseConfiguration.GroupConfig(
                groupCount = 2,
                teamsPerGroup = 4,
                advancingPerGroup = 2
            ),
            matches = emptyList()
        )

        assertFailsWith<IllegalArgumentException> {
            GroupService.startPhase(phase, (1..7).map { SeededParticipant(it) })
        }
    }

    @Test
    fun `startNextRound returns the next scheduled group round after current round completes`() {
        val phase = Phase(
            order = 1,
            format = Format.GROUP,
            rounds = 3,
            configuration = PhaseConfiguration.GroupConfig(
                groupCount = 1,
                teamsPerGroup = 4,
                advancingPerGroup = 1
            ),
            matches = completedThroughRound(
                GroupService.startPhase(
                    phase = Phase(
                        order = 1,
                        format = Format.GROUP,
                        rounds = 3,
                        configuration = PhaseConfiguration.GroupConfig(1, 4, 1),
                        matches = emptyList()
                    ),
                    participants = (1..4).map { SeededParticipant(it) }
                ),
                throughRound = 1
            )
        )

        val nextRound = GroupService.startNextRound(phase)

        assertTrue(nextRound.isNotEmpty())
        assertTrue(nextRound.all { it.round == 2 })
    }

    private fun completedThroughRound(matches: List<Match>, throughRound: Int): List<Match> {
        return matches.map { match ->
            if (match.round > throughRound) {
                match
            } else {
                match.copy(
                    winnerId = match.player1Id,
                    score = TennisScore(listOf(SetScore(6, 0, null), SetScore(6, 0, null))),
                    status = MatchStatus.COMPLETED
                )
            }
        }
    }
}
