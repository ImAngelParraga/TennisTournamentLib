package parraga.bros.tournament.domain

data class SeededParticipant(
    val playerId: Int,
    val seed: Int? = null
) {
    init {
        require(playerId > 0) { "playerId must be greater than 0" }
        require(seed == null || seed > 0) { "seed must be greater than 0 when provided" }
    }
}
