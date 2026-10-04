package com.stella.game.game

enum class ItemCategory { KEY, EQUIPMENT, RESOURCE, DATA, RELIC }

data class ItemDefinition(
    val id: String,
    val name: String,
    val description: String,
    val category: ItemCategory,
    val stackable: Boolean = false,
    val consumable: Boolean = false
)

object ItemCatalog {
    val all = listOf(
        ItemDefinition(
            "captains_access_card",
            "Captain's Access Card",
            "Encrypted command credential. Automatically unlocks captain-level doors and systems.",
            ItemCategory.KEY
        ),
        ItemDefinition(
            "oxygen_canister",
            "Oxygen Canister",
            "Portable emergency oxygen. Restores 25% suit oxygen.",
            ItemCategory.RESOURCE,
            stackable = true,
            consumable = true
        ),
        ItemDefinition(
            "data_shard",
            "Data Shard",
            "A hardened portable storage crystal containing captured signal data.",
            ItemCategory.DATA,
            stackable = true
        ),
        ItemDefinition(
            "plasma_cutter",
            "Plasma Cutter",
            "Industrial cutting tool. Can breach damaged doors, panels and light structural locks.",
            ItemCategory.EQUIPMENT
        ),
        ItemDefinition(
            "crew_photo",
            "Crew Photograph",
            "A worn photograph of Eidolon's command crew. Several faces have been scratched out.",
            ItemCategory.DATA
        ),
        ItemDefinition(
            "scanner",
            "Survey Scanner",
            "Handheld spectral scanner capable of finding hidden electronics, energy signatures and anomalies.",
            ItemCategory.EQUIPMENT
        ),
        ItemDefinition(
            "med_injector",
            "Med Injector",
            "Emergency auto-injector. Restores 30% biological condition.",
            ItemCategory.RESOURCE,
            stackable = true,
            consumable = true
        ),
        ItemDefinition(
            "alien_crystal",
            "Unknown Crystal",
            "A non-human crystalline material that reacts to STELLA's signal carrier.",
            ItemCategory.RELIC
        ),
        ItemDefinition(
            "stella_memory_fragment",
            "STELLA Memory Fragment",
            "A severed piece of STELLA's encrypted memory lattice.",
            ItemCategory.KEY
        )
    ).associateBy { it.id }

    fun get(id: String): ItemDefinition? = all[id]
}
