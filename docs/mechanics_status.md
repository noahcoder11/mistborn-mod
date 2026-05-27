# Mechanics Status

## Working Before This Pass

| System | Status |
| --- | --- |
| Player capability | Persistent NBT capability exists and syncs to client. |
| Metal reserves | Working reserve map with caps and burn drain. |
| Metal vials | Drinkable and fill reserves. |
| Lerasium | Consumable bead grants Mistborn when enabled. |
| Iron/Steel | Reusable metal-force system moves metallic targets/player. |
| Pewter | Buffs, hunger cost, drag, emergency survival. |
| Tin | Night vision and flared entity highlighting. |
| Copper | Coppercloud effect, particles, Bronze hiding on capability holders. |
| Bronze | Detects nearby active Metallic Arts/corruption and syncs pulse HUD data. |
| Atium | Fast-draining future-sight effects and future-shadow particles. |
| Aluminum/Duralumin | Purge and burst behavior exist. |
| Cadmium/Bendalloy | First-pass status-effect bubbles exist; no global tick changes. |
| HUD | Selected metal, reserve bars, Bronze pulse, corruption text. |
| Commands | Power/reserve/metalmind/corruption/godmetal basics exist. |

## Implemented In This Pass

| System | Status |
| --- | --- |
| Feruchemy data | Added per-metal store/tap/off modes to the player capability and sync NBT. |
| Metalmind item storage | Metalminds now store charge/capacity/owner/unkeyed state on ItemStack NBT. |
| Metalmind use | Right-click or keybind cycles store, tap, and off for usable metals. |
| Feruchemical Iron | Store/tap affects weight-like fall and resistance behavior. |
| Feruchemical Steel | Store slows, tap grants speed and mining speed. |
| Feruchemical Tin | Store blinds, tap grants night vision. |
| Feruchemical Pewter | Store weakens, tap grants strength and mining speed. |
| Feruchemical Gold | Store drains vitality, tap heals with charge cost. |
| Feruchemical Copper | Stores and retrieves XP without duplicating XP. |
| Feruchemical Bendalloy | Stores hunger/nutrition and taps it back into food. |
| Feruchemical Bronze | First-pass wakefulness/night activity buff. |
| Feruchemical Aluminum | Storing can mark a metalmind unkeyed. |
| Feruchemical Nicrosil | Tapping can lightly boost an active reserve. |
| Hemalurgy data | Added installed spike list and spike serialization to capability. |
| Spike installation | Charged spike items install into the player, grant a metal power, and add corruption. |
| Spike removal | Spike removal tool removes the latest spike, hurts the player, and lowers corruption slightly. |
| Hemalurgic corruption | Tick effects now apply scaling drawbacks and Bronze-detectable corruption. |
| Hemalurgy commands | Added get/add/remove/charge/corruption command group. |
| Burn commands | Added `/metalarts burn` and `/metalarts stopburning`. |
| Debug commands | Added capability dump and Kredik Shaw debug placement. |
| Well blocks | Added Well of Ascension block, pulse core, sealed door, and ancient metal floor. |
| Well interaction | Interacting with the Well can grant reserves, temporary future sight, and Lerasium if enabled. |
| Bronze and Well | Bronze now detects nearby Well blocks as directional pulses. |
| Kredik Shaw | Replaced with a seven-layer circular fortress-city generator inspired by the Hill of a Thousand Spires schematic. |
| Metalborn guard | Kredik Shaw now spawns real Metalborn guards and a Steel Inquisitor at structure-controlled positions. |
| Tags | Added push/pull anchors, metal armor, and structure biome tags. |

## Implemented In Deferred Completion Pass

| System | Status |
| --- | --- |
| Custom entity registry | Registered Metalborn entity types for Coinshot Bandit, Lurcher Guard, Pewter Thug, Tineye Scout, Rioter, Soother, Seeker, Smoker, Atium Seer, Mistborn Assassin, Koloss, Kandra, and Steel Inquisitor. |
| Entity rendering | Client renderer maps each Metalborn entity to the existing original entity textures. |
| Spawn eggs | Added spawn eggs, lang entries, and spawn egg models for the full Metalborn entity set. |
| Metalborn AI | Shared Metalborn enemy class now applies role-specific Allomantic combat: steelpush, ironpull, pewter buffs, copperclouds, seeking, emotional pressure, Atium dodges, and Mistborn mixed attacks. |
| Steel Inquisitor boss | Implemented as a high-health boss with boss bar, push/pull attacks, seeking, Atium dodges, phase-like resistance, and rare god-metal/spike drops. |
| Kredik Shaw mobs | Kredik Shaw now spawns real Metalborn mobs and a Steel Inquisitor in the Well route instead of tagged vanilla zombies. |
| Natural structure placement | Kredik Shaw uses the registered vanilla structure path; manual chunk-load placement is disabled to prevent duplicate generation. |
| Smaller structures | Added procedural Steel Ministry outposts, skaa hideouts, canal ruins, noble keeps, Atium caverns, Koloss camps, and Kandra dens with loot and mobs. |
| Debug commands | Added `/metalarts debug locate_kredik_shaw` and `/metalarts debug spawn_metalborn <role>`. |
| Machine interactions | Machine blocks now perform direct gameplay interactions: grinding flakes, working metalminds, smelting alloys, pressing spikes, installing charged spikes, and toggling/inspecting metalminds. |
| Asset validation | Asset checker now includes Metalborn spawn eggs and entity texture coverage. |

