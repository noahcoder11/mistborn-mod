# Reference Texture Traits

These local files were inspected only for broad style traits. No reference PNGs were copied, traced, recolored, embedded, or imported into the mod asset tree.

## Summary

- PNG files scanned: 151
- 16x16 textures: 149
- Dimensions: 16x16 (149), 64x64 (2)
- Median unique colors in 16x16 item-like textures: 7.0
- Median contrast in 16x16 item-like textures: 171.2
- 16x16 item-like textures with transparent edge padding: 94 / 94
- Median unique colors in 16x16 block-like textures: 9

## Observed Broad Traits

- Most item assets use compact centered silhouettes on transparent backgrounds.
- Ingots, nuggets, raw chunks, and alloy blends are template-driven rather than individually ornate.
- Ores use vanilla-like stone/deepslate bases with restrained mineral clusters.
- Icons favor plain readable symbols with minimal color and strong contrast.
- Metal colors are differentiated by a few stable ramps instead of many noisy highlights.

## Applied To This Mod

- Kept all generated mod textures original and deterministic.
- Tightened ingot, nugget, powder, flake, bead, and icon templates around simpler 16x16 silhouettes.
- Reduced stone-block noise and shifted ores toward vanilla-style clusters.
- Documented this pass separately so future artists can repeat the same style audit without importing reference art.

## Category Counts

| Category | Files | Median Colors | Median Contrast |
| --- | ---: | ---: | ---: |
| `block` | 49 | 9 | 92.7 |
| `entity` | 3 | 60 | 157.4 |
| `icon` | 5 | 2 | 252.0 |
| `item` | 94 | 7.0 | 171.2 |
