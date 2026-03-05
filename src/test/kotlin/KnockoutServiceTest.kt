package parraga.bros.tournament.services

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import parraga.bros.tournament.domain.Match
import parraga.bros.tournament.domain.MatchDependency
import parraga.bros.tournament.domain.MatchStatus
import parraga.bros.tournament.domain.Outcome
import parraga.bros.tournament.domain.SeededParticipant
import parraga.bros.tournament.domain.SeedingStrategy

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
    fun `startPhase uses input order seeding by default`() {
        val players = listOf(10, 20, 30, 40)

        val matches = KnockoutService.startPhase(players)
        val round1Matches = matches.filter { it.round == 1 }.sortedBy { it.id }

        assertEquals(2, round1Matches.size)
        assertEquals(10, round1Matches[0].player1Id)
        assertEquals(20, round1Matches[0].player2Id)
        assertEquals(30, round1Matches[1].player1Id)
        assertEquals(40, round1Matches[1].player2Id)
    }

    @Test
    fun `startPhase with input order seeding assigns byes to first players`() {
        val players = listOf(1, 2, 3, 4, 5)

        val matches = KnockoutService.startPhase(
            playerIds = players,
            qualifiers = 1,
            thirdPlacePlayoff = false,
            seedingStrategy = SeedingStrategy.INPUT_ORDER
        )

        val round1Matches = matches.filter { it.round == 1 }.sortedBy { it.id }
        val byeMatches = round1Matches.filter { it.player2Id == null }

        assertEquals(3, byeMatches.size, "Expected three byes for 5 players in an 8-slot bracket")
        assertEquals(listOf(1, 2, 3), byeMatches.mapNotNull { it.player1Id }, "Byes should be assigned to first seeds")
    }

    @Test
    fun `startPhase with random seeding still assigns each player exactly once in round 1`() {
        val players = (1..8).toList()

        val matches = KnockoutService.startPhase(
            playerIds = players,
            qualifiers = 1,
            thirdPlacePlayoff = false,
            seedingStrategy = SeedingStrategy.RANDOM
        )

        val round1Matches = matches.filter { it.round == 1 }
        val assignedPlayers = round1Matches
            .flatMap { listOfNotNull(it.player1Id, it.player2Id) }
            .toSet()

        assertEquals(players.toSet(), assignedPlayers)
    }

    @Test
    fun `startPhase with partial seeding keeps top seeds fixed and randomizes remaining opponents`() {
        val players = (1..8).toList()
        val seeded = setOf(1, 2)

        val matches = KnockoutService.startPhase(
            playerIds = players,
            qualifiers = 1,
            thirdPlacePlayoff = false,
            seedingStrategy = SeedingStrategy.PARTIAL_SEEDED,
            seededPlayerCount = 2
        )

        val round1Matches = matches.filter { it.round == 1 }
        val seededMatches = round1Matches.filter { it.player1Id in seeded || it.player2Id in seeded }
        assertEquals(2, seededMatches.size)
        seededMatches.forEach { match ->
            val bothSeeded = match.player1Id in seeded && match.player2Id in seeded
            assertTrue(!bothSeeded, "Seeded players should not face each other in round 1 when unseeded opponents exist")
        }

        val assignedPlayers = round1Matches
            .flatMap { listOfNotNull(it.player1Id, it.player2Id) }
            .toSet()
        assertEquals(players.toSet(), assignedPlayers)
    }

    @Test
    fun `startPhase with partial seeding assigns byes to top seeds first`() {
        val players = listOf(1, 2, 3, 4, 5)

        val matches = KnockoutService.startPhase(
            playerIds = players,
            qualifiers = 1,
            thirdPlacePlayoff = false,
            seedingStrategy = SeedingStrategy.PARTIAL_SEEDED,
            seededPlayerCount = 2
        )

        val round1Matches = matches.filter { it.round == 1 }
        val byeMatches = round1Matches.filter { it.player2Id == null }
        val byeRecipients = byeMatches.mapNotNull { it.player1Id }.toSet()

        assertEquals(3, byeMatches.size)
        assertTrue(1 in byeRecipients)
        assertTrue(2 in byeRecipients)
    }

    @Test
    fun `startPhase with partial seeding rejects missing seeded count`() {
        val players = (1..8).toList()

        val exception = assertThrows<IllegalArgumentException> {
            KnockoutService.startPhase(
                playerIds = players,
                qualifiers = 1,
                thirdPlacePlayoff = false,
                seedingStrategy = SeedingStrategy.PARTIAL_SEEDED
            )
        }

        assertTrue(exception.message!!.contains("seededPlayerCount"))
    }

    @Test
    fun `startPhase rejects seeded count when strategy is not partial`() {
        val players = (1..8).toList()

        val exception = assertThrows<IllegalArgumentException> {
            KnockoutService.startPhase(
                playerIds = players,
                qualifiers = 1,
                thirdPlacePlayoff = false,
                seedingStrategy = SeedingStrategy.INPUT_ORDER,
                seededPlayerCount = 2
            )
        }

        assertTrue(exception.message!!.contains("seededPlayerCount is only supported"))
    }

    @Test
    fun `startPhase with participant seeds supports partial seeding without seeded count`() {
        val participants = listOf(
            SeededParticipant(1, seed = 1),
            SeededParticipant(2, seed = 2),
            SeededParticipant(3, seed = null),
            SeededParticipant(4, seed = null),
            SeededParticipant(5, seed = null),
            SeededParticipant(6, seed = null),
            SeededParticipant(7, seed = null),
            SeededParticipant(8, seed = null)
        )
        val seeded = setOf(1, 2)

        val matches = KnockoutService.startPhase(
            participants = participants,
            qualifiers = 1,
            thirdPlacePlayoff = false,
            seedingStrategy = SeedingStrategy.PARTIAL_SEEDED
        )

        val round1Matches = matches.filter { it.round == 1 }
        val seededMatches = round1Matches.filter { it.player1Id in seeded || it.player2Id in seeded }
        assertEquals(2, seededMatches.size)
        seededMatches.forEach { match ->
            val bothSeeded = match.player1Id in seeded && match.player2Id in seeded
            assertTrue(!bothSeeded)
        }
    }

    @Test
    fun `startPhase with participant seeds rejects duplicate seeds`() {
        val participants = listOf(
            SeededParticipant(1, seed = 1),
            SeededParticipant(2, seed = 1),
            SeededParticipant(3, seed = null),
            SeededParticipant(4, seed = null)
        )

        val exception = assertThrows<IllegalArgumentException> {
            KnockoutService.startPhase(
                participants = participants,
                qualifiers = 1,
                thirdPlacePlayoff = false,
                seedingStrategy = SeedingStrategy.PARTIAL_SEEDED
            )
        }

        assertTrue(exception.message!!.contains("Duplicate seed values"))
    }

    @Test
    fun `startPhase with participant seeds rejects partial strategy with no seeded participants`() {
        val participants = listOf(
            SeededParticipant(1, seed = null),
            SeededParticipant(2, seed = null),
            SeededParticipant(3, seed = null),
            SeededParticipant(4, seed = null)
        )

        val exception = assertThrows<IllegalArgumentException> {
            KnockoutService.startPhase(
                participants = participants,
                qualifiers = 1,
                thirdPlacePlayoff = false,
                seedingStrategy = SeedingStrategy.PARTIAL_SEEDED
            )
        }

        assertTrue(exception.message!!.contains("at least one seeded participant"))
    }

    @Test
    fun `startPhase with qualifiers generates only required rounds`() {
        val players = listOf(1, 2, 3, 4, 5, 6, 7, 8)
        val matches = KnockoutService.startPhase(players, qualifiers = 4)

        assertEquals(4, matches.count { it.round == 1 }, "Round 1 should have 4 matches")
        assertEquals(0, matches.count { it.round == 2 }, "Round 2 should not be generated when qualifiers is 4")
        assertEquals(4, matches.size, "Total matches should equal only round 1")
    }

    @Test
    fun `startPhase returns empty list when qualifiers equals player count`() {
        val players = listOf(1, 2, 3, 4, 5, 6, 7, 8)
        val matches = KnockoutService.startPhase(players, qualifiers = 8)

        assertTrue(matches.isEmpty(), "No matches should be generated when all players qualify")
    }

    @Test
    fun `startPhase with third place creates extra match with loser dependencies`() {
        val players = listOf(1, 2, 3, 4)
        val matches = KnockoutService.startPhase(players, qualifiers = 1, thirdPlacePlayoff = true)

        assertEquals(4, matches.size, "Should include 3 standard matches plus third-place match")

        val finalRound = matches.maxOf { it.round }
        val finalRoundMatches = matches.filter { it.round == finalRound }
        assertEquals(2, finalRoundMatches.size, "Final round should have final and third-place match")

        val thirdPlace = finalRoundMatches.firstOrNull { match ->
            match.dependencies.all { it.requiredOutcome == Outcome.LOSER }
        }
        assertTrue(thirdPlace != null, "Third-place match should depend on losers")
    }

    @Test
    fun `startPhase with third place rejects qualifiers greater than 1`() {
        val players = listOf(1, 2, 3, 4, 5, 6, 7, 8)

        val exception = assertThrows<IllegalArgumentException> {
            KnockoutService.startPhase(players, qualifiers = 4, thirdPlacePlayoff = true)
        }

        assertTrue(exception.message!!.contains("Third-place playoff requires qualifiers to be 1"))
    }

    @Test
    fun `startPhase with maximum possible byes does not create matches with both players as bye`() {
        val players = (1..9).toList()
        val matches = KnockoutService.startPhase(players)

        val round1Matches = matches.filter { it.round == 1 }
        assertTrue(round1Matches.isNotEmpty(), "No matches generated for round 1")

        round1Matches.forEach { match ->
            val bothByes = match.player1Id == null && match.player2Id == null
            assertTrue(!bothByes, "Match ${match.id} in round 1 has two byes (null vs null)")
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
