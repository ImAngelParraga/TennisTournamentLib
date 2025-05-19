package parraga.bros.tournament.services

import parraga.bros.tournament.domain.Match
import parraga.bros.tournament.domain.Phase

interface PhaseService {
    fun startPhase(phase: Phase, playerIds: List<Int>): List<Match>

    fun startNextRound(
        nextRoundMatches: List<Match>,
        previousRoundMatches: List<Match>
    ): List<Match>
}