# Expanded Cosmere Magic Systems: Mod Design Proposal

This document outlines the gameplay implementation of advanced Cosmere magic systems, translating lore concepts (Hemalurgy, Allomancy, Feruchemy, Savantism, and God Metals) into concrete, balanced, and engaging Minecraft mod mechanics.

---

## 1. Detailed Mechanics for Each System

### Hemalurgic Spiking Ritual
*   **Execution:** Players must place a victim on a **Hemalurgic Altar**, target a specific bind point using a specialized UI (or physical block orientation), and strike with a designated metal spike using a Mallet.
*   **Transfer Efficiency:** An "Instant Heart-to-Heart" transfer can be performed by dual-wielding an empty spike and a mallet, killing an entity, and immediately (within 3 seconds) using the charged spike on a recipient. This skips storage and grants 95%+ efficiency.

### Savantism
*   **Accumulation:** Burning a metal grants 1 Savant XP per second. Flaring grants 5 Savant XP per second.
*   **Degradation:** Not burning the metal for a full in-game day slowly decays Savant XP, preventing accidental low-level savantism.
*   **Thresholds:** Savantism unlocks at specific XP thresholds (e.g., Stage 1 at 10,000 XP, Stage 3 at 100,000 XP).
*   **Dependency:** At Stage 2+, being without the metal applies custom debuffs (e.g., `Tin Withdrawal`: Blindness, Slowness).

