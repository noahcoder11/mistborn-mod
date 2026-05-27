# Balance

The first pass favors survival-friendly control over raw power.

- Iron and Steel forces are capped by range, force, mass category, and anchored-block logic.
- Steel movement can launch the player from anchors, but acceleration is clamped and depends on an active burning reserve.
- Pewter costs hunger over time and applies drag when reserves run out.
- Pewter's emergency survival consumes reserves and stops Pewter, preventing a free immortality loop.
- Tin gives useful awareness, but flared sensory drawbacks are reserved for event-specific second-pass hooks.
- Bronze only gives direction, distance, and broad metal hints in the first pass. Copperclouds block detection.
- Atium burns quickly and provides short combat assistance rather than invulnerability.
- Lerasium is configurable and defaults to rare/admin-gated acquisition.
- Duralumin consumes all compatible active reserves and applies cooldown/drawbacks.
- Aluminum deletes reserves, making it a defensive panic option with a real cost.
- PvP-affecting config defaults are conservative.

Feruchemy and Hemalurgy are intentionally staged because they need careful anti-loop rules for XP, healing, hunger, shared metalminds, and power theft.
