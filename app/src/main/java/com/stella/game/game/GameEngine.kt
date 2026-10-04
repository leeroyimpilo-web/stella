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

    var notification by mutableStateOf<String?>(null)
        private set

    val currentScene: Scene
        get() = ChapterOne.scenes[state.sceneId] ?: ChapterOne.scenes.getValue("awakening_01")

    fun hasSave(): Boolean = prefs.getBoolean("has_save", false)

    fun newGame() {
        state = GameState(playStarted = true)
        notification = null
        save()
    }

    fun continueGame() {
        state = loadState().copy(playStarted = true)
        notification = null
    }

    fun canChoose(choice: Choice): Boolean =
        state.flags.containsAll(choice.requiredFlags) &&
            choice.requiredItems.all { state.hasItem(it) }

    fun missingItemNames(choice: Choice): List<String> =
        choice.requiredItems
            .filterNot { state.hasItem(it) }
            .map { ItemCatalog.get(it)?.name ?: it }

    fun choose(choice: Choice) {
        if (!canChoose(choice)) return

        val before = state.inventory
        var nextInventory = before

        choice.removeItems.forEach { id ->
            val index = nextInventory.indexOf(id)
            if (index >= 0) nextInventory = nextInventory.toMutableList().also { it.removeAt(index) }
        }

        choice.addItems.forEach { id ->
            val definition = ItemCatalog.get(id)
            if (definition?.stackable == true || !nextInventory.contains(id)) {
                nextInventory = nextInventory + id
            }
        }

        val gained = nextInventory.filterIndexed { index, item ->
            index >= before.size || before.count { it == item } < nextInventory.take(index + 1).count { it == item }
        }.firstOrNull { item -> before.count { it == item } < nextInventory.count { it == item } }

        val newCodex = choice.addCodex - state.codex

        state = state.copy(
            sceneId = choice.targetSceneId,
            power = state.power + choice.powerDelta,
            oxygen = state.oxygen + choice.oxygenDelta,
            hull = state.hull + choice.hullDelta,
            health = state.health + choice.healthDelta,
            stellaTrust = state.stellaTrust + choice.trustDelta,
            flags = state.flags + choice.addFlags,
            inventory = nextInventory,
            codex = state.codex + choice.addCodex,
            history = (state.history + choice.id).takeLast(120),
            playStarted = true
        ).clamp()

        notification = when {
            gained != null -> "ITEM ACQUIRED // " + (ItemCatalog.get(gained)?.name?.uppercase() ?: gained.uppercase())
            newCodex.isNotEmpty() -> "CODEX UPDATED // " + (CodexCatalog.all[newCodex.first()]?.title ?: newCodex.first())
            else -> null
        }
        save()
    }

    fun useItem(itemId: String) {
        if (!state.hasItem(itemId)) return

        when (itemId) {
            "oxygen_canister" -> {
                state = state.copy(
                    oxygen = state.oxygen + 25,
                    inventory = removeOne(state.inventory, itemId)
                ).clamp()
                notification = "OXYGEN RESTORED // +25%"
            }
            "med_injector" -> {
                state = state.copy(
                    health = state.health + 30,
                    inventory = removeOne(state.inventory, itemId)
                ).clamp()
                notification = "BIOLOGICAL CONDITION // +30%"
            }
            "scanner" -> {
                state = state.copy(flags = state.flags + "scanner_active")
                notification = "SCANNER ACTIVE // CONTEXTUAL OPTIONS ENABLED"
            }
            "stella_memory_fragment" -> {
                if (!state.flags.contains("memory_fragment_examined")) {
                    state = state.copy(
                        flags = state.flags + "memory_fragment_examined",
                        stellaTrust = state.stellaTrust + 3,
                        codex = state.codex + "memory_fragment"
                    ).clamp()
                    notification = "MEMORY DECRYPTION // FRAGMENT 01"
                } else {
                    notification = "MEMORY FRAGMENT // ALREADY DECRYPTED"
                }
            }
            else -> {
                notification = "KEY ITEM // USED AUTOMATICALLY WHEN REQUIRED"
            }
        }
        save()
    }

    fun clearNotification() {
        notification = null
    }

    fun returnToMenu() {
        state = state.copy(playStarted = false)
    }

    private fun removeOne(items: List<String>, id: String): List<String> {
        val index = items.indexOf(id)
        if (index < 0) return items
        return items.toMutableList().also { it.removeAt(index) }
    }

    private fun save() {
        prefs.edit()
            .putBoolean("has_save", true)
            .putString("scene_id", state.sceneId)
            .putInt("power", state.power)
            .putInt("oxygen", state.oxygen)
            .putInt("hull", state.hull)
            .putInt("health", state.health)
            .putInt("trust", state.stellaTrust)
            .putString("flags", state.flags.joinToString("|"))
            .putString("inventory", state.inventory.joinToString("|"))
            .putString("codex", state.codex.joinToString("|"))
            .putString("history", state.history.joinToString("|"))
            .apply()
    }

    private fun loadState(): GameState {
        if (!prefs.getBoolean("has_save", false)) return GameState()

        fun decodeSet(key: String): Set<String> =
            prefs.getString(key, "")
                .orEmpty()
                .split("|")
                .filter { it.isNotBlank() }
                .toSet()

        fun decodeList(key: String): List<String> =
            prefs.getString(key, "")
                .orEmpty()
                .split("|")
                .filter { it.isNotBlank() }

        return GameState(
            sceneId = prefs.getString("scene_id", "awakening_01") ?: "awakening_01",
            power = prefs.getInt("power", 74),
            oxygen = prefs.getInt("oxygen", 91),
            hull = prefs.getInt("hull", 64),
            health = prefs.getInt("health", 100),
            stellaTrust = prefs.getInt("trust", 50),
            flags = decodeSet("flags"),
            inventory = decodeList("inventory"),
            codex = decodeSet("codex").ifEmpty { setOf("eidolon") },
            history = decodeList("history"),
            playStarted = false
        ).clamp()
    }
}
