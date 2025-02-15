package parraga.bros.tournament.services

import parraga.bros.tournament.domain.Match

interface PhaseService {
    fun startPhase(playerIds: List<Int>): List<Match>
    fun startNextRound(nextRoundMatches: List<Match>, previousRoundMatches: List<Match>): List<Match>
}