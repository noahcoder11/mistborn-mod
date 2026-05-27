# Current State Audit

Date: 2026-05-15

## Project Target

- Loader: Forge
- Minecraft: 1.20.1
- Java: 17 target in `build.gradle`
- Mappings: Official Mojang mappings
- Mod id: `mistborn_metal_arts`
- Base package: `com.not_noah.mistborn_metal_arts`

## Entrypoint

- `src/main/java/com/not_noah/mistborn_metal_arts/MistbornMetalArts.java`
- Registers item, block, effect, sound, particle, menu, block entity, entity type, and creative tab DeferredRegisters.
- Registers common/server configs.
- Registers the SimpleChannel network.
- Registers Forge event handlers and `/metalarts`.
- Registers optional Curios compatibility through a reflection-backed bridge only when Curios is loaded.

## Registries

- `registry/ModItems.java`: metal flakes, powders, blends, raw ores, beads, ingots, vials, metalminds, unkeyed metalminds, spike blanks, charged spikes, god metal beads, tools, machines, ores, cache blocks.
- `registry/ModBlocks.java`: machines, ore blocks, ash deposit, Atium geode cluster, Lerasium cache, metal cache.
- `registry/ModEffects.java`: pewter drag, sensory overload, coppercloud, bronze seeking, Atium sight, emotional pressure.
- `registry/ModParticles.java`: metal line, coppercloud, Atium shadow.
- `registry/ModSounds.java`: sound event registrations for metal burn, push, pull, bronze pulse, and Lerasium consume.
- `registry/ModEntityTypes.java`: registers custom Metalborn mobs and the Steel Inquisitor boss using DeferredRegister.
- `registry/ModStructures.java`: registers the Kredik Shaw structure type and structure piece type so vanilla structure lookup can index it.
- `registry/ModMenus.java` and `registry/ModBlockEntities.java`: still no menu/block-entity inventories; machine blocks currently use direct right-click interactions.
- `registry/ModCreativeTabs.java`: exposes machines, ores, metals, vials, metalminds, spikes, and Metalborn spawn eggs.

## Items

- Real item behavior exists for:
  - `MetalVialItem`: drinkable, fills reserves, returns empty vial.
  - `LerasiumBeadItem`: consumable, grants Mistborn when enabled.
- Real behavior now exists for:
  - `MetalmindItem`: item NBT charge, capacity, owner/unkeyed state, store/tap/off cycling.
  - `HemalurgicSpikeItem`: charged spikes install permanently or equip into optional Curios Hemalurgic slots and grant powers with corruption.
  - `SpikeRemovalToolItem`: removes the latest installed spike, then Curios-equipped charged spikes, when config allows.
- Testing kits, guidebook, and raw metal components remain mostly informational/crafting pieces.

## Blocks

- `MetalArtsMachineBlock` now performs direct interactions:
  - metallurgy table grinds ingots into flakes
  - metalworking table makes metalminds
  - alloy furnace makes core alloys
  - spike press makes blank spikes
  - bind point table installs charged spikes
  - metalmind charging stand inspects/toggles metalmind use
- Machine blocks still do not open full menu-backed GUIs.
- `WellOfAscensionBlock` exists and handles the Well interaction event.

## Effects

- Effects are registered and usable.
- Current implementation uses vanilla status effects plus the custom markers listed above.

## Capabilities

- `MetalArtsData` is a persistent player capability storing:
  - Allomantic powers
  - Feruchemical powers
  - current reserves
  - burning/flaring metals
  - selected metal
  - simple metalmind charge map
  - corruption
  - cooldowns
  - Bronze pulse HUD data
  - Coppercloud state
- `MetalArtsProvider` serializes/deserializes the data.
- Clone/login/respawn/dimension sync events are present.
- Added to the capability:
  - per-metal Feruchemy store/tap modes
  - installed Hemalurgic spikes
  - temporary equipped Curios spike corruption
  - spike grant metadata
  - Well touched state

## Packets

- `MetalArtsNetwork` registers one clientbound full-sync packet and one serverbound action packet.
- `ClientboundMetalArtsSyncPacket` safely gates client handling through `DistExecutor`.
- `ServerboundMetalActionPacket` routes keybind actions to server-authoritative Allomancy.
- The existing full-sync packet now carries the expanded capability data. Feruchemy toggle and stop-all style server actions are routed through the shared action packet.

## Keybinds

- Registered: open menu, burn selected, stop burning, flare, push/pull, cycle selected, aluminum purge, time bubble.
- Added: Feruchemy toggle keybind routed through the server action packet.

## HUD

- `MetalArtsHudOverlay` draws selected metal, reserve bars, Bronze pulse text, and corruption.
- It is simple but functional.
- It does not yet show item metalmind charges, installed spikes, time bubble status, or a graphical radial menu.

## Commands

