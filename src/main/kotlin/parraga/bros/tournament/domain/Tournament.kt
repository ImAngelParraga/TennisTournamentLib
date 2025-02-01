package parraga.bros.tournament.domain

import sun.jvm.hotspot.opto.Phase

data class Tournament(
    val players: List<Player>,
    val phases: List<Phase>
)
