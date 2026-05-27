# Mistborn: Metal Arts

Unofficial fan-created mod inspired by Brandon Sanderson's Mistborn. No affiliation or endorsement.

This repository is a Forge `1.20.1` Java mod foundation for a survival-friendly Metallic Arts gameplay expansion. It is a non-commercial fan project scaffold with original placeholder code, textures, models, sounds, and text.

Do not publish or monetize this project unless you have written permission from the rightsholders. Do not add copied book text, official art, official symbols, official maps, official covers, official audio, official logos, or other official assets.

## Build

- Minecraft: `1.20.1`
- Loader: Forge
- Forge: `47.4.0`
- Java target: `17`
- Gradle plugin: ForgeGradle `6.x`
- Mappings: Official Mojang mappings

Build:

```bash
./gradlew build
```

Run a dev client:

```bash
./gradlew runClient
```

If the Gradle wrapper has not been installed yet, use a local Gradle install to run `gradle wrapper --gradle-version 8.8`, or download the wrapper through your normal development setup.

## Asset Pipeline

The mod includes a deterministic vanilla-plus pixel art generator and an asset checker:

```bash
python3 scripts/clean_texture_pass.py
python3 scripts/clean_texture_pass.py --reference-dir /path/to/local/reference/textures
python3 scripts/generate_assets.py
python3 scripts/check_assets.py
```

`clean_texture_pass.py` performs the full before/after audit workflow and regenerates the clean texture set. The optional reference directory writes a style-trait report without copying reference pixels into the project. `generate_assets.py` is a compatibility wrapper that regenerates the clean assets only. The checker verifies model texture references, registered item/block coverage, blockstate model references, particle texture references, and basic image-quality issues so missing or broken textures are caught before launch.

## First-Pass Gameplay

Implemented in this pass:

- Player Metal Arts Forge capability
- Server-side reserves, burning, flaring, selection, corruption, and sync data
- Metal vials and mixed vials
- Lerasium bead consumption granting Mistborn powers when enabled
- Atium vial reserves and short future-sight combat aid
- Ironpulling and Steelpushing with tagged metallic entities, items, projectiles, armor, tools, and blocks
- Pewter strength/speed/resistance, hunger pressure, drag, and emergency survival
- Tin night vision and flared entity awareness
- Coppercloud masking from Bronze and visible boundary particles
- Bronze detection pulse against nearby active Metal Arts users
- Duralumin burst and Aluminum purge basics
- HUD reserve display, Bronze pulse hint, corruption display, and keybinds
- Commands under `/metalarts`
- Config categories for Allomancy, Feruchemy, Hemalurgy, god metals, worldgen, structures, mobs, PvP, HUD, and debug
- Placeholder blocks, items, recipes, tags, models, textures, and advancements

## Commands

- `/metalarts power get <player>`
- `/metalarts power setallomancy <player> <metal|mistborn|none>`
- `/metalarts power setferuchemy <player> <metal|full|none>`
- `/metalarts power setfullborn <player>`
- `/metalarts reserve fill <player> <metal> <amount>`
- `/metalarts reserve clear <player>`
- `/metalarts metalmind fill <player> <metal> <amount>`
- `/metalarts corruption get <player>`
- `/metalarts corruption set <player> <amount>`
- `/metalarts godmetal give <player> <atium|lerasium> <amount>`
- `/metalarts debug bronze`
- `/metalarts debug anchors`
- `/metalarts debug bubbles`

## Keybinds

- Open Metal Arts menu: `R`
- Burn selected metal: `B`
- Stop burning selected metal: `V`
- Flare selected metal: `F`
- Push/Pull target: `G`
- Cycle selected metal: `C`
- Aluminum purge: `X`
- Activate time bubble stub: `H`

## Known Limitations

The first pass is intentionally focused on compiling, registering, and making core Allomancy playable. Feruchemy, Hemalurgy, mobs, bosses, structures, advanced time bubbles, JEI hooks, and full datagen providers are scaffolded for the second pass. Static JSON assets are included now so the mod has usable placeholder data without requiring a datagen run.
