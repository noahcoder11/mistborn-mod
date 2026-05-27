# Current State Assessment

Date: 2026-05-15

## Overall State

The mod is now a playable Forge 1.20.1 gameplay foundation rather than a pure content shell. Core Allomancy, first-pass Feruchemy, first-pass Hemalurgy, Metalborn mobs, procedural structure placement, machine interactions, HUD sync, configs, commands, assets, and documentation are present.

The strongest systems are Allomancy, player capability persistence, vials/reserves, Iron/Steel movement, Pewter/Tin/Copper/Bronze, Lerasium, Atium, commands, assets, and the basic gameplay loop around metal use.

The biggest remaining production risks are runtime balancing, worldgen polish, full GUI-backed machines, advanced time bubble physics, Kandra contracts/trading, and expanding the smaller structures into full registered structure sets.

## Hemalurgy And Curios

Curios integration was the highest-risk area before this pass. Curios was marked mandatory, while common gameplay classes directly referenced Curios API classes. That meant the mod could fail hard without Curios and made Hemalurgy logic split between permanent capability spikes and visual Curios slots.

This pass fixed that by making Curios optional and moving Curios calls behind `CuriosCompat`. Charged spikes equipped in Curios Hemalurgic slots now grant powers and contribute temporary corruption while worn. The permanent install path still works without Curios.

Follow-up crash fix: the dev runtime was still using an old Curios userdev dependency that crashed during mixin application before the mod finished loading. The project now targets Curios `5.14.1+1.20.1`, requires that version or newer when Curios is present, and enables ForgeGradle mixin refmap remapping for dev runs.

## Verification

- Gradle build passed, including `jar` and `reobfJar`.
- Datagen passed with Curios loaded in the userdev runtime.
- Headless server launch reached the normal EULA stop with no mod loading crash.
- Asset checker passed: 550 models, 642 textures, 19 particle definitions.
- Resource JSON validation passed.

## Remaining Weak Spots

- Need in-game testing with Curios installed and absent.
- Curios support currently covers Hemalurgic spikes only; metalminds still use inventory/offhand fallback scanning.
- Machine GUIs are still direct interactions rather than real menus.
- Kredik Shaw is now a registered Minecraft structure and should work with `/locate structure mistborn_metal_arts:kredik_shaw`. Its active generator is the new seven-layer Hill of a Thousand Spires fortress-city; smaller structures are still procedural/event-based and use mod debug/testing commands rather than vanilla `/locate`.
- Hemalurgy still needs richer charging rituals, decay behavior, bind point progression, and mob-source safeguards.