### God Metal Interactions
*   **Pure God Metals:** Consumable items that grant 3-minute temporary buffs (e.g., Raysium applies an aura that drains enemy health/Investiture to restore the player's reserves).
*   **Lerasium Alloys:** Require a specialized **Arcane Crucible** to smelt. Drinking permanently unlocks specific trait lines in the Spiritual DNA without adding Hemalurgic instability.

---

## 2. Stat Formulas

### Power Stacking (Diminishing Returns)
When stacking duplicate powers (e.g., multiple Steel spikes), the effective strength multiplier drops per additional spike.
*   **Formula:** `Total Strength = Base + ∑ (Spike_Strength * (0.8 ^ n))` where `n` is the number of prior spikes granting that exact power.
*   *Example:* Base 100%. Spike 1 (+100%), Spike 2 (+80%), Spike 3 (+64%). Total = 344%.

### Hemalurgic Decay
Spikes lose power exponentially based on their storage medium.
*   **Formula:** `Current Power = Initial Power * e^(-k * t)`
*   `t` = time in in-game days.
*   `k` (Decay Constant):
    *   Exposed to air: `k = 0.5` (Rapid loss)
    *   Dried Blood: `k = 0.1`
    *   Fresh Blood Tank: `k = 0.01`
    *   Aluminum Casing: `k = 0.0` (Perfect preservation)

### Soul Stability
A dynamic cap defining how close the player is to Soul Rejection.
*   **Formula:** `Stability = 100 - (Total Spikes * 10) - (Duplicate Spikes * 5) - (God Metal Spikes * 15) + Linchpin_Bonus(30) + Honor_Buffs`
*   If `Stability < 20`, major instability begins. If `Stability <= 0`, Soul Rejection occurs.

### Identity Contamination
Tracks foreign soul fragments.
*   **Formula:** `Contamination = ∑ (Spike_Base_Strength * 10)`
*   High Contamination (>100) causes hostile mobs to occasionally ignore the player (recognizing them as something else), but causes random screen-shake and hallucination events (spawning phantom hostile mobs that vanish when hit).

---

## 3. Crafting & Preservation Recipes

*   **Fresh Blood Vial:** Right-click a bleeding entity with a Glass Bottle.
*   **Blood Preservation Tank:** Crafted with Glass, Iron, and a Redstone block. Can hold 4 spikes. Requires feeding it Fresh Blood Vials periodically.
*   **Aluminum Casing:** 
    *   *Recipe:* Smithing Table -> Charged Spike + Aluminum Ingot + Aluminum Ingot.
    *   *Function:* Pauses decay completely. Must be stripped (shapeless crafting) before equipping, which resumes decay.
*   **Arcane Crucible (God Metal Alloy creation):** Multiblock structure requiring Lava, Netherite blocks, and a bellows system. Smelting Lerasium with another metal has a chance to fail unless the heat is perfectly maintained via a minigame UI.

---

## 4. Progression Tiers

*   **Tier 1: Safe / Natural (Early Game):** Player snaps naturally or finds a weak metalmind. Limited to one or two basic powers. Focus is on gathering raw metals and basic survival.
*   **Tier 2: The First Sins (Mid Game):** Player discovers Hemalurgy. They hunt specific mobs (or villagers/other players) for 1-2 basic spikes. Storage is crude, decay is high. Identity Contamination begins.
*   **Tier 3: The Ascendant (Late Game):** Player establishes Blood Tanks. They acquire a **Linchpin Spike** (dropped by an Inquisitor Boss). Stacking 5+ spikes becomes viable. Savantism is purposefully cultivated.
*   **Tier 4: Cosmere Scholar (Endgame):** Mixing God Metals. Forging Trellium spikes to hide from server admins/warden-like Shard entities. Creating Lerasium alloys for permanent, bloat-heavy omnipotence.

---

## 5. UI Suggestions

*   **Spiritual HUD (The "Spiritweb"):** Accessed via a hotkey. Shows a mandala-like diagram of the player.
    *   **Nodes:** Represent powers (glowing blue for natural, jagged red for spiked).
    *   **Cracks in the Glass:** A background texture that visually shatters as **Soul Stability** drops.
    *   **Whispers UI:** When **Identity Contamination** is high, faint, unreadable text occasionally flashes at the edges of the screen.
    *   **Savantism Glow:** When a metal nears Stage 3 Savantism, its icon in the radial menu continuously emits a blazing, particle-heavy glow.
*   **Spiritual Bloat Tooltip:** When holding a God Metal, a tooltip warns: *"Your soul is heavy. Adding this may sever existing ties."*

---

## 6. Combat Examples (Stacked Powers)

*   **The Inquisitor Juggernaut:** 
    *   *Build:* Pewter Savant (Stage 3) + 4 Iron Spikes (Physical Strength) + 2 Pewter Spikes (Physical Feruchemy).
    *   *Combat:* Base attack damage is 25. Health regenerates instantly. However, due to Savantism, if Pewter runs out, they suffer `Slowness IV` and `Weakness III`. They carry 10 Pewter vials just to stay alive.
*   **The Trellium Ghost:**
    *   *Build:* Trellium Spike + Stage 2 Tin Savantism + Bronze Spike.
    *   *Combat:* Can see enemies through walls perfectly (Tin) and detect their magic use (Bronze). The Trellium spike gives them the `Un-targetable` tag—tracking projectiles (like Shulker attacks or specific modded spells) physically swerve around them, and Sculk Sensors cannot hear them.

---

## 7. Failure States

*   **Soul Rejection (Stability <= 0):** Attempting to add a spike plays a shattering sound. The player takes 90% of their max health in unblockable "Spiritual Damage." The spike is violently ejected into the world as an item, and the player loses all magic access for 5 minutes.
*   **Linchpin Collapse:** If the linchpin spike is removed while holding 5+ other spikes, the player enters **Identity Collapse**. Controls are randomly inverted, vision is blurred, and spikes rapidly drop from their Curios slots one by one until stability normalizes.
*   **Savant Dependency:** Reaching Stage 4 Pewter Savantism means the player *must* burn Pewter. If their reserve hits 0, they immediately enter a "Coma" state (Blindness, Slowness 255, unable to jump or attack) until fed metal by an ally or dispenser.

---

## 8. Balance Recommendations

*   **Diminishing Returns are Mandatory:** The mathematical drop-off for duplicate powers prevents infinite scaling. A 10th Steel spike should grant less than a 1% boost.
*   **The Maintenance Cost:** Hemalurgy is cheap to acquire but expensive to maintain. Blood Tanks require constant upkeep. If a player logs off with a spike in a chest, it rots.
*   **Linchpin Bottleneck:** Hard-cap the number of linchpin spikes that generate in a world, or tie them to extremely difficult endgame boss fights (e.g., the Steel Inquisitor).

---

## 9. Integration Ideas

*   **The Inquisitor Boss:** Spawns at a rare `Kredik Shaw` structure. Drops a Linchpin Spike and 2-3 highly decayed, random Hemalurgic spikes.
*   **Morality/Karma:** Stealing spikes from Villagers or passive entities increases an unseen "Ruin's Gaze" stat. High Ruin's Gaze causes the environment to subtly decay around the player (crops die, iron golems become hostile).
*   **Spren/Shardblade Mod Compatibility:** If a player with high **Spiritual Bloat** (from Lerasium alloys) attempts to wield a living Shardblade, the blade deals continuous damage to the player and refuses to summon.
*   **Trellium Ores:** Only found in the Deep Dark, surrounded by corrupted Sculk. Mining it without Aluminum tools alerts the Warden instantly.

---

## 10. The Three Paths of Progression

The mod UI and advancements should clearly define these three distinct paths to power:

1.  **The Safe Path (Organic & Lerasium):** Slow. Rely on natural snapping, finding rare Lerasium, and mastering 1-2 metals. You will never be a god, but you will never lose control of your character. *Advancement Branch: "The Survivor".*
2.  **The Risky Path (Savantism & Minor Spiking):** Medium speed. Pushing your body to the limit with flares and 1-2 strategic spikes. Extremely powerful in specific niches, but requires constant resource management to feed dependencies. *Advancement Branch: "The Overcharged".*
3.  **The Forbidden Path (Heavy Hemalurgy & God Metals):** Fast and horrifying. Requires murdering entities, managing blood vats, and gambling with Soul Rejection. Grants godlike, unrestricted access to all powers, but one misstep results in complete character breakdown. *Advancement Branch: "Ruin's Pawn".*
