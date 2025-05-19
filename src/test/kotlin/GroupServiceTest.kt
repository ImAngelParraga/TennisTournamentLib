package parraga.bros.tournament.services

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import parraga.bros.tournament.domain.Format
import parraga.bros.tournament.domain.Match
import parraga.bros.tournament.domain.MatchStatus
import parraga.bros.tournament.domain.Phase
import parraga.bros.tournament.domain.PhaseConfiguration

class GroupServiceTest {

    @Nested
    @DisplayName("StartPhase Tests")
    inner class StartPhaseTests {
        @Test
        fun `should throw exception when player count is less than 2`() {
            // Given
            val phase = createPhase()
            val playerIds = listOf(1)

            // When, Then
            assertThrows<IllegalArgumentException> {
                GroupService.startPhase(phase, playerIds)
            }
        }

        @Test
        fun `should throw exception when configuration is not GroupConfig`() {
            // Given
            val phase = Phase(
                order = 1,
                format = Format.GROUP,
                rounds = 1,
                configuration = PhaseConfiguration.KnockoutConfig(thirdPlacePlayoff = false),
                matches = emptyList()
            )
            val playerIds = listOf(1, 2, 3, 4)

            // When, Then
            assertThrows<IllegalArgumentException> {
                GroupService.startPhase(phase, playerIds)
            }
        }

        @Test
        fun `should create one group with all players when total players is 6 or less`() {
            // Given
            val phase = createPhase()
            val playerIds = listOf(1, 2, 3, 4, 5)

            // When
            val matches = GroupService.startPhase(phase, playerIds)

            // Then
            assertEquals(10, matches.size) // 5 players means 10 matches (n*(n-1)/2)
            assertEquals(1, matches.map { it.groupId }.distinct().size) // All matches in one group
        }

        @Test
        fun `should respect configured groupCount`() {
            // Given
            val phase = createPhase(groupCount = 3)
            val playerIds = List(12) { it + 1 }

            // When
            val matches = GroupService.startPhase(phase, playerIds)

            // Then
            assertEquals(3, matches.map { it.groupId }.distinct().size)
            matches.groupBy { it.groupId }.forEach { (_, groupMatches) ->
                val playersInGroup = groupMatches.flatMap { listOf(it.player1Id, it.player2Id) }.distinct()
                assertEquals(4, playersInGroup.size) // 12 players / 3 groups = 4 players per group
            }
        }

        @Test
        fun `should respect configured playersPerGroup`() {
            // Given
            val phase = createPhase(playersPerGroup = 3)
            val playerIds = List(9) { it + 1 }

            // When
            val matches = GroupService.startPhase(phase, playerIds)

            // Then
            assertEquals(3, matches.map { it.groupId }.distinct().size) // 9 players / 3 per group = 3 groups
        }

        @Test
        fun `should distribute players evenly when groupCount specified`() {
            // Given
            val phase = createPhase(groupCount = 3)
            val playerIds = List(10) { it + 1 } // 10 players divided into 3 groups

            // When
            val matches = GroupService.startPhase(phase, playerIds)

            // Then
            val groups = matches.groupBy { it.groupId }
            assertEquals(3, groups.size)

            // Calculate player count per group
            val playersPerGroup = groups.mapValues { (_, groupMatches) ->
                groupMatches.flatMap { listOf(it.player1Id, it.player2Id) }.distinct().size
            }

            // First group has 4 players, other groups have 3 players each
            val expectedDistribution = mapOf(1 to 4, 2 to 3, 3 to 3)
            assertEquals(expectedDistribution, playersPerGroup)
        }

        @Test
        fun `should create correct number of matches for each group`() {
            // Given
            val phase = createPhase(groupCount = 2)
            val playerIds = List(8) { it + 1 }

            // When
            val matches = GroupService.startPhase(phase, playerIds)

            // Then
            val groupMatches = matches.groupBy { it.groupId }
            assertEquals(2, groupMatches.size)

            // Each group has 4 players, so 6 matches per group (n*(n-1)/2)
            assertEquals(6, groupMatches[1]?.size)
            assertEquals(6, groupMatches[2]?.size)
        }

        @Test
        fun `should generate matches with correct status and no winners`() {
            // Given
            val phase = createPhase()
            val playerIds = listOf(1, 2, 3, 4)

            // When
            val matches = GroupService.startPhase(phase, playerIds)

            // Then
            matches.forEach { match ->
                assertEquals(MatchStatus.SCHEDULED, match.status)
                assertNull(match.winnerId)
                assertTrue(match.dependencies.isEmpty())
            }
        }

        @Test
        fun `should distribute matches across rounds with no player playing more than once per round`() {
            // Given
            val phase = createPhase(groupCount = 2)
            val playerIds = List(8) { it + 1 } // 8 players in 2 groups

            // When
            val matches = GroupService.startPhase(phase, playerIds)

            // Then
            val matchesByGroup = matches.groupBy { it.groupId }
            assertEquals(2, matchesByGroup.size) // Confirm we have 2 groups

            matchesByGroup.forEach { (groupId, groupMatches) ->
                val roundMatches = groupMatches.groupBy { it.round }

                // Calculate players in this group
                val playersInGroup = groupMatches.flatMap { listOf(it.player1Id, it.player2Id) }.distinct()

                // For 4 players per group, there should be 3 rounds
                val expectedRounds = playersInGroup.size - 1
                assertEquals(expectedRounds, roundMatches.size,
                    "Group $groupId: Expected $expectedRounds rounds for ${playersInGroup.size} players")

                // Check that in each round, no player plays more than once
                roundMatches.forEach { (round, matchesInRound) ->
                    val playersInRound = mutableSetOf<Int>()
                    matchesInRound.forEach { match ->
                        assertFalse(playersInRound.contains(match.player1Id),
                            "Player ${match.player1Id} plays multiple times in round $round of group $groupId")
                        playersInRound.add(match.player1Id!!)

                        assertFalse(playersInRound.contains(match.player2Id),
                            "Player ${match.player2Id} plays multiple times in round $round of group $groupId")
                        playersInRound.add(match.player2Id!!)
                    }

                    // Each round should have exactly half the players playing
                    assertEquals(playersInGroup.size / 2, matchesInRound.size,
                        "Round $round should have the correct number of matches")
                }
            }
        }
    }