- Current `/metalarts` supports:
  - power get/setallomancy/setferuchemy/setfullborn
  - reserve fill/clear
  - metalmind fill
  - corruption get/set
  - godmetal give
  - debug bronze/anchors/bubbles
- Added:
  - `/metalarts burn`
  - `/metalarts stopburning`
  - `/metalarts hemalurgy ...`
  - `/metalarts debug capability`
  - `/metalarts debug place_kredik_shaw`
  - `/metalarts debug locate_kredik_shaw`
  - `/metalarts debug spawn_metalborn <role>`

## Worldgen and Structures

- `Kredik Shaw` is now a registered Minecraft structure with `worldgen/structure` and `worldgen/structure_set` JSON, so `/locate structure mistborn_metal_arts:kredik_shaw` should work after restart/reload.
- `MetalArtsWorldgen` no longer places Kredik Shaw manually; the registered structure is the only active Kredik Shaw generation path.
- Static assets and block textures exist for structure-themed blocks.
- `KredikShawBuilder` implements a procedural/template-style seven-layer fortress-city with gates, ramps, dense modules, spires, citadel, and Well chamber.
- `SmallStructureBuilder` implements Steel Ministry outposts, skaa hideouts, canal ruins, noble keeps, Atium caverns, Koloss camps, and Kandra dens.
- The Well of Ascension emits Bronze-detectable pulses and has a player interaction event.

## Assets and Models

- The asset folder is large and mostly complete:
  - item, block, GUI, HUD, particle, mob effect, entity, and armor textures
  - item models, block models, and blockstates
  - recipes, loot tables, tags, and a starter advancement set
- `scripts/check_assets.py` passes: 550 models, 642 textures, 19 particle definitions.
- Existing textures from the previous agent should be preserved unless individually replaced by the user’s asset direction. They are not blocking gameplay work.

## Tags

- Present:
  - `mistborn_metal_arts:metallic_blocks`
  - `mistborn_metal_arts:metallic_items`
  - `mistborn_metal_arts:metalminds`
  - `mistborn_metal_arts:hemalurgic_spikes`
  - `mistborn_metal_arts:god_metals`
  - `mistborn_metal_arts:pushable_entities`
  - `mistborn_metal_arts:pullable_entities`
- Added:
  - `push_pull_anchors`
  - `metal_armor`
  - structure spawn biome tags
  - `charged_hemalurgic_spikes` for Curios Hemalurgic slots

## What Currently Compiles

- `build/classes/java/main` contains 51 compiled classes for the current source tree, indicating a previous successful compile.
- After this audit, a direct Java compile using the generated Forge classpath passed for the edited source tree.
- Current Gradle execution cannot start inside this sandbox because Gradle’s single-use daemon needs a local socket and the environment denies it with `java.net.SocketException: Operation not permitted`.
- Because of that sandbox failure, the full Gradle build cannot be freshly confirmed until local socket/network permission is granted.

## What Currently Does Not Compile

- No Java source compile errors were observed in the direct compile check.
- The only observed build failure is the sandbox blocking Gradle daemon socket binding.

## Real Gameplay Already Present

- Drink metal vials to fill reserves.
- Consume Lerasium to become Mistborn.
- Start/stop/flare selected Allomantic metals through keybind packets.
- Reserve drain while burning.
- Pewter buffs and emergency survival.
- Tin night vision/entity hints while flaring.
- Coppercloud effect and particles.
- Bronze detects active power/corruption on nearby capability holders.
- Atium status effects and future-shadow particles.
- Iron/Steel force system moves metallic entities/items/projectiles or moves the player from anchored blocks.
- Basic time bubble effects exist through status effects, not global tick-rate changes.
- Store/tap first-pass Feruchemy through item-backed metalminds.
- Install/remove Hemalurgic spikes and suffer corruption drawbacks.
- Equip charged Hemalurgic spikes in optional Curios slots; those spikes grant powers and temporary corruption while worn.
- Locate, debug-place, or naturally generate Kredik Shaw and interact with the Well.
- Spawn and fight custom Metalborn mobs and the Steel Inquisitor boss.
- Use machine blocks for direct crafting/processing interactions.

## Placeholders or Thin Systems

- Full menu-backed machines are still deferred.
- Full jigsaw/template-pool structures for smaller locations are still deferred.
- Kandra contract/trade behavior is still deferred.
- Time bubble projectile-boundary handling remains a future refinement.
- Worn metalminds do not yet have Curios slot integration; they still use inventory/offhand fallback scanning.

## Preserve From Previous Work

- Forge entrypoint and registry layout.
- Existing Allomancy manager and metal-force helper.
- Metal enum and tag API.
- Player capability and network sync baseline.
- Vial and Lerasium item behavior.
- HUD/keybind client isolation.
- Existing static assets, generated models, recipes, loot tables, tags, and advancements.
- Asset checker and texture documentation pipeline.
