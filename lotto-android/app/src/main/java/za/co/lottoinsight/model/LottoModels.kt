package za.co.lottoinsight.model

import java.security.SecureRandom

data class LottoGame(
    val id: String,
    val name: String,
    val pickCount: Int,
    val maxNumber: Int,
    val powerBallMax: Int = 0
)

val games = listOf(
    LottoGame("lotto", "Lotto", 6, 52),
    LottoGame("lotto-plus-1", "Lotto Plus 1", 6, 52),
    LottoGame("lotto-5-max", "Lotto 5 Max", 6, 52),
    LottoGame("powerball", "PowerBall", 5, 50, 16),
    LottoGame("powerball-plus", "PowerBall XTRA", 5, 50, 16),
    LottoGame("daily-lotto", "Daily Lotto", 5, 36)
)

data class Draw(
    val game: String,
    val date: String,
    val numbers: List<Int>,
    val bonus: Int?,
    val verified: Boolean
)

data class Ticket(
    val id: Long = 0,
    val game: String,
    val numbers: List<Int>,
    val powerBall: Int? = null,
    val created: String = ""
)

object NumberGenerator {
    private val secureRandom = SecureRandom()

    fun generate(game: LottoGame, count: Int, spread: Boolean): List<Ticket> {
        val result = linkedMapOf<String, Ticket>()
        var attempts = 0
        while (result.size < count && attempts < count * 100) {
            attempts++
            val selected = if (spread) spreadNumbers(game) else randomNumbers(game)
            val power = if (game.powerBallMax > 0) secureRandom.nextInt(game.powerBallMax) + 1 else null
            val ticket = Ticket(game = game.id, numbers = selected, powerBall = power)
            result[selected.joinToString("-") + "-" + (power ?: "")] = ticket
        }
        return result.values.toList()
    }

    private fun randomNumbers(game: LottoGame): List<Int> {
        val chosen = mutableSetOf<Int>()
        while (chosen.size < game.pickCount) {
            chosen += secureRandom.nextInt(game.maxNumber) + 1
        }
        return chosen.sorted()
    }

    private fun spreadNumbers(game: LottoGame): List<Int> {
        val result = mutableSetOf<Int>()
        for (slot in 0 until game.pickCount) {
            val low = slot * game.maxNumber / game.pickCount + 1
            val high = (slot + 1) * game.maxNumber / game.pickCount
            result += low + secureRandom.nextInt(high - low + 1)
        }
        return result.sorted()
    }
}
