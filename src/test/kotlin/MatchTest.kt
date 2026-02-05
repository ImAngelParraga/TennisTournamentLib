package parraga.bros.tournament.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MatchTest {

    @Test
    fun `applyScore sets score winner and completed status`() {
        val match = Match(
            id = 1,
            round = 1,
            player1Id = 1,
            player2Id = 2,
            status = MatchStatus.SCHEDULED
        )
        val score = TennisScore(
            sets = listOf(
                SetScore(6, 4, null),
                SetScore(6, 3, null)
            )
        )

        match.applyScore(score)

        assertEquals(score, match.score)
        assertEquals(1, match.winnerId)
        assertEquals(MatchStatus.COMPLETED, match.status)
    }
}
