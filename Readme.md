# Tournament Management System

A Kotlin library for managing different tournament formats including knockout, group, and Swiss systems.

## Features

- Multiple tournament formats supported:
    - **Knockout**: Single or double elimination tournaments
    - **Group**: Round-robin competitions with configurable group sizes
    - **Swiss**: Points-based pairing system (in progress)

- Match management with:
    - Status tracking (scheduled, live, completed, walkover)
    - Dependencies between matches
    - Tennis scoring support

## Core Components

### Phase Types

The system supports three tournament formats:

```kotlin
enum class Format { KNOCKOUT, GROUP, SWISS }
```

Each format has specific configuration options:

- **Knockout**: Optional third-place playoff
- **Group**: Configurable group count, teams per group, and advancing teams
- **Swiss**: Customizable point system

### Services

- `PhaseService`: Interface defining tournament phase operations
- `KnockoutService`: Implementation for knockout tournaments
- `GroupService`: Implementation for group format tournaments
- `SwissService`: Implementation for Swiss format tournaments
- `TournamentService`: Main service handling overall tournament management

### Domain Models

- `Match`: Core entity representing a match between two players/teams
- `MatchDependency`: Defines how matches relate to each other
- `Phase`: Contains format and configuration information