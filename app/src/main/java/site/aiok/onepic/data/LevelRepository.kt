package site.aiok.onepic.data

import site.aiok.onepic.model.LevelConfig

object LevelRepository {
    val levels = listOf(
        LevelConfig(
            levelId = 1,
            title = "Starter",
            difficulty = "Easy",
            imageResId = 0, // 0 means generated
            rows = 3,
            cols = 3
        ),
        LevelConfig(
            levelId = 2,
            title = "Warm Up",
            difficulty = "Easy",
            imageResId = 0,
            rows = 3,
            cols = 4
        ),
        LevelConfig(
            levelId = 3,
            title = "Challenge",
            difficulty = "Medium",
            imageResId = 0,
            rows = 4,
            cols = 5
        ),
        LevelConfig(
            levelId = 4,
            title = "Master",
            difficulty = "Hard",
            imageResId = 0,
            rows = 5,
            cols = 6
        ),
        LevelConfig(
            levelId = 5,
            title = "Grandmaster",
            difficulty = "Expert",
            imageResId = 0,
            rows = 6,
            cols = 8
        )
    )

    fun getLevel(id: Int): LevelConfig? {
        return levels.find { it.levelId == id }
    }
}
