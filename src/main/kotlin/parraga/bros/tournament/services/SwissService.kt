package parraga.bros.tournament.services

import parraga.bros.tournament.domain.Match

object SwissService : PhaseService {
    override fun startPhase(playerIds: List<Int>): List<Match> {
        TODO()
    }

    override fun startNextRound(
        nextRoundMatches: List<Match>,
        previousRoundMatches: List<Match>
    ): List<Match> {
        TODO("Not yet implemented")
    }
}