    @Nested
    @DisplayName("StartNextRound Tests")
    inner class StartNextRoundTests {
        @Test
        fun `should return the same matches for next round`() {
            // Given
            val nextRoundMatches = listOf(
                createMatch(1, 2, 3),
                createMatch(2, 4, 5)
            )
            val previousRoundMatches = listOf(
                createMatch(3, 6, 7, winnerId = 7, status = MatchStatus.COMPLETED),
                createMatch(4, 8, 9, winnerId = 9, status = MatchStatus.COMPLETED)
            )

            // When
            val result = GroupService.startNextRound(nextRoundMatches, previousRoundMatches)

            // Then
            assertSame(nextRoundMatches, result)
        }
    }

    // Helper methods
    private fun createPhase(
        groupCount: Int? = null,
        playersPerGroup: Int? = null,
        totalAdvancingPlayers: Int? = null
    ): Phase {
        return Phase(
            order = 1,
            format = Format.GROUP,
            rounds = 1,
            configuration = PhaseConfiguration.GroupConfig(
                groupCount = groupCount,
                playersPerGroup = playersPerGroup,
                totalAdvancingPlayers = totalAdvancingPlayers
            ),
            matches = emptyList()
        )
    }

    private fun createMatch(
        id: Int,
        player1Id: Int,
        player2Id: Int,
        round: Int = 1,
        groupId: Int = 1,
        winnerId: Int? = null,
        status: MatchStatus = MatchStatus.SCHEDULED
    ): Match {
        return Match(
            id = id,
            round = round,
            groupId = groupId,
            player1Id = player1Id,
            player2Id = player2Id,
            status = status,
            winnerId = winnerId,
            dependencies = emptyList()
        )
    }
}