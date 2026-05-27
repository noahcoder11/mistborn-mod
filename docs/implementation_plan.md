# Implementation Plan

## Constraints

- Preserve Forge 1.20.1, Java 17, and current package structure.
- Keep existing assets, registries, configs, commands, and Allomancy behavior unless they are broken.
- Add gameplay incrementally around the existing capability/network/events.
- Keep server authority for powers and avoid client-only classes in common/server paths.

## Pass 1: Make Missing Player Systems Real

1. Extend `MetalArtsData` with Feruchemy store/tap modes and installed Hemalurgic spike records.
2. Add clean API methods for store/tap toggles, metalmind charge caps, spike install/remove, and corruption changes.
3. Extend network actions for Feruchemy toggle and stop all burning if needed.
4. Wire player tick events to Feruchemy and Hemalurgy managers.
5. Keep current sync packet as full NBT sync for reliability.

## Pass 2: Feruchemy

1. Store metalmind data on `ItemStack` NBT: metal, charge, capacity, owner, unkeyed flag.
2. Allow store/tap from carried metalminds.
3. Implement first-pass metals:
   - Iron: weight
   - Steel: speed
   - Pewter: strength
   - Gold: health
   - Copper: XP memory
   - Bendalloy: nutrition
   - Bronze: wakefulness
   - Tin: senses
   - Aluminum: identity/unkeyed support
   - Nicrosil: investiture boost
4. Clamp all rates and avoid duplication loops.

## Pass 3: Hemalurgy

1. Add spike NBT for charge metal, granted power, strength, decay, and installed state.
2. Let charged spikes install through use, with corruption cost and configurable max spikes.
3. Let removal tool remove the latest spike, damaging the player and respecting config.
4. Add commands for get/add/remove/charge/corruption.
5. Apply corruption drawbacks every tick.

## Pass 4: Commands and Testing Hooks

1. Add `/metalarts burn` and `/metalarts stopburning`.
2. Add `/metalarts debug capability`.
3. Add `/metalarts debug place_kredik_shaw`.
4. Add `/metalarts hemalurgy ...` command group.
5. Update docs and test report after implementation.

## Pass 5: Kredik Shaw and Well of Ascension

1. Add Well-themed blocks and block items using existing original textures/models.
2. Add a procedural debug builder for Kredik Shaw:
   - blackstone/deepslate palace footprint
   - towers/spires
   - courtyard and interior cache
   - hidden descent
   - Well chamber below
3. Add Well interaction event:
   - Bronze-detectable pulse
   - one-use/cooldown reward logic
   - optional Lerasium reward when config allows
4. Add configs for Kredik Shaw and Well.
5. Defer full natural generation/template pools to a later pass if needed.

## Pass 6: Mobs

1. Register at least one Metalborn mob with real power behavior.
2. Prefer a simple monster using vanilla AI plus custom Steelpush/Pewter goal logic.
3. Add renderer only on the client side.
4. Defer full boss/neutral/trader systems until the core gameplay compiles.

## Pass 7: Data and Validation

1. Add missing tags: `push_pull_anchors`, `metal_armor`, structure biome tags.
2. Add missing models/blockstates/lang for new blocks/items.
3. Run `scripts/check_assets.py`.
4. Run Gradle build when the sandbox allows daemon socket binding.
5. Record results in `docs/test_report.md`.

## Practical First-Pass Completion Target

- Preserve working Allomancy.
- Implement real Feruchemy for several metals.
- Implement installable Hemalurgic spikes with corruption.
- Implement command coverage for testing.
- Implement debug-placeable Kredik Shaw and a functioning Well chamber/event.
- Update docs to distinguish finished gameplay from future structure generation and full mob/boss work.

## Deferred Completion Pass Completed

- Registered the full first-pass Metalborn entity set and spawn eggs.
- Added a shared role-based Metalborn mob implementation with real combat powers.
- Added a Steel Inquisitor boss with boss bar, phase-like defenses, push/pull attacks, Atium dodges, and rare drops.
- Replaced tagged vanilla Kredik Shaw guards with custom Metalborn mobs.
- Added registered-structure placement for Kredik Shaw and retained smaller procedural structure hooks.
- Added direct right-click gameplay for machine blocks while full GUI-backed menus remain future work.
- Expanded debug commands for locating Kredik Shaw targets and spawning specific Metalborn roles.

## Still Deferred

- Replace event-based procedural structures with datapack-friendly jigsaw/template-pool structure sets.
- Add full machine menus, inventories, progress bars, and JEI-style recipe integration.
- Expand Kandra contracts/trading and structure key progression.
- Refine advanced time bubble projectile boundary simulation.
