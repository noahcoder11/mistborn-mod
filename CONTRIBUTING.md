# Contributing

This is an unofficial, non-commercial fan-created project. Keep contributions original and avoid copied book text, official art, official symbols, official maps, covers, audio, logos, or other official assets.

## Code Style

- Java 17.
- Forge 1.20.1 conventions.
- Server-authoritative gameplay logic.
- Client-only rendering, HUD, and keybind code stays in `client/`.
- Shared gameplay state belongs in capabilities or synced packets, not client globals.
- Prefer `DeferredRegister` for registries.
- Prefer tags for metallic blocks, items, and entities.
- Keep configs server-readable and conservative by default for PvP and Hemalurgy.
- Keep TODOs specific and tied to extension points.

## Balance Style

- Every active power needs a resource cost.
- No permanent creative flight.
- No infinite healing, XP, hunger, or attribute loops.
- Strong powers need counterplay.
- Time bubble behavior must never alter the global server tick rate.
