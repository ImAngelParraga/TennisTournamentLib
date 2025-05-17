package parraga.bros.tournament.services

import parraga.bros.tournament.domain.Match
import parraga.bros.tournament.domain.MatchStatus

object GroupService : PhaseService {
    override fun startPhase(playerIds: List<Int>): List<Match> {
        require(playerIds.size >= 2) { "Tournament must have at least 2 players" }

        // Determine group size and number of groups
        val playersPerGroup = determinePlayersPerGroup(playerIds.size)
        val numGroups = (playerIds.size + playersPerGroup - 1) / playersPerGroup

        val allMatches = mutableListOf<Match>()
        var matchId = 0

        // Distribute players into groups
        val shuffledPlayers = playerIds.shuffled()
        val groups = shuffledPlayers.chunked(playersPerGroup)

        // For each group, generate round-robin matches
        groups.forEachIndexed { groupIndex, groupPlayers ->
            val groupId = groupIndex + 1
            val groupMatches = createRoundRobinMatches(
                groupPlayers,
                groupId,
                matchId
            )
            matchId += groupMatches.size
            allMatches.addAll(groupMatches)
        }

        return allMatches
    }

    override fun startNextRound(
        nextRoundMatches: List<Match>,
        previousRoundMatches: List<Match>
    ): List<Match> {
        // In group format, all matches are generated at the beginning
        // so there's no need to create new matches for next round
        // This function simply returns the matches that should be started next
        return nextRoundMatches
    }

    private fun createRoundRobinMatches(
        players: List<Int>,
        groupId: Int,
        startMatchId: Int
    ): List<Match> {
        val matches = mutableListOf<Match>()
        var currentMatchId = startMatchId

        // For round-robin, each player plays against all other players once
        for (i in players.indices) {
            for (j in (i + 1) until players.size) {
                matches.add(
                    Match(
                        id = currentMatchId++,
                        round = 1, // All matches in the same round for round-robin
                        groupId = groupId,
                        player1Id = players[i],
                        player2Id = players[j],
                        status = MatchStatus.SCHEDULED,
                        winnerId = null,
                        dependencies = emptyList()
                    )
                )
            }
        }

        return matches
    }

    private fun determinePlayersPerGroup(totalPlayers: Int): Int {
        // Common group sizes are 3, 4, 5, 6 players
        return when {
            totalPlayers <= 6 -> totalPlayers // One group if 6 or fewer players
            totalPlayers <= 12 -> if (totalPlayers % 2 == 0) 4 else 3 // Prefer groups of 4
            totalPlayers <= 24 -> if (totalPlayers % 3 == 0) 3 else 4 // Try to balance
            else -> 4 // Default to groups of 4 for larger tournaments
        }
    }
}