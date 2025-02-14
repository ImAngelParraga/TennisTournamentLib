package parraga.bros.tournament.services

import parraga.bros.tournament.domain.Format
import parraga.bros.tournament.domain.Match
import parraga.bros.tournament.domain.Phase

object TournamentService {

    fun startPhase(phase: Phase, playerIds: List<Int>) = when (phase.format) {
        Format.GROUP -> GroupService.startPhase(playerIds)
        Format.SWISS -> SwissService.startPhase(playerIds)
        Format.KNOCKOUT -> KnockoutService.startPhase(playerIds)
    }

    // TODO: obtain previous round matches. Should I check last round finished? Should I ask for round number?
    // TODO: obtain next round matches. Should this logic be inside phase service? I don't think so.
    fun startNextRound(phase: Phase): List<Match> = when (phase.format) {
        Format.GROUP -> GroupService.startNextRound()
        Format.SWISS -> SwissService.startNextRound()
        Format.KNOCKOUT -> KnockoutService.startNextRound()
    }

}