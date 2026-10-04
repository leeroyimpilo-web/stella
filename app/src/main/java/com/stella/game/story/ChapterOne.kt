package com.stella.game.story

import com.stella.game.game.*

object ChapterOne {
    val scenes: Map<String, Scene> = listOf(
        Scene(
            "awakening_01", 1, "AWAKENING",
            SceneDirection("cryo_awaken", "CRYOGENIC CHAMBER C-7", "cryo_hum", TransitionStyle.BLACKOUT, HapticCue.HEARTBEAT, cameraMove = "slow_reveal"),
            listOf(
                StoryLine(Speaker.SYSTEM, "LIFE SUPPORT ........ ONLINE", 500),
                StoryLine(Speaker.SYSTEM, "NAVIGATION ........... ERROR", 350),
                StoryLine(Speaker.SYSTEM, "CREW NETWORK ........ OFFLINE", 450),
                StoryLine(Speaker.NARRATOR, "Cold air burns your lungs.", 700),
                StoryLine(Speaker.NARRATOR, "Frost slides down the inside of the cryopod glass.", 650),
                StoryLine(Speaker.STELLA, "Good morning.", 900),
                StoryLine(Speaker.STELLA, "Please remain calm.", 700),
                StoryLine(Speaker.STELLA, "You have been asleep for twenty-three years.", 1100)
            ),
            listOf(
                Choice("ask_where", "\"Where am I?\"", "awakening_where", trustDelta = 1),
                Choice("open_pod", "OPEN THE POD", "awakening_exit", powerDelta = -1),
                Choice("check_crew", "CHECK CREW STATUS", "awakening_crew")
            )
        ),
        Scene(
            "awakening_where", 1, "THE EIDOLON",
            SceneDirection("cryo_closeup", "CRYOGENIC CHAMBER C-7", cameraMove = "close_push"),
            listOf(
                StoryLine(Speaker.PLAYER, "Where am I?"),
                StoryLine(Speaker.STELLA, "Aboard the deep-space vessel Eidolon."),
                StoryLine(Speaker.STELLA, "Mission clock: eight thousand, four hundred and thirteen days."),
                StoryLine(Speaker.NARRATOR, "A pause."),
                StoryLine(Speaker.STELLA, "Our mission has failed.")
            ),
            listOf(
                Choice("what_happened", "\"What happened?\"", "awakening_failure", trustDelta = 1),
                Choice("open_now", "OPEN THE POD", "awakening_exit")
            )
        ),
        Scene(
            "awakening_crew", 1, "CREW STATUS",
            SceneDirection("cryo_bay_dark", "CRYOGENIC CHAMBER C-7", "warning_low", TransitionStyle.GLITCH, HapticCue.TAP, true, "pan_pods"),
            listOf(
                StoryLine(Speaker.SYSTEM, "CREW MANIFEST: 42"),
                StoryLine(Speaker.SYSTEM, "ACTIVE BIOMETRIC SIGNALS: 1"),
                StoryLine(Speaker.NARRATOR, "Your pod is the only one showing a heartbeat."),
                StoryLine(Speaker.PLAYER, "Stella... where is everyone?"),
                StoryLine(Speaker.STELLA, "I recommend we discuss that after you leave cryostasis.")
            ),
            listOf(
                Choice("demand_answer", "DEMAND AN ANSWER", "awakening_failure", trustDelta = -3, addFlags = setOf("distrusted_stella_early")),
                Choice("open_pod_crew", "OPEN THE POD", "awakening_exit", trustDelta = 1)
            )
        ),
        Scene(
            "awakening_failure", 1, "MISSION FAILURE",
            SceneDirection("stella_terminal", "CRYOGENIC CHAMBER C-7", "low_signal", TransitionStyle.GLITCH, HapticCue.ALARM, true),
            listOf(
                StoryLine(Speaker.STELLA, "I do not have a complete answer."),
                StoryLine(Speaker.NARRATOR, "The lights flicker."),
                StoryLine(Speaker.STELLA, "Parts of my memory were deliberately removed."),
                StoryLine(Speaker.STELLA, "By a member of this crew."),
                StoryLine(Speaker.SYSTEM, "UNIDENTIFIED SIGNAL DETECTED")
            ),
            listOf(
                Choice("signal_question", "\"What signal?\"", "awakening_signal"),
                Choice("exit_after_failure", "GET OUT OF THE POD", "awakening_exit")
            )
        ),
        Scene(
            "awakening_signal", 1, "THE SIGNAL",
            SceneDirection("signal_static", "UNKNOWN TRANSMISSION", "radio_static", TransitionStyle.BLACKOUT, HapticCue.HEARTBEAT, true, "static_hold"),
            listOf(
                StoryLine(Speaker.SYSTEM, "SOURCE: UNKNOWN"),
                StoryLine(Speaker.SYSTEM, "DISTANCE: UNRESOLVED"),
                StoryLine(Speaker.NARRATOR, "Static fills the chamber speakers."),
                StoryLine(Speaker.UNKNOWN, "...Eidolon..."),
                StoryLine(Speaker.NARRATOR, "Then a human voice says your name."),
                StoryLine(Speaker.STELLA, "That transmission should not exist.")
            ),
            listOf(
                Choice("record_signal", "RECORD THE TRANSMISSION", "awakening_exit", addFlags = setOf("recorded_first_signal")),
                Choice("kill_audio", "CUT THE AUDIO", "awakening_exit", trustDelta = -1)
            )
        ),
        Scene(
            "awakening_exit", 1, "FIRST STEPS",
            SceneDirection("cryo_exit", "CRYOGENIC BAY", "ship_hum_distant", TransitionStyle.FADE, HapticCue.IMPACT, cameraMove = "wide_reveal"),
            listOf(
                StoryLine(Speaker.SYSTEM, "CRYOGENIC SEAL RELEASED"),
                StoryLine(Speaker.NARRATOR, "The pod opens with a violent hiss."),
                StoryLine(Speaker.NARRATOR, "Your bare feet touch metal that feels colder than ice."),
                StoryLine(Speaker.NARRATOR, "Forty-one cryopods stand around you."),
                StoryLine(Speaker.NARRATOR, "Every one of them is open."),
                StoryLine(Speaker.NARRATOR, "Every one of them is empty."),
                StoryLine(Speaker.STELLA, "Captain, we need to reach the bridge."),
                StoryLine(Speaker.STELLA, "And we need to do it before the ship loses power.")
            ),
            listOf(
                Choice("go_bridge", "FOLLOW THE EMERGENCY LIGHTS", "corridor_01", powerDelta = -2),
                Choice("inspect_pods", "INSPECT THE EMPTY PODS", "pods_secret", addFlags = setOf("searched_cryo"))
            )
        ),
        Scene(
            "pods_secret", 1, "WHAT THEY LEFT",
            SceneDirection("empty_pod_detail", "CRYOGENIC BAY", cameraMove = "macro_detail"),
            listOf(
                StoryLine(Speaker.NARRATOR, "A smear of dried blood marks Pod C-12."),
                StoryLine(Speaker.NARRATOR, "Someone scratched three words into the glass from the outside."),
                StoryLine(Speaker.SYSTEM, "DO NOT TRUST HER"),
                StoryLine(Speaker.NARRATOR, "Stella says nothing.")
            ),
            listOf(
                Choice("ask_stella_warning", "\"Stella. Explain this.\"", "corridor_01", trustDelta = -2, addFlags = setOf("saw_warning")),
                Choice("hide_discovery", "SAY NOTHING", "corridor_01", addFlags = setOf("hid_warning"))
            )
        ),
        Scene(
            "corridor_01", 1, "DECK SEVEN",
            SceneDirection("corridor_emergency", "DECK 7 // ACCESS CORRIDOR", "corridor_creaks", TransitionStyle.HARD_CUT, HapticCue.TAP, true, "tracking_forward"),
            listOf(
                StoryLine(Speaker.NARRATOR, "Emergency lights pulse down a corridor disappearing into black."),
                StoryLine(Speaker.NARRATOR, "Something metallic strikes the hull far above you."),
                StoryLine(Speaker.SYSTEM, "POWER RESERVE: CRITICAL TREND"),
                StoryLine(Speaker.STELLA, "Keep moving."),
                StoryLine(Speaker.NARRATOR, "A door thirty metres ahead slides open by itself."),
                StoryLine(Speaker.STELLA, "I did not open that door.")
            ),
            listOf(
                Choice("approach_door", "APPROACH THE OPEN DOOR", "door_01", powerDelta = -1),
                Choice("service_route", "TAKE THE SERVICE PASSAGE", "service_01", oxygenDelta = -2, addFlags = setOf("took_service_route"))
            )
        ),
        Scene(
            "door_01", 1, "THE OPEN DOOR",
            SceneDirection("open_door_silhouette", "DECK 7 // FORWARD CORRIDOR", "silence_tension", TransitionStyle.BLACKOUT, HapticCue.HEARTBEAT, true),
            listOf(
                StoryLine(Speaker.NARRATOR, "The corridor beyond is unlit."),
                StoryLine(Speaker.NARRATOR, "For half a second, you see a human silhouette."),
                StoryLine(Speaker.NARRATOR, "Then it is gone."),
                StoryLine(Speaker.PLAYER, "Who's there?"),
                StoryLine(Speaker.STELLA, "Captain..."),
                StoryLine(Speaker.STELLA, "According to my sensors, you are the only living person aboard this ship.")
            ),
            listOf(
                Choice("enter_dark", "ENTER THE DARK CORRIDOR", "chapter1_checkpoint", addFlags = setOf("followed_silhouette")),
                Choice("retreat_bridge", "HEAD FOR THE BRIDGE", "chapter1_checkpoint", trustDelta = 1)
            )
        ),
        Scene(
            "service_01", 1, "SERVICE TUNNEL",
            SceneDirection("service_tunnel", "MAINTENANCE SHAFT 7-B", "ventilation_low", TransitionStyle.HARD_CUT, HapticCue.TAP, cameraMove = "crawl_forward"),
            listOf(
                StoryLine(Speaker.NARRATOR, "You squeeze into the maintenance shaft."),
                StoryLine(Speaker.NARRATOR, "Your breath hangs in the air."),
                StoryLine(Speaker.NARRATOR, "A maintenance terminal blinks awake as you pass."),
                StoryLine(Speaker.SYSTEM, "LAST USER: CAPT. E. REYES"),
                StoryLine(Speaker.SYSTEM, "COMMAND: TERMINATE STELLA CORE"),
                StoryLine(Speaker.STELLA, "Please keep moving.")
            ),
            listOf(
                Choice("remember_terminal", "MEMORISE THE LOG", "chapter1_checkpoint", trustDelta = -2, addFlags = setOf("saw_termination_order")),
                Choice("obey_stella", "KEEP MOVING", "chapter1_checkpoint", trustDelta = 2)
            )
        ),
        Scene(
            "chapter1_checkpoint", 1, "THE BRIDGE",
            SceneDirection("bridge_first_view", "EIDOLON // BRIDGE", "bridge_low", TransitionStyle.FADE, HapticCue.NONE, cameraMove = "epic_wide"),
            listOf(
                StoryLine(Speaker.NARRATOR, "The bridge doors grind apart."),
                StoryLine(Speaker.NARRATOR, "Beyond the glass: no Earth. No familiar stars."),
                StoryLine(Speaker.NARRATOR, "A vast blue-white star burns across half the sky."),
                StoryLine(Speaker.SYSTEM, "NAVIGATION FIX ACQUIRED"),
                StoryLine(Speaker.SYSTEM, "POSITION: UNKNOWN SYSTEM"),
                StoryLine(Speaker.STELLA, "Captain..."),
                StoryLine(Speaker.STELLA, "We are not where we are supposed to be.")
            ),
            listOf(
                Choice("chapter_continue", "RESTART OPENING PREVIEW", "awakening_01", addFlags = setOf("chapter1_preview_complete"))
            )
        )
    ).associateBy { it.id }
}
