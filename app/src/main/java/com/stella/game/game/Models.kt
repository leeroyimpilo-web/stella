package com.stella.game.game

enum class Speaker { NARRATOR, STELLA, SYSTEM, PLAYER, UNKNOWN }
enum class TransitionStyle { FADE, GLITCH, HARD_CUT, BLACKOUT }
enum class HapticCue { NONE, TAP, IMPACT, HEARTBEAT, ALARM }

data class StoryLine(
    val speaker: Speaker = Speaker.NARRATOR,
    val text: String,
    val delayBeforeMs: Long = 0L
)

data class Choice(
    val id: String,
    val text: String,
    val targetSceneId: String,
    val trustDelta: Int = 0,
    val powerDelta: Int = 0,
    val oxygenDelta: Int = 0,
    val hullDelta: Int = 0,
    val addFlags: Set<String> = emptySet(),
    val requiredFlags: Set<String> = emptySet()
)

data class SceneDirection(
    val artworkKey: String,
    val location: String,
    val ambience: String = "ship_hum",
    val transition: TransitionStyle = TransitionStyle.FADE,
    val haptic: HapticCue = HapticCue.NONE,
    val danger: Boolean = false,
    val cameraMove: String = "slow_push"
)

data class Scene(
    val id: String,
    val chapter: Int,
    val title: String,
    val direction: SceneDirection,
    val lines: List<StoryLine>,
    val choices: List<Choice>
)

data class GameState(
    val sceneId: String = "awakening_01",
    val power: Int = 74,
    val oxygen: Int = 91,
    val hull: Int = 64,
    val stellaTrust: Int = 50,
    val flags: Set<String> = emptySet(),
    val history: List<String> = emptyList(),
    val playStarted: Boolean = false
) {
    fun clamp(): GameState = copy(
        power = power.coerceIn(0, 100),
        oxygen = oxygen.coerceIn(0, 100),
        hull = hull.coerceIn(0, 100),
        stellaTrust = stellaTrust.coerceIn(0, 100)
    )
}
