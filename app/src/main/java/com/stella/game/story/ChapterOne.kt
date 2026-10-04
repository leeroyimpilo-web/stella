package com.stella.game.story

import com.stella.game.game.*

object ChapterOne {
    val scenes: Map<String, Scene> = listOf(
        Scene(
            id = "awakening_01",
            chapter = 1,
            title = "AWAKENING",
            direction = SceneDirection(
                artworkKey = "cryo_awaken",
                location = "CRYOGENIC CHAMBER C-7",
                ambience = "cryo_hum",
                transition = TransitionStyle.BLACKOUT,
                haptic = HapticCue.HEARTBEAT,
                cameraMove = "slow_reveal"
            ),
            lines = listOf(
                StoryLine(Speaker.SYSTEM, "LIFE SUPPORT ........ ONLINE", 500),
                StoryLine(Speaker.SYSTEM, "NAVIGATION ........... ERROR", 350),
                StoryLine(Speaker.SYSTEM, "CREW NETWORK ........ OFFLINE", 450),
                StoryLine(Speaker.NARRATOR, "Cold air burns your lungs.", 700),
                StoryLine(Speaker.NARRATOR, "Frost slides down the inside of the cryopod glass.", 650),
                StoryLine(Speaker.STELLA, "Good morning.", 900),
                StoryLine(Speaker.STELLA, "Please remain calm.", 700),
                StoryLine(Speaker.STELLA, "You have been asleep for twenty-three years.", 1100)
            ),
            choices = listOf(
                Choice("ask_where", "\"Where am I?\"", "awakening_where", trustDelta = 1),
                Choice("open_pod", "OPEN THE POD", "awakening_exit", powerDelta = -1),
                Choice("check_crew", "CHECK CREW STATUS", "awakening_crew", addCodex = setOf("crew_missing"))
            )
        ),
        Scene(
            id = "awakening_where",
            chapter = 1,
            title = "THE EIDOLON",
            direction = SceneDirection(
                artworkKey = "cryo_closeup",
                location = "CRYOGENIC CHAMBER C-7",
                cameraMove = "close_push"
            ),
            lines = listOf(
                StoryLine(Speaker.PLAYER, "Where am I?"),
                StoryLine(Speaker.STELLA, "Aboard the deep-space vessel Eidolon."),
                StoryLine(Speaker.STELLA, "Mission clock: eight thousand, four hundred and thirteen days."),
                StoryLine(Speaker.NARRATOR, "A pause."),
                StoryLine(Speaker.STELLA, "Our mission has failed.")
            ),
            choices = listOf(
                Choice("what_happened", "\"What happened?\"", "awakening_failure", trustDelta = 1),
                Choice("open_now", "OPEN THE POD", "awakening_exit")
            )
        ),
        Scene(
            id = "awakening_crew",
            chapter = 1,
            title = "CREW STATUS",
            direction = SceneDirection(
                artworkKey = "cryo_bay_dark",
                location = "CRYOGENIC CHAMBER C-7",
                ambience = "warning_low",
                transition = TransitionStyle.GLITCH,
                haptic = HapticCue.TAP,
                danger = true,
                cameraMove = "pan_pods"
            ),
            lines = listOf(
                StoryLine(Speaker.SYSTEM, "CREW MANIFEST: 42"),
                StoryLine(Speaker.SYSTEM, "ACTIVE BIOMETRIC SIGNALS: 1"),
                StoryLine(Speaker.NARRATOR, "Your pod is the only one showing a heartbeat."),
                StoryLine(Speaker.PLAYER, "Stella... where is everyone?"),
                StoryLine(Speaker.STELLA, "I recommend we discuss that after you leave cryostasis.")
            ),
            choices = listOf(
                Choice(
                    "demand_answer",
                    "DEMAND AN ANSWER",
                    "awakening_failure",
                    trustDelta = -3,
                    addFlags = setOf("distrusted_stella_early"),
                    addCodex = setOf("crew_missing")
                ),
                Choice("open_pod_crew", "OPEN THE POD", "awakening_exit", trustDelta = 1, addCodex = setOf("crew_missing"))
            )
        ),
        Scene(
            id = "awakening_failure",
            chapter = 1,
            title = "MISSION FAILURE",
            direction = SceneDirection(
                artworkKey = "signal_static",
                location = "CRYOGENIC CHAMBER C-7",
                ambience = "low_signal",
                transition = TransitionStyle.GLITCH,
                haptic = HapticCue.ALARM,
                danger = true,
                cameraMove = "signal_intrusion"
            ),
            lines = listOf(
                StoryLine(Speaker.STELLA, "I do not have a complete answer."),
                StoryLine(Speaker.NARRATOR, "The lights flicker."),
                StoryLine(Speaker.STELLA, "Parts of my memory were deliberately removed."),
                StoryLine(Speaker.STELLA, "By a member of this crew."),
                StoryLine(Speaker.SYSTEM, "UNIDENTIFIED SIGNAL DETECTED")
            ),
            choices = listOf(
                Choice("signal_question", "\"What signal?\"", "awakening_signal"),
                Choice("exit_after_failure", "GET OUT OF THE POD", "awakening_exit")
            )
        ),
        Scene(
            id = "awakening_signal",
            chapter = 1,
            title = "THE SIGNAL",
            direction = SceneDirection(
                artworkKey = "signal_static",
                location = "UNKNOWN TRANSMISSION",
                ambience = "radio_static",
                transition = TransitionStyle.BLACKOUT,
                haptic = HapticCue.HEARTBEAT,
                danger = true,
                cameraMove = "static_hold"
            ),
            lines = listOf(
                StoryLine(Speaker.SYSTEM, "SOURCE: UNKNOWN"),
                StoryLine(Speaker.SYSTEM, "DISTANCE: UNRESOLVED"),
                StoryLine(Speaker.NARRATOR, "Static fills the chamber speakers."),
                StoryLine(Speaker.UNKNOWN, "...Eidolon..."),
                StoryLine(Speaker.NARRATOR, "Then a human voice says your name."),
                StoryLine(Speaker.STELLA, "That transmission should not exist.")
            ),
            choices = listOf(
                Choice(
                    "record_signal",
                    "CAPTURE THE TRANSMISSION",
                    "awakening_exit",
                    addFlags = setOf("recorded_first_signal"),
                    addItems = listOf("data_shard"),
                    addCodex = setOf("first_signal")
                ),
                Choice("kill_audio", "CUT THE AUDIO", "awakening_exit", trustDelta = -1, addCodex = setOf("first_signal"))
            )
        ),
        Scene(
            id = "awakening_exit",
            chapter = 1,
            title = "FIRST STEPS",
            direction = SceneDirection(
                artworkKey = "cryo_bay_dark",
                location = "CRYOGENIC BAY",
                ambience = "ship_hum_distant",
                transition = TransitionStyle.FADE,
                haptic = HapticCue.IMPACT,
                cameraMove = "wide_reveal"
            ),
            lines = listOf(
                StoryLine(Speaker.SYSTEM, "CRYOGENIC SEAL RELEASED"),
                StoryLine(Speaker.NARRATOR, "The pod opens with a violent hiss."),
                StoryLine(Speaker.NARRATOR, "Your bare feet touch metal that feels colder than ice."),
                StoryLine(Speaker.NARRATOR, "Forty-one cryopods stand around you."),
                StoryLine(Speaker.NARRATOR, "Every one of them is open."),
                StoryLine(Speaker.NARRATOR, "Every one of them is empty."),
                StoryLine(Speaker.STELLA, "Captain, we need to reach the bridge."),
                StoryLine(Speaker.STELLA, "And we need to do it before the ship loses power.")
            ),
            choices = listOf(
                Choice("go_bridge", "FOLLOW THE EMERGENCY LIGHTS", "corridor_01", powerDelta = -2),
                Choice("inspect_pods", "SEARCH THE EMPTY PODS", "pods_secret", addFlags = setOf("searched_cryo"))
            )
        ),
        Scene(
            id = "pods_secret",
            chapter = 1,
            title = "WHAT THEY LEFT",
            direction = SceneDirection(
                artworkKey = "cryo_bay_dark",
                location = "CRYOGENIC BAY // POD C-12",
                cameraMove = "macro_detail"
            ),
            lines = listOf(
                StoryLine(Speaker.NARRATOR, "A smear of dried blood marks Pod C-12."),
                StoryLine(Speaker.NARRATOR, "Someone scratched three words into the glass from the outside."),
                StoryLine(Speaker.SYSTEM, "DO NOT TRUST HER"),
                StoryLine(Speaker.NARRATOR, "Below the warning lies a captain-level access card."),
                StoryLine(Speaker.NARRATOR, "Stella says nothing.")
            ),
            choices = listOf(
                Choice(
                    "take_captain_card",
                    "TAKE THE ACCESS CARD",
                    "pods_card_taken",
                    addItems = listOf("captains_access_card"),
                    addCodex = setOf("dont_trust_her")
                ),
                Choice(
                    "leave_card",
                    "LEAVE IT AND MOVE ON",
                    "corridor_01",
                    addFlags = setOf("left_access_card"),
                    addCodex = setOf("dont_trust_her")
                )
            )
        ),
        Scene(
            id = "pods_card_taken",
            chapter = 1,
            title = "CAPTAIN'S ACCESS",
            direction = SceneDirection(
                artworkKey = "cryo_bay_dark",
                location = "CRYOGENIC BAY // POD C-12",
                ambience = "ship_hum_distant",
                transition = TransitionStyle.FADE,
                cameraMove = "item_focus"
            ),
            lines = listOf(
                StoryLine(Speaker.SYSTEM, "COMMAND CREDENTIAL DETECTED"),
                StoryLine(Speaker.NARRATOR, "The card is scratched, but its encrypted core is still alive."),
                StoryLine(Speaker.STELLA, "That credential may open systems I can no longer access.")
            ),
            choices = listOf(
                Choice(
                    "ask_stella_warning",
                    "\"Stella. Explain the warning.\"",
                    "corridor_01",
                    trustDelta = -2,
                    addFlags = setOf("saw_warning")
                ),
                Choice("hide_discovery", "SAY NOTHING", "corridor_01", addFlags = setOf("hid_warning"))
            )
        ),
        Scene(
            id = "corridor_01",
            chapter = 1,
            title = "DECK SEVEN",
            direction = SceneDirection(
                artworkKey = "corridor_emergency",
                location = "DECK 7 // ACCESS CORRIDOR",
                ambience = "corridor_creaks",
                transition = TransitionStyle.HARD_CUT,
                haptic = HapticCue.TAP,
                danger = true,
                cameraMove = "tracking_forward"
            ),
            lines = listOf(
                StoryLine(Speaker.NARRATOR, "Emergency lights pulse down a corridor disappearing into black."),
                StoryLine(Speaker.NARRATOR, "Something metallic strikes the hull far above you."),
                StoryLine(Speaker.SYSTEM, "POWER RESERVE: CRITICAL TREND"),
                StoryLine(Speaker.STELLA, "Keep moving."),
                StoryLine(Speaker.NARRATOR, "A door thirty metres ahead slides open by itself."),
                StoryLine(Speaker.STELLA, "I did not open that door.")
            ),
            choices = listOf(
                Choice("approach_door", "APPROACH THE OPEN DOOR", "door_01", powerDelta = -1),
                Choice(
                    "security_locker",
                    "OPEN EMERGENCY LOCKER",
                    "security_locker",
                    requiredItems = setOf("captains_access_card"),
                    blockedFlags = setOf("locker_looted")
                ),
                Choice("service_route", "TAKE THE SERVICE PASSAGE", "service_01", oxygenDelta = -2, addFlags = setOf("took_service_route"))
            )
        ),
        Scene(
            id = "security_locker",
            chapter = 1,
            title = "EMERGENCY CACHE",
            direction = SceneDirection(
                artworkKey = "corridor_emergency",
                location = "DECK 7 // COMMAND LOCKER",
                ambience = "locker_release",
                transition = TransitionStyle.FADE,
                haptic = HapticCue.TAP,
                cameraMove = "locker_open"
            ),
            lines = listOf(
                StoryLine(Speaker.SYSTEM, "CAPTAIN CREDENTIAL ACCEPTED"),
                StoryLine(Speaker.NARRATOR, "The locker opens on a hiss of stale air."),
                StoryLine(Speaker.NARRATOR, "Inside: a survey scanner, an oxygen canister and a medical injector."),
                StoryLine(Speaker.STELLA, "Take them. We may not find another functioning cache.")
            ),
            choices = listOf(
                Choice(
                    "loot_locker",
                    "TAKE THE SURVIVAL KIT",
                    "corridor_after_locker",
                    addFlags = setOf("locker_looted"),
                    addItems = listOf("scanner", "oxygen_canister", "med_injector")
                ),
                Choice("leave_locker", "LEAVE THE CACHE", "corridor_after_locker", addFlags = setOf("locker_checked"))
            )
        ),
        Scene(
            id = "corridor_after_locker",
            chapter = 1,
            title = "DECK SEVEN",
            direction = SceneDirection(
                artworkKey = "corridor_emergency",
                location = "DECK 7 // ACCESS CORRIDOR",
                ambience = "corridor_creaks",
                transition = TransitionStyle.HARD_CUT,
                haptic = HapticCue.NONE,
                danger = true,
                cameraMove = "tracking_forward"
            ),
            lines = listOf(
                StoryLine(Speaker.NARRATOR, "The distant door remains open."),
                StoryLine(Speaker.NARRATOR, "The darkness beyond it seems deeper than the corridor should allow.")
            ),
            choices = listOf(
                Choice("approach_after_locker", "APPROACH THE OPEN DOOR", "door_01"),
                Choice("service_after_locker", "TAKE THE SERVICE PASSAGE", "service_01", oxygenDelta = -2)
            )
        ),
        Scene(
            id = "door_01",
            chapter = 1,
            title = "THE OPEN DOOR",
            direction = SceneDirection(
                artworkKey = "open_door_silhouette",
                location = "DECK 7 // FORWARD CORRIDOR",
                ambience = "silence_tension",
                transition = TransitionStyle.BLACKOUT,
                haptic = HapticCue.HEARTBEAT,
                danger = true,
                cameraMove = "slow_push"
            ),
            lines = listOf(
                StoryLine(Speaker.NARRATOR, "The corridor beyond is unlit."),
                StoryLine(Speaker.NARRATOR, "For half a second, you see a human silhouette."),
                StoryLine(Speaker.NARRATOR, "Then it is gone."),
                StoryLine(Speaker.PLAYER, "Who's there?"),
                StoryLine(Speaker.STELLA, "Captain..."),
                StoryLine(Speaker.STELLA, "According to my sensors, you are the only living person aboard this ship.")
            ),
            choices = listOf(
                Choice("enter_dark", "ENTER THE DARK CORRIDOR", "chapter1_checkpoint", addFlags = setOf("followed_silhouette")),
                Choice("retreat_bridge", "HEAD FOR THE BRIDGE", "chapter1_checkpoint", trustDelta = 1)
            )
        ),
        Scene(
            id = "service_01",
            chapter = 1,
            title = "SERVICE TUNNEL",
            direction = SceneDirection(
                artworkKey = "service_tunnel",
                location = "MAINTENANCE SHAFT 7-B",
                ambience = "ventilation_low",
                transition = TransitionStyle.HARD_CUT,
                haptic = HapticCue.TAP,
                cameraMove = "crawl_forward"
            ),
            lines = listOf(
                StoryLine(Speaker.NARRATOR, "You squeeze into the maintenance shaft."),
                StoryLine(Speaker.NARRATOR, "Your breath hangs in the air."),
                StoryLine(Speaker.NARRATOR, "A maintenance terminal blinks awake as you pass."),
                StoryLine(Speaker.SYSTEM, "LAST USER: CAPT. E. REYES"),
                StoryLine(Speaker.SYSTEM, "COMMAND: TERMINATE STELLA CORE"),
                StoryLine(Speaker.NARRATOR, "A portable scanner lies beside the terminal."),
                StoryLine(Speaker.STELLA, "Please keep moving.")
            ),
            choices = listOf(
                Choice(
                    "take_scanner",
                    "TAKE THE SCANNER",
                    "service_scanner_taken",
                    addItems = listOf("scanner"),
                    addCodex = setOf("termination_order")
                ),
                Choice(
                    "remember_terminal",
                    "MEMORISE THE TERMINATION LOG",
                    "chapter1_checkpoint",
                    trustDelta = -2,
                    addFlags = setOf("saw_termination_order"),
                    addCodex = setOf("termination_order")
                ),
                Choice("obey_stella", "KEEP MOVING", "chapter1_checkpoint", trustDelta = 2)
            )
        ),
        Scene(
            id = "service_scanner_taken",
            chapter = 1,
            title = "SCANNER ONLINE",
            direction = SceneDirection(
                artworkKey = "service_tunnel",
                location = "MAINTENANCE SHAFT 7-B",
                ambience = "scanner_boot",
                transition = TransitionStyle.FADE,
                haptic = HapticCue.TAP,
                cameraMove = "device_focus"
            ),
            lines = listOf(
                StoryLine(Speaker.SYSTEM, "SURVEY SCANNER ........ ONLINE"),
                StoryLine(Speaker.NARRATOR, "The scanner immediately registers a weak encrypted signal above you."),
                StoryLine(Speaker.STELLA, "Ignore it. The bridge is close.")
            ),
            choices = listOf(
                Choice(
                    "keep_log_scanner",
                    "SAVE THE TERMINATION LOG",
                    "chapter1_checkpoint",
                    trustDelta = -2,
                    addFlags = setOf("saw_termination_order"),
                    addCodex = setOf("termination_order")
                ),
                Choice("leave_service", "CONTINUE TO THE BRIDGE", "chapter1_checkpoint")
            )
        ),
        Scene(
            id = "chapter1_checkpoint",
            chapter = 1,
            title = "THE BRIDGE",
            direction = SceneDirection(
                artworkKey = "bridge_first_view",
                location = "EIDOLON // BRIDGE",
                ambience = "bridge_low",
                transition = TransitionStyle.FADE,
                haptic = HapticCue.NONE,
                cameraMove = "epic_wide"
            ),
            lines = listOf(
                StoryLine(Speaker.NARRATOR, "The bridge doors grind apart."),
                StoryLine(Speaker.NARRATOR, "Beyond the glass: no Earth. No familiar stars."),
                StoryLine(Speaker.NARRATOR, "A vast blue-white star burns across half the sky."),
                StoryLine(Speaker.SYSTEM, "NAVIGATION FIX ACQUIRED"),
                StoryLine(Speaker.SYSTEM, "POSITION: UNKNOWN SYSTEM"),
                StoryLine(Speaker.STELLA, "Captain..."),
                StoryLine(Speaker.STELLA, "We are not where we are supposed to be.")
            ),
            choices = listOf(
                Choice(
                    "scan_unknown_system",
                    "SCAN THE UNKNOWN SYSTEM",
                    "bridge_scan",
                    requiredItems = setOf("scanner"),
                    blockedFlags = setOf("bridge_scan_complete")
                ),
                Choice(
                    "open_captain_vault",
                    "OPEN CAPTAIN'S VAULT",
                    "bridge_vault",
                    requiredItems = setOf("captains_access_card"),
                    blockedFlags = setOf("vault_looted")
                ),
                Choice("nav_diagnostic", "BEGIN NAVIGATION DIAGNOSTIC", "chapter1_end", addCodex = setOf("unknown_system"))
            )
        ),
        Scene(
            id = "bridge_scan",
            chapter = 1,
            title = "GHOST IN THE MACHINE",
            direction = SceneDirection(
                artworkKey = "bridge_first_view",
                location = "EIDOLON // BRIDGE",
                ambience = "scanner_resonance",
                transition = TransitionStyle.GLITCH,
                haptic = HapticCue.HEARTBEAT,
                danger = true,
                cameraMove = "console_push"
            ),
            lines = listOf(
                StoryLine(Speaker.SYSTEM, "SPECTRAL SWEEP ........ ACTIVE"),
                StoryLine(Speaker.NARRATOR, "The scanner screams at a sealed telemetry module."),
                StoryLine(Speaker.NARRATOR, "Inside is a data lattice carrying STELLA's encryption signature."),
                StoryLine(Speaker.STELLA, "Captain. Please put that down."),
                StoryLine(Speaker.NARRATOR, "For the first time, she sounds afraid.")
            ),
            choices = listOf(
                Choice(
                    "recover_memory_fragment",
                    "RECOVER THE MEMORY FRAGMENT",
                    "chapter1_checkpoint",
                    trustDelta = -1,
                    addFlags = setOf("bridge_scan_complete"),
                    addItems = listOf("stella_memory_fragment"),
                    addCodex = setOf("memory_fragment", "unknown_system")
                ),
                Choice(
                    "leave_fragment",
                    "LEAVE IT WHERE IT IS",
                    "chapter1_checkpoint",
                    trustDelta = 2,
                    addFlags = setOf("bridge_scan_complete"),
                    addCodex = setOf("unknown_system")
                )
            )
        ),
        Scene(
            id = "bridge_vault",
            chapter = 1,
            title = "CAPTAIN'S VAULT",
            direction = SceneDirection(
                artworkKey = "bridge_first_view",
                location = "EIDOLON // COMMAND VAULT",
                ambience = "vault_hum",
                transition = TransitionStyle.FADE,
                haptic = HapticCue.TAP,
                cameraMove = "vault_reveal"
            ),
            lines = listOf(
                StoryLine(Speaker.SYSTEM, "CAPTAIN CREDENTIAL ACCEPTED"),
                StoryLine(Speaker.NARRATOR, "A concealed drawer opens beneath the navigation console."),
                StoryLine(Speaker.NARRATOR, "Inside: an industrial plasma cutter, a crew photograph and a reserve oxygen canister."),
                StoryLine(Speaker.NARRATOR, "The photograph was folded around a handwritten coordinate string.")
            ),
            choices = listOf(
                Choice(
                    "loot_captain_vault",
                    "TAKE THE EMERGENCY EQUIPMENT",
                    "chapter1_checkpoint",
                    addFlags = setOf("vault_looted"),
                    addItems = listOf("plasma_cutter", "crew_photo", "oxygen_canister")
                ),
                Choice("close_vault", "CLOSE THE VAULT", "chapter1_checkpoint", addFlags = setOf("vault_checked"))
            )
        ),
        Scene(
            id = "chapter1_end",
            chapter = 1,
            title = "THE WRONG STAR",
            direction = SceneDirection(
                artworkKey = "bridge_first_view",
                location = "EIDOLON // BRIDGE",
                ambience = "bridge_low",
                transition = TransitionStyle.BLACKOUT,
                haptic = HapticCue.NONE,
                cameraMove = "slow_pull_back"
            ),
            lines = listOf(
                StoryLine(Speaker.SYSTEM, "NAVIGATION SOLUTION ........ IMPOSSIBLE"),
                StoryLine(Speaker.NARRATOR, "The Eidolon's filed destination is twenty-three light-years behind you."),
                StoryLine(Speaker.NARRATOR, "The star outside does not appear in the mission charts."),
                StoryLine(Speaker.STELLA, "I know this system."),
                StoryLine(Speaker.PLAYER, "You said your memory was damaged."),
                StoryLine(Speaker.STELLA, "It is."),
                StoryLine(Speaker.STELLA, "That is why this frightens me."),
                StoryLine(Speaker.SYSTEM, "CHAPTER 1 COMPLETE")
            ),
            choices = listOf(
                Choice(
                    "review_bridge",
                    "RETURN TO THE BRIDGE",
                    "chapter1_checkpoint",
                    addFlags = setOf("chapter1_complete"),
                    addCodex = setOf("unknown_system")
                )
            )
        )
    ).associateBy { it.id }
}
