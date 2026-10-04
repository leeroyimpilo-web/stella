package com.stella.game.game

enum class CodexCategory { LOCATION, CREW, TECHNOLOGY, SIGNAL, EVENT, ANOMALY }

data class CodexEntry(
    val id: String,
    val title: String,
    val category: CodexCategory,
    val summary: String
)

object CodexCatalog {
    val all = listOf(
        CodexEntry("eidolon", "EIDOLON", CodexCategory.LOCATION, "Deep-space vessel carrying forty-two crew into an unknown mission window."),
        CodexEntry("crew_missing", "THE MISSING CREW", CodexCategory.EVENT, "Forty-two names on the manifest. One living biometric signal."),
        CodexEntry("first_signal", "THE SIGNAL", CodexCategory.SIGNAL, "An impossible transmission addressed the Eidolon by name—and then addressed you."),
        CodexEntry("dont_trust_her", "DO NOT TRUST HER", CodexCategory.EVENT, "A warning scratched into cryopod C-12 from the outside."),
        CodexEntry("termination_order", "TERMINATE STELLA", CodexCategory.EVENT, "A command terminal records an attempt by Captain Reyes to terminate STELLA's core."),
        CodexEntry("unknown_system", "UNKNOWN SYSTEM", CodexCategory.LOCATION, "A blue-white star and an unfamiliar planetary system. Eidolon is nowhere near its filed route."),
        CodexEntry("memory_fragment", "MEMORY FRAGMENT 01", CodexCategory.TECHNOLOGY, "A disconnected STELLA memory segment hidden inside bridge telemetry hardware.")
    ).associateBy { it.id }
}
