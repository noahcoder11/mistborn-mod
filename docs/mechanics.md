# Mechanics

Unofficial fan-created mod inspired by Brandon Sanderson's Mistborn. No affiliation or endorsement.

## Allomancy

Players fill metal reserves by drinking metal vials, then burn metals through keybinds or commands. Burning drains reserves every server tick. Flaring increases output and burn cost.

Implemented first-pass metals:

- Iron: pulls metallic targets. Light targets move toward the player; anchored or heavy targets pull the player toward them.
- Steel: pushes metallic targets. Light targets move away; anchored blocks push the player away, enabling capped Coinshot-style jumps.
- Tin: grants night vision and flared awareness by briefly outlining nearby living entities.
- Pewter: grants strength, speed, jump, mining speed, resistance, extra hunger exhaustion, drag on burnout, and one emergency survival check when reserves are high.
- Copper: creates a coppercloud effect that hides active Metal Arts from Bronze seeking and shows a simple boundary particle ring.
- Bronze: detects nearby burning metals, spikes/corruption, and active god-metal effects unless blocked by copperclouds.
- Atium: rare, fast-burning future-sight aid that highlights threats and reduces some incoming damage. It is intentionally short-lived.
- Aluminum: purges reserves and cancels emotional pressure or overload effects.
- Duralumin: consumes active reserves for a burst effect, then enters cooldown.

Second-pass or partial metals:

- Zinc and Brass: configured and scaffolded for mob aggression and calming behavior.
- Gold and Electrum: configured for self-analysis and danger prediction.
- Cadmium and Bendalloy: first-pass stable status-effect bubble hooks exist; advanced projectile boundary handling is planned.
- Chromium and Nicrosil: configured for leeching and nicrobursting.
- Lerasium: consumed as an item to grant Mistborn powers when enabled.

## Feruchemy

Metalminds now use item NBT for charge, capacity, owner, and unkeyed state. Right-clicking a usable metalmind cycles store, tap, and off. First-pass Feruchemy is implemented for Iron, Steel, Tin, Pewter, Gold, Copper, Bendalloy, Bronze, Aluminum, and Nicrosil with conservative caps and anti-duplication rules.

## Hemalurgy

Charged spikes can be installed permanently through use while sneaking, through the Bind Point Table, or temporarily worn in Curios Hemalurgic slots when Curios is installed. Installed or worn charged spikes grant their stored Allomantic/Feruchemical power and add corruption drawbacks. The spike removal tool removes permanent spikes first, then Curios-equipped spikes.

Curios is optional. Without Curios, charged spikes still work through the permanent install path and Bind Point Table. With Curios, only charged spikes can occupy Hemalurgic slots; blank spikes stay crafting/ritual components.

## Metallic Tags

Push/pull uses tags first:

- `mistborn_metal_arts:metallic_blocks`
- `mistborn_metal_arts:metallic_items`
- `mistborn_metal_arts:pushable_entities`
- `mistborn_metal_arts:pullable_entities`

Armor, tiered tools, item entities, and projectiles are treated as metallic fallback targets for gameplay feel.
