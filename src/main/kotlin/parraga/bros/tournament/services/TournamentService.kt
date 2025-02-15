package parraga.bros.tournament.services

import parraga.bros.tournament.domain.Format
import parraga.bros.tournament.domain.Match
import parraga.bros.tournament.domain.MatchStatus
import parraga.bros.tournament.domain.Phase

object TournamentService {

    fun startPhase(phase: Phase, playerIds: List<Int>) = when (phase.format) {
        Format.GROUP -> GroupService.startPhase(playerIds)
        Format.SWISS -> SwissService.startPhase(playerIds)
        Format.KNOCKOUT -> KnockoutService.startPhase(playerIds)
    }

    fun startNextRound(phase: Phase): List<Match> {
        val allMatches = phase.matches

        val lastCompletedRound = getLastCompletedRound(allMatches)

        val previousRoundMatches = allMatches.filter { it.round == lastCompletedRound }
        assertMatchesForRoundAreCompleted(previousRoundMatches, lastCompletedRound)

        val nextRound = lastCompletedRound + 1
        val nextRoundMatches = allMatches.filter { it.round == nextRound }
        assertRoundMatchesAreNotEmpty(nextRoundMatches, nextRound)

        val phaseService = when (phase.format) {
            Format.KNOCKOUT -> KnockoutService
            Format.GROUP -> GroupService
            Format.SWISS -> SwissService
        }

        return phaseService.startNextRound(nextRoundMatches, previousRoundMatches)
    }

    private fun getLastCompletedRound(matches: List<Match>) =
        matches.filter { it.status == MatchStatus.COMPLETED && it.winnerId != null }
            .maxOfOrNull { it.round }
            ?: throw IllegalStateException("No completed rounds available")

    private fun assertMatchesForRoundAreCompleted(matches: List<Match>, round: Int) {
        if (matches.any { it.status != MatchStatus.COMPLETED || it.winnerId == null }) {
            throw IllegalStateException("Round $round has uncompleted matches")
        }
    }

    private fun assertRoundMatchesAreNotEmpty(roundMatches: List<Match>, round: Int) {
        if (roundMatches.isEmpty()) {
            throw IllegalStateException("Next round ($round) matches not generated")
        }
    }
}