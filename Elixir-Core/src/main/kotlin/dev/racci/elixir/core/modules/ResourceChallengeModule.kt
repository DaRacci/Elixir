package dev.racci.elixir.core.modules

import dev.racci.elixir.core.data.ElixirConfig

public object ResourceChallengeModule : ModuleActor<ElixirConfig.Modules.ResourceChallenge>() {
    override suspend fun load() {
        // TODO: Load challenges from database
        // TODO: If there are no challenges, create a new one
        // TODO: If last challenge is expired, create a new one
        // TODO: If creating new challenge make broadcast and automated discord ping
        // TODO: Sorted leaderboard for the active challenge
        // TODO: Leaderboard for all time challenges (maybe?)
        // TODO: Viewable challenge history
    }
}
