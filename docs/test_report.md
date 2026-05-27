# Test Report

Date: 2026-05-15

## 2026-05-16 Kredik Shaw Natural Generation Fix

The square cutoff on naturally generated Kredik Shaw was traced to vanilla's structure reference radius being smaller than the new 512-block-wide palace. The structure now records its natural origin and fills missing loaded chunks in small persistent slices.

| Check | Result | Notes |
| --- | --- | --- |
| Direct Java compile | Passed | All `src/main/java` sources compile against cached Forge 1.20.1 dependencies with warnings only. |
| Gradle wrapper | Blocked in Codex sandbox | The wrapper attempted to write its lock file under `/Users/noah/.gradle`, which this sandbox cannot modify. A workspace-local Gradle cache then needed a network download, which is unavailable here. |

Manual in-game validation still needed: find a naturally generated `mistborn_metal_arts:kredik_shaw`, wait near the edges for loaded chunks to receive their slices, and confirm the circular footprint no longer truncates into a square.

## Commands Run

- `python3 scripts/check_assets.py`
- JSON validation over all `src/main/resources/**/*.json`
- `./gradlew runData --no-daemon --stacktrace`
- `./gradlew runServer --no-daemon --stacktrace`
- `./gradlew build --no-daemon --stacktrace`

## Results

| Check | Result | Notes |
| --- | --- | --- |
| Asset checker | Passed | 550 models, 642 textures, 19 particle definitions. |
| Resource JSON validation | Passed | All JSON files under `src/main/resources` parse successfully. |
| Datagen launch | Passed | Curios mixin refmap remapping is now configured for ForgeGradle userdev runs. |
| Server launch smoke test | Passed | Headless server reaches the normal EULA stop with no mod loading crash. |
| Gradle build | Passed | `build`, `jar`, and `reobfJar` completed successfully. |

## Gameplay Validation Status

The mod now passes compile, resource, datagen, asset, and headless server-load checks. Full in-game behavior still needs manual client testing.

## Implemented Test Paths

- `/metalarts power setallomancy <player> mistborn`
- `/metalarts power setferuchemy <player> <metal|full|none>`
- `/metalarts reserve fill <player> <metal> <amount>`
- `/metalarts burn <player> <metal>`
- `/metalarts stopburning <player> <metal|all>`
- `/metalarts hemalurgy get <player>`
- `/metalarts hemalurgy addspike <player> <metal> <power>`
- `/metalarts hemalurgy removespike <player> <slot>`
- `/metalarts hemalurgy chargespike <player> <metal> <power>`
- `/metalarts hemalurgy corruption <get|set|add> ...`
- `/metalarts debug capability <player>`
- `/metalarts debug place_kredik_shaw`
- `/metalarts debug locate_kredik_shaw`
- `/metalarts debug spawn_metalborn <role>`
- Vanilla: `/locate structure mistborn_metal_arts:kredik_shaw`

## Manual Runtime Tests Still Needed

- Drink vials and verify HUD reserve updates.
- Burn Iron/Steel and verify force behavior against anchors and item entities.
- Right-click metalminds and verify store/tap charge movement.
- Install/remove charged spikes and verify corruption effects.
- Debug-place the new seven-layer Kredik Shaw, climb the ramps/shafts, enter the lower chamber, and interact with the Well.
- Explore naturally generated Kredik Shaw and smaller structures in new overworld chunks.
- Restart/reload the world, run `/locate structure mistborn_metal_arts:kredik_shaw`, teleport to the result, and verify the registered structure generates only the new Hill of a Thousand Spires layout in new chunks.
- Burn Bronze near the Well and verify directional pulse data.
- Fight custom Coinshot Bandits, Lurcher Guards, Seekers, Smokers, Mistborn Assassins, Koloss, and the Steel Inquisitor.
- Use machine blocks to create flakes, alloys, metalminds, blank spikes, and bind charged spikes.
- With Curios installed, right-click a charged spike and verify it equips into a Hemalurgic slot, grants its power, adds temporary corruption, renders on the player, and can be removed with the spike removal tool.
- Without Curios installed, verify the mod still loads and charged spikes fall back to permanent sneak-install behavior.
