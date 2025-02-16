package parraga.bros.tournament.services

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import parraga.bros.tournament.domain.Match
import parraga.bros.tournament.domain.MatchDependency
import parraga.bros.tournament.domain.MatchStatus
import parraga.bros.tournament.domain.Outcome

class KnockoutServiceTest {

    // ------------------------- Tests for startPhase -------------------------
    @Test
    fun `startPhase with 4 players generates semifinals and final matches`() {
        val players = listOf(1, 2, 3, 4)
        val matches = KnockoutService.startPhase(players)

        assertEquals(2, matches.count { it.round == 1 }, "Round 1 (semifinals) should have 2 matches")
        assertEquals(1, matches.count { it.round == 2 }, "Round 2 (final) should have 1 match")
        assertEquals(3, matches.size, "Total matches (semifinals + final) should be 3")
    }

    @Test
    fun `startPhase with 5 players adds byes and generates rounds 1, 2, and 3`() {
        val players = listOf(1, 2, 3, 4, 5)
        val matches = KnockoutService.startPhase(players)

        // 8 players (5 real + 3 byes).
        // Round 1: 4 matches, Round 2: 2 matches, Round 3: 1 match
        assertEquals(4, matches.count { it.round == 1 }, "Round 1 should have 4 matches")
        assertEquals(2, matches.count { it.round == 2 }, "Round 2 should have 2 matches")
        assertEquals(1, matches.count { it.round == 3 }, "Round 3 should have 1 match")
        assertEquals(7, matches.size, "Incorrect total number of generated matches")
    }

    @Test
    fun `startPhase with 2 players generates 1 match in round 1`() {
        val players = listOf(1, 2)
        val matches = KnockoutService.startPhase(players)

        assertEquals(1, matches.size, "There should be exactly 1 match in round 1")
        assertEquals(1, matches[0].round, "The match should belong to round 1")
    }

    @Test
    fun `startPhase with maximum possible byes does not create matches with both players as bye`() {
        val players = (1..9).toList()
        val matches = KnockoutService.startPhase(players)

        val round1Matches = matches.filter { it.round == 1 }
        assertTrue(round1Matches.isNotEmpty(), "No matches generated for round 1")

        round1Matches.forEach { match ->
            val bothByes = match.player1Id == -1 && match.player2Id == -1
            assertTrue(!bothByes, "Match ${match.id} in round 1 has two byes (-1 vs -1)")
        }

        val round1MatchesWithBye = round1Matches.count { it.status == MatchStatus.WALKOVER }
        assertTrue(round1MatchesWithBye == 7, "There should be 7 matches with byes in round 1")
    }

    // ------------------------- Tests for startNextRound -------------------------
    @Test
    fun `startNextRound correctly assigns players when dependencies are completed`() {
        // Previous round matches (completed)
        val previousMatches = listOf(
            Match(1, 1, player1Id = 1, player2Id = 2, winnerId = 1, status = MatchStatus.COMPLETED),
            Match(2, 1, player1Id = 3, player2Id = 4, winnerId = 3, status = MatchStatus.COMPLETED)
        )

        // Next round matches (with dependencies)
        val nextRoundMatches = listOf(
            Match(
                id = 3,
                round = 2,
                dependencies = listOf(
                    MatchDependency(1, Outcome.WINNER),
                    MatchDependency(2, Outcome.WINNER)
                ),
                status = MatchStatus.SCHEDULED
            )
        )

        val updatedMatches = KnockoutService.startNextRound(nextRoundMatches, previousMatches)

        assertEquals(1, updatedMatches.size)
        assertEquals(1, updatedMatches[0].player1Id, "Player 1 should be the winner of dependency 1")
        assertEquals(3, updatedMatches[0].player2Id, "Player 2 should be the winner of dependency 2")
    }

    @Test
    fun `startNextRound throws exception if a dependency does not exist`() {
        val previousMatches = listOf(
            Match(1, 1, player1Id = 1, player2Id = 2, winnerId = 1, status = MatchStatus.COMPLETED)
        )

        val nextRoundMatches = listOf(
            Match(
                id = 3,
                round = 2,
                dependencies = listOf(
                    MatchDependency(1, Outcome.WINNER),
                    MatchDependency(999, Outcome.WINNER) // Non-existent ID
                ),
                status = MatchStatus.SCHEDULED
            )
        )

        val exception = assertThrows<IllegalStateException> {
            KnockoutService.startNextRound(nextRoundMatches, previousMatches)
        }

        assertTrue(exception.message!!.contains("Dependent match 999 not found"))
    }

    @Test
    fun `startNextRound throws exception if a dependent match is not completed`() {
        val previousMatches = listOf(
            Match(1, 1, player1Id = 1, player2Id = 2, winnerId = null, status = MatchStatus.LIVE),
            Match(2, 1, player1Id = 3, player2Id = 4, winnerId = null, status = MatchStatus.LIVE)
        )

        val nextRoundMatches = listOf(
            Match(
                id = 3,
                round = 2,
                dependencies = listOf(
                    MatchDependency(1, Outcome.WINNER),
                    MatchDependency(2, Outcome.WINNER)
                ),
                status = MatchStatus.SCHEDULED
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            KnockoutService.startNextRound(nextRoundMatches, previousMatches)
        }

        assertTrue(exception.message!!.contains("is not completed or has no winner"))
    }

    @Test
    fun `startNextRound throws exception if a match does not have exactly 2 dependencies`() {
        val previousMatches = listOf(
            Match(1, 1, player1Id = 1, player2Id = 2, winnerId = 1, status = MatchStatus.COMPLETED)
        )

        val invalidNextRoundMatches = listOf(
            Match( // Only 1 dependency
                id = 3,
                round = 2,
                dependencies = listOf(MatchDependency(1, Outcome.WINNER)),
                status = MatchStatus.SCHEDULED
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            KnockoutService.startNextRound(invalidNextRoundMatches, previousMatches)
        }

        assertTrue(exception.message!!.contains("Each knockout match must have exactly 2 dependencies"))
    }
}