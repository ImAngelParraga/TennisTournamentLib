package parraga.bros.tournament.services

import parraga.bros.tournament.domain.Format
import parraga.bros.tournament.domain.Match
import parraga.bros.tournament.domain.MatchStatus
import parraga.bros.tournament.domain.Phase
import parraga.bros.tournament.domain.PhaseConfiguration
import parraga.bros.tournament.domain.SeededParticipant

object TournamentService {

    fun startPhase(phase: Phase, playerIds: List<Int>) = when (phase.format) {
        Format.GROUP -> GroupService.startPhase(phase, playerIds.map { SeededParticipant(it) })
        Format.SWISS -> SwissService.startPhase(phase, playerIds.map { SeededParticipant(it) })
        Format.KNOCKOUT -> {
            val config = phase.configuration as? PhaseConfiguration.KnockoutConfig
                ?: throw IllegalArgumentException("Knockout phase requires KnockoutConfig configuration")
            KnockoutService.startPhase(
                playerIds = playerIds,
                qualifiers = config.qualifiers,
                thirdPlacePlayoff = config.thirdPlacePlayoff,
                seedingStrategy = config.seedingStrategy,
                seededPlayerCount = config.seededPlayerCount
            )
        }
    }

    fun startPhaseWithParticipants(phase: Phase, participants: List<SeededParticipant>) = when (phase.format) {
        Format.GROUP -> GroupService.startPhase(phase, participants)
        Format.SWISS -> SwissService.startPhase(phase, participants)
        Format.KNOCKOUT -> {
            val config = phase.configuration as? PhaseConfiguration.KnockoutConfig
                ?: throw IllegalArgumentException("Knockout phase requires KnockoutConfig configuration")
            KnockoutService.startPhase(
                participants = participants,
                qualifiers = config.qualifiers,
                thirdPlacePlayoff = config.thirdPlacePlayoff,
                seedingStrategy = config.seedingStrategy
            )
        }
    }

    fun startNextRound(phase: Phase): List<Match> {
        return when (phase.format) {
            Format.GROUP -> GroupService.startNextRound(phase)
            Format.SWISS -> SwissService.startNextRound(phase)
            Format.KNOCKOUT -> {
                val allMatches = phase.matches
                val lastCompletedRound = getLastCompletedRound(allMatches)
                val previousRoundMatches = allMatches.filter { it.round == lastCompletedRound }
                assertMatchesForRoundAreCompleted(previousRoundMatches, lastCompletedRound)

                val nextRound = lastCompletedRound + 1
                val nextRoundMatches = allMatches.filter { it.round == nextRound }
                assertRoundMatchesAreNotEmpty(nextRoundMatches, nextRound)

                KnockoutService.startNextRound(nextRoundMatches, previousRoundMatches)
            }
        }
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