## Implemented In Hemalurgy Curios Fix Pass

| System | Status |
| --- | --- |
| Curios dependency | Curios is now optional in `mods.toml`; normal gameplay paths no longer hard-reference Curios classes. |
| Curios bridge | Added a safe compatibility bridge that registers Curios handlers only when Curios is loaded. |
| Spike slots | Hemalurgic eye, heart, shoulder, and spine Curios slots now accept charged spikes through a charged-spike tag. |
| Curio spike powers | Charged spikes equipped in Curios slots grant their stored Allomantic/Feruchemical power while worn. |
| Curio corruption | Equipped Curios spikes contribute temporary corruption to HUD, Bronze detection, and Hemalurgy drawbacks. |
| Spike use flow | Right-clicking a charged spike tries Curios equip first when available, then falls back to permanent sneak-install behavior. |
| Spike removal | Spike removal tool removes permanent installed spikes first, then removes Curios-equipped charged spikes and returns them to inventory/drop. |

## Implemented In Structure Locate Fix Pass

| System | Status |
| --- | --- |
| Kredik Shaw registry | Added a real `mistborn_metal_arts:kredik_shaw` StructureType, StructurePieceType, structure JSON, and structure set JSON. |
| Vanilla locate | Kredik Shaw should now be discoverable with `/locate structure mistborn_metal_arts:kredik_shaw` after restarting/reloading the datapack. |
| Generation rarity | Reduced default Kredik Shaw spacing from 96 chunks to 48 chunks and broadened allowed spawn biomes. |
| Structure placement | Refactored Kredik Shaw builder so registered structure pieces place only inside the chunk bounding box during structure generation. |
| Debug locate | `/metalarts debug locate_kredik_shaw` now uses the registered structure lookup only, matching vanilla `/locate`. |

## Implemented In Hill Of A Thousand Spires Pass

| System | Status |
| --- | --- |
| Old layout removal | Replaced the compact palace/tunnel builder; the compatibility ID `mistborn_metal_arts:kredik_shaw` now points only at the new generator. |
| Seven terraces | Added circular layers with diameters matching the schematic: 512, 420, 340, 260, 190, 130, and 70 blocks. |
| Vertical scaling | The generator targets 510 blocks above base in tall dimensions and scales down gracefully in vanilla Overworld height. |
| City modules | Added reusable procedural modules for walls, gates, halls, warehouses, forges, archives, residences, courtyards, support columns, ramps, spires, citadel, and Well chamber. |
| Spire field | Added dense irregular spire placement with thick, medium, thin, extra-thin, and clustered variants. |
| Performance | Structure generation is clipped to the active structure chunk, avoiding full-fortress scans for every generated chunk. |

## Remaining Placeholders After This Pass

| System | Status |
| --- | --- |
| Full jigsaw structures | Kredik Shaw is procedural/template-driven rather than NBT or jigsaw; smaller structures are still procedural/event fallback generation. |
| Machine GUI screens | Machine blocks now have real interaction behavior, but full menu-backed inventories/progress bars are still deferred. |
| Advanced time bubbles | Still status-effect based; projectile boundary logic is deferred. |
| Kandra trading/contracts | Kandra exists as a neutral-ish entity, but contract/trade gameplay remains future work. |
| Structure keys/progression locks | Kredik Shaw can generate and includes Well progression, but keyed doors and multi-step lock progression are still thin. |
| Worn metalminds | Curios support currently focuses on Hemalurgic spikes; metalminds still use inventory/offhand fallback rules. |

## Deferred Unless Time Allows

- Optional future NBT/jigsaw detail packs for individual Kredik Shaw rooms.
- Full natural smaller structures.
- Full Metalborn mob family and Steel Inquisitor boss.
- GUI-backed machine processing.
- Advanced time-bubble projectile boundary handling.
