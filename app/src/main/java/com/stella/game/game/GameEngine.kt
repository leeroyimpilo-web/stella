package com.stella.game.game

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.stella.game.story.ChapterOne

class GameEngine(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("stella_save", Context.MODE_PRIVATE)

    var state by mutableStateOf(loadState())
        private set

    val currentScene: Scene
        get() = ChapterOne.scenes[state.sceneId] ?: ChapterOne.scenes.getValue("awakening_01")

    fun hasSave(): Boolean = prefs.getBoolean("has_save", false)
    fun newGame() { state = GameState(playStarted = true); save() }
    fun continueGame() { state = loadState().copy(playStarted = true) }

    fun choose(choice: Choice) {
        if (!state.flags.containsAll(choice.requiredFlags)) return
        state = state.copy(
            sceneId = choice.targetSceneId,
            power = state.power + choice.powerDelta,
            oxygen = state.oxygen + choice.oxygenDelta,
            hull = state.hull + choice.hullDelta,
            stellaTrust = state.stellaTrust + choice.trustDelta,
            flags = state.flags + choice.addFlags,
            history = (state.history + choice.id).takeLast(80),
            playStarted = true
        ).clamp()
        save()
    }

    fun returnToMenu() { state = state.copy(playStarted = false) }

    private fun save() {
        prefs.edit()
            .putBoolean("has_save", true)
            .putString("scene_id", state.sceneId)
            .putInt("power", state.power)
            .putInt("oxygen", state.oxygen)
            .putInt("hull", state.hull)
            .putInt("trust", state.stellaTrust)
            .putString("flags", state.flags.joinToString("|"))
            .putString("history", state.history.joinToString("|"))
            .apply()
    }

    private fun loadState(): GameState {
        if (!prefs.getBoolean("has_save", false)) return GameState()
        fun set(key: String) = prefs.getString(key, "").orEmpty().split("|").filter { it.isNotBlank() }.toSet()
        fun list(key: String) = prefs.getString(key, "").orEmpty().split("|").filter { it.isNotBlank() }
        return GameState(
            sceneId = prefs.getString("scene_id", "awakening_01") ?: "awakening_01",
            power = prefs.getInt("power", 74),
            oxygen = prefs.getInt("oxygen", 91),
            hull = prefs.getInt("hull", 64),
            stellaTrust = prefs.getInt("trust", 50),
            flags = set("flags"),
            history = list("history"),
            playStarted = false
        ).clamp()
    }
}
