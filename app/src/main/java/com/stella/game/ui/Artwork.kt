package com.stella.game.ui

import com.stella.game.R

internal fun sceneArtRes(key: String): Int = when (key) {
    "cryo_awaken", "cryo_closeup" -> R.drawable.cryo_awaken
    "cryo_bay_dark", "cryo_exit", "empty_pod_detail" -> R.drawable.cryo_bay_dark
    "signal_static", "stella_terminal" -> R.drawable.signal_static
    "corridor_emergency" -> R.drawable.corridor_emergency
    "open_door_silhouette" -> R.drawable.open_door_silhouette
    "service_tunnel" -> R.drawable.service_tunnel
    "bridge_first_view" -> R.drawable.bridge_first_view
    else -> R.drawable.cryo_awaken
}

internal fun itemArtRes(id: String): Int = when (id) {
    "captains_access_card" -> R.drawable.captains_access_card
    "oxygen_canister" -> R.drawable.oxygen_canister
    "data_shard" -> R.drawable.data_shard
    "plasma_cutter" -> R.drawable.plasma_cutter
    "crew_photo" -> R.drawable.crew_photo
    "scanner" -> R.drawable.scanner
    "med_injector" -> R.drawable.med_injector
    "alien_crystal" -> R.drawable.alien_crystal
    "stella_memory_fragment" -> R.drawable.stella_memory_fragment
    else -> R.drawable.data_shard
}
