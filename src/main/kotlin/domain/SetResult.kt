package domain

sealed class SetResult {
    data class RegularSet(val player1Games: Int, val player2Games: Int) : SetResult()
    data class MatchTiebreak(val player1Points: Int, val player2Points: Int) : SetResult()
}
