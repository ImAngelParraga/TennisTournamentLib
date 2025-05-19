package parraga.bros.tournament.services

import parraga.bros.tournament.domain.Match
import parraga.bros.tournament.domain.MatchStatus
import parraga.bros.tournament.domain.Phase
import parraga.bros.tournament.domain.PhaseConfiguration

object GroupService : PhaseService {
    override fun startPhase(phase: Phase, playerIds: List<Int>): List<Match> {
        require(playerIds.size >= 2) { "Tournament must have at least 2 players" }

        // Extract configuration as GroupConfig
        val config = phase.configuration as? PhaseConfiguration.GroupConfig
            ?: throw IllegalArgumentException("Phase configuration must be GroupConfig for group format")

        // Determine group size and number of groups based on configuration
        val playersPerGroup = config.playersPerGroup ?: determinePlayersPerGroup(playerIds.size)
        val numGroups = config.groupCount ?: ((playerIds.size + playersPerGroup - 1) / playersPerGroup)

        val allMatches = mutableListOf<Match>()
        var matchId = 0

        // Distribute players into groups
        val shuffledPlayers = playerIds.shuffled()

        // Ensure we create the specified number of groups
        val groups = if (config.groupCount != null) {
            // If group count is specified, distribute players as evenly as possible
            val playersPerGroupList = distributePlayersEvenly(shuffledPlayers.size, numGroups)
            var startIndex = 0
            playersPerGroupList.map { count ->
                val group = shuffledPlayers.subList(startIndex, startIndex + count)
                startIndex += count
                group
            }
        } else {
            // Otherwise just chunk them evenly
            shuffledPlayers.chunked(playersPerGroup)
        }

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

    private fun distributePlayersEvenly(totalPlayers: Int, groupCount: Int): List<Int> {
        val basePlayersPerGroup = totalPlayers / groupCount
        val remainder = totalPlayers % groupCount

        return List(groupCount) { i ->
            basePlayersPerGroup + if (i < remainder) 1 else 0
        }
    }

    private fun createRoundRobinMatches(
        players: List<Int>,
        groupId: Int,
        startMatchId: Int
    ): List<Match> {
        val matches = mutableListOf<Match>()
        var currentMatchId = startMatchId

        // For odd number of players, add a "bye" player (null)
        var schedulePlayers = if (players.size % 2 == 1)
            players + listOf(null)
        else
            players

        val n = schedulePlayers.size

        // Round-robin tournament requires (n-1) rounds where n is number of players
        val rounds = n - 1

        // Create schedule using circle method
        for (round in 0 until rounds) {
            // In each round, pair players accordingly
            for (i in 0 until n / 2) {
                val player1Index = i
                val player2Index = n - 1 - i

                val player1 = schedulePlayers[player1Index]
                val player2 = schedulePlayers[player2Index]

                // Skip matches with the "bye" player (null)
                if (player1 != null && player2 != null) {
                    matches.add(
                        Match(
                            id = currentMatchId++,
                            round = round + 1, // Round numbers start at 1
                            groupId = groupId,
                            player1Id = player1,
                            player2Id = player2,
                            status = MatchStatus.SCHEDULED,
                            winnerId = null,
                            dependencies = emptyList()
                        )
                    )
                }
            }

            // Rotate players for next round (keep first player fixed, rotate others)
            if (round < rounds - 1) {
                val rotated = schedulePlayers.toMutableList()
                val firstPlayer = rotated.removeAt(0)
                rotated.add(1, rotated.removeAt(rotated.lastIndex))
                rotated.add(0, firstPlayer)
                schedulePlayers = rotated
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