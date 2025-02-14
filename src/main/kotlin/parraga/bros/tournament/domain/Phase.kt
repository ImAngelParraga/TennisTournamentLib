package parraga.bros.tournament.domain

data class Phase(
    val order: Int,
    val format: Format,
    val rounds: Int,
    val configuration: PhaseConfiguration,
    val matches: List<Match>
)

enum class Format { KNOCKOUT, GROUP, SWISS }

sealed interface PhaseConfiguration {
    data class KnockoutConfig(
        val thirdPlacePlayoff: Boolean
    ) : PhaseConfiguration
    
    data class GroupConfig(
        val groupCount: Int,
        val teamsPerGroup: Int,
        val advancingPerGroup: Int
    ) : PhaseConfiguration

    data class SwissConfig(
        val pointsPerWin: Int = 1
    ) : PhaseConfiguration
}