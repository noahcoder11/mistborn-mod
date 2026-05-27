#!/usr/bin/env python3
"""Validate Mistborn: Metal Arts resource references and basic image quality."""

from __future__ import annotations

import json
import sys
from pathlib import Path

from clean_texture_pass import ALLOMANTIC, ASSETS, FERUCHEMICAL, METAL_IDS, MODID, TEXTURES, metrics_for

ROOT = Path(__file__).resolve().parents[1]

REGISTERED_BLOCKS = [
    "metallurgy_table", "metalworking_table", "alloy_furnace", "spike_press", "bind_point_table", "metalmind_charging_stand",
    "atium_geode_cluster", "lerasium_cache_block", "metal_cache", "zinc_ore", "deepslate_zinc_ore",
    "tin_ore", "deepslate_tin_ore", "aluminum_ore", "deepslate_aluminum_ore", "chromium_ore",
    "deepslate_chromium_ore", "cadmium_ore", "deepslate_cadmium_ore", "ash_deposit",
    "nickel_ore", "deepslate_nickel_ore", "silver_ore", "deepslate_silver_ore", "lead_ore", "deepslate_lead_ore",
    "well_of_ascension_block", "well_pulse_core", "sealed_well_door", "ancient_metal_floor",
]
REGISTERED_PARTICLES = ["metal_line", "coppercloud", "atium_shadow"]
REGISTERED_EFFECTS = ["pewter_drag", "sensory_overload", "coppercloud", "bronze_seeking", "atium_sight", "emotional_pressure", "hemalurgic_corruption"]
REGISTERED_ENTITIES = [
    "coinshot_bandit", "lurcher_guard", "pewter_thug", "tineye_scout", "rioter", "soother", "seeker",
    "smoker", "atium_seer", "mistborn_assassin", "koloss", "kandra", "steel_inquisitor",
]


def registered_items() -> list[str]:
    items = [
        "empty_glass_vial", "mixed_metal_vial", "allomancer_testing_kit", "feruchemist_testing_kit",
        "metal_arts_guidebook", "spike_removal_tool", "guide", "coin_pouch", "coinshot_coin", "metallic_coin",
    ]
    items.extend(f"{entity}_spawn_egg" for entity in REGISTERED_ENTITIES)
    items.extend(REGISTERED_BLOCKS)
    for metal in METAL_IDS:
        items.extend([
            f"{metal}_flakes", f"{metal}_powder", f"{metal}_bead", f"{metal}_ingot", f"{metal}_nugget",
            f"{metal}_spike", f"charged_{metal}_spike", f"decaying_{metal}_spike", f"{metal}_blend",
            f"raw_{metal}_ore", f"{metal}_block",
        ])
    for metal in ALLOMANTIC:
        items.append(f"{metal}_vial")
    for metal in FERUCHEMICAL:
        items.extend([f"{metal}_metalmind", f"{metal}_mind", f"unkeyed_{metal}_metalmind", f"unkeyed_{metal}_mind"])
    return sorted(set(items))


def load_json(path: Path):
    try:
        return json.loads(path.read_text())
    except Exception as exc:
        return {"__error__": str(exc)}


def texture_path(ref: str) -> Path | None:
    if ref.startswith("#"):
        return None
    namespace, _, value = ref.partition(":")
    if not value:
        namespace, value = MODID, namespace
    if namespace != MODID:
        return None
    return TEXTURES / f"{value}.png"


def model_path(ref: str) -> Path | None:
    namespace, _, value = ref.partition(":")
    if not value:
        namespace, value = MODID, namespace
    if namespace != MODID:
        return None
    return ASSETS / "models" / f"{value}.json"


def model_textures(model: Path) -> list[Path]:
    data = load_json(model)
    result = []
    for ref in data.get("textures", {}).values():
        if isinstance(ref, str):
            path = texture_path(ref)
            if path:
                result.append(path)
    return result


def has_missing_texture_pattern(metrics) -> bool:
    # Minecraft's missing texture is a magenta/black checker. This catches an
    # accidental baked-in checker without rejecting legitimate red/purple art.
    try:
        from clean_texture_pass import read_png
        _w, _h, pixels = read_png(TEXTURES / metrics.path)
    except Exception:
        return False
    total = magenta = black = 0
    for row in pixels:
        for r, g, b, a in row:
            if a <= 8:
                continue
            total += 1
            if r > 220 and b > 220 and g < 80:
                magenta += 1
            if r < 40 and g < 40 and b < 40:
                black += 1
    return total > 0 and magenta / total > 0.25 and black / total > 0.25


def main() -> int:
    missing = []
    bad_json = []
    failures = []
    warnings = []

    for model in (ASSETS / "models").rglob("*.json"):
        data = load_json(model)
        if "__error__" in data:
            bad_json.append((model, data["__error__"]))
            continue
        parent = data.get("parent")
        if parent and parent.startswith(MODID + ":"):
            path = model_path(parent)
            if path and not path.exists():
                missing.append((model, "parent model", path))
        for key, ref in data.get("textures", {}).items():
            path = texture_path(ref)
            if path and not path.exists():
                missing.append((model, f"texture {key}", path))

    for state in (ASSETS / "blockstates").glob("*.json"):
        data = load_json(state)
        if "__error__" in data:
            bad_json.append((state, data["__error__"]))
            continue
        variants = data.get("variants", {})
        for variant in variants.values():
            entries = variant if isinstance(variant, list) else [variant]
            for entry in entries:
                ref = entry.get("model") if isinstance(entry, dict) else None
                if ref:
                    path = model_path(ref)
                    if path and not path.exists():
                        missing.append((state, "blockstate model", path))

    for particle in (ASSETS / "particles").glob("*.json"):
        data = load_json(particle)
        if "__error__" in data:
            bad_json.append((particle, data["__error__"]))
            continue
        for ref in data.get("textures", []):
            path = texture_path(ref)
            if path and not path.exists():
                missing.append((particle, "particle texture", path))

    for item in registered_items():
        model = ASSETS / "models/item" / f"{item}.json"
        if not model.exists():
            missing.append((Path("registry"), f"registered item model {item}", model))
        else:
            parent = load_json(model).get("parent", "")
        if model.exists() and not model_textures(model) and not parent.startswith(f"{MODID}:block/") and parent != "minecraft:item/template_spawn_egg":
            failures.append(f"Registered item model has no texture: {model}")

    for block in REGISTERED_BLOCKS:
        for kind, path in [
            ("blockstate", ASSETS / "blockstates" / f"{block}.json"),
            ("block model", ASSETS / "models/block" / f"{block}.json"),
            ("block item model", ASSETS / "models/item" / f"{block}.json"),
        ]:
            if not path.exists():
                missing.append((Path("registry"), f"registered block {kind} {block}", path))

    for particle in REGISTERED_PARTICLES:
        path = ASSETS / "particles" / f"{particle}.json"
        if not path.exists():
            missing.append((Path("registry"), f"registered particle definition {particle}", path))

    for effect in REGISTERED_EFFECTS:
        path = TEXTURES / "mob_effect" / f"{effect}.png"
        if not path.exists():
            missing.append((Path("registry"), f"registered effect icon {effect}", path))

    for entity in REGISTERED_ENTITIES:
        path = TEXTURES / "entity" / f"{entity}.png"
        if not path.exists():
            missing.append((Path("registry"), f"registered entity texture {entity}", path))

    texture_count = 0
    for png in TEXTURES.rglob("*.png"):
        texture_count += 1
        metrics = metrics_for(png, TEXTURES)
        if metrics.path == "gui/blank.png":
            continue
        hard_flags = [flag for flag in metrics.flags if flag in {"fully transparent", "flat single-color", "dimension not divisible by 16"}]
        if hard_flags:
            failures.append(f"{metrics.path}: {', '.join(hard_flags)}")
        if has_missing_texture_pattern(metrics):
            failures.append(f"{metrics.path}: looks like a missing-texture checker")
        for flag in metrics.flags:
            if flag not in {"fully transparent", "flat single-color", "dimension not divisible by 16"}:
                warnings.append(f"{metrics.path}: {flag} ({metrics.width}x{metrics.height}, colors={metrics.unique_colors}, contrast={metrics.contrast})")

    if bad_json or missing or failures:
        print("Asset check failed.")
        for path, err in bad_json:
            print(f"BAD JSON: {path}: {err}")
        for source, kind, target in missing:
            print(f"MISSING: {source} -> {kind}: {target}")
        for failure in failures:
            print(f"FAIL: {failure}")
        for warning in warnings[:80]:
            print(f"WARN: {warning}")
        if len(warnings) > 80:
            print(f"WARN: {len(warnings) - 80} additional warnings")
        return 1

    model_count = len(list((ASSETS / "models").rglob("*.json")))
    particle_count = len(list((ASSETS / "particles").glob("*.json")))
    print(f"Asset check passed: {model_count} models, {texture_count} textures, {particle_count} particle definitions.")
    if warnings:
        print(f"Quality warnings: {len(warnings)}")
        for warning in warnings[:40]:
            print(f"WARN: {warning}")
        if len(warnings) > 40:
            print(f"WARN: {len(warnings) - 40} additional warnings")
    return 0


if __name__ == "__main__":
    sys.exit(main())
