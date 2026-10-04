# STELLA Cinematic Artwork Manifest

## Visual direction
Portrait 9:16 cinematic science-fiction environments using the approved STELLA look: dark industrial starship interiors, wet reflective metal, cold cyan/blue illumination, red emergency accents, fog, condensation, volumetric light and strong central perspective.

## Generated scene set
| Scene key | Artwork | Usage |
| --- | --- | --- |
| `cryo_awaken` | Frozen Cryo Bay on a Derelict Starship | Opening awakening |
| `cryo_bay_dark` | Abandoned Cryogenic Corridor | Empty pod / missing crew investigation |
| `corridor_emergency` | Neon Fogged Industrial Space Station Corridor | Deck 7 traversal |
| `open_door_silhouette` | Frozen Cyberpunk Airlock Corridor | Open-door / silhouette tension |
| `service_tunnel` | Rain-Soaked Cyberpunk Maintenance Shaft | Service Shaft 7-B |
| `signal_static` | Glitching AI Core Transmission Chamber | Impossible signal / STELLA memory sequences |
| `bridge_access` | Neon Mist at the Sealed Blast Door | Sealed bridge approach |
| `bridge_first_view` | Silent Bridge Beneath a Blue Star | Bridge reveal |

## Text presentation rule
Story dialogue now scrolls upward over the lower portion of the image. The newest line is full-brightness while previous lines fade backward into the scene. Automatic reading time is calculated from word count and is clamped between 3.2 and 7.6 seconds per line. The player can tap the dialogue layer to advance manually.

## Inventory pickup rule
Pickup notifications appear near the upper centre of the image rather than over the story text. They remain visible for 3.6 seconds and then clear automatically.

## Production rule
Scene artwork contains no baked-in UI or dialogue. All HUD, objective, inventory, codex and dialogue elements remain native Compose overlays.
