#!/usr/bin/env python3
"""Prepara derivados aprovados do Lote 1 sem tocar nos PNGs catalogados do artista."""

from __future__ import annotations

import hashlib
import json
import shutil
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "res/assets/incoming/lote1"
SPRITES = ROOT / "res/assets/generated/rpg_sprites"
WORLD = ROOT / "res/assets/generated/rpg_world/lote1"

HERO_SHEETS = (
    "hero_walk_down_sheet.png",
    "hero_walk_left_sheet.png",
    "hero_walk_right_sheet.png",
    "hero_walk_up_sheet.png",
    "hero_attack_down_sheet.png",
    "hero_attack_left_sheet.png",
    "hero_attack_right_sheet.png",
    "hero_attack_up_sheet.png",
)

WOLF_SHEETS = (
    "enemy_moss_wolf_walk_down_sheet.png",
    "enemy_moss_wolf_walk_left_sheet.png",
    "enemy_moss_wolf_walk_right_sheet.png",
    "enemy_moss_wolf_walk_up_sheet.png",
    "enemy_moss_wolf_attack_down_sheet.png",
    "enemy_moss_wolf_attack_left_sheet.png",
    "enemy_moss_wolf_attack_right_sheet.png",
    "enemy_moss_wolf_attack_up_sheet.png",
)

WORLD_FILES = (
    "mist_clearing_tileset.png",
    "wood_bridge_start.png",
    "wood_bridge_middle.png",
    "wood_bridge_end.png",
    "npc_commandant_ava_portrait.png",
)

BLOCKED_FILES = tuple(sorted(SOURCE.glob("boss_mist_titan_*_sheet.png")))


def file_hash(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def frame_prefix(sheet_name: str) -> str:
    suffix = "_sheet.png"
    if not sheet_name.endswith(suffix):
        raise ValueError(f"Nome de spritesheet não suportado: {sheet_name}")
    return sheet_name[: -len(suffix)]


def split_three_frames(sheet_name: str) -> list[str]:
    source = SOURCE / sheet_name
    with Image.open(source) as raw:
        image = raw.convert("RGBA")
    if image.width != 96 or image.height != 32:
        raise ValueError(f"{sheet_name}: esperado 96×32, recebido {image.width}×{image.height}")

    prefix = frame_prefix(sheet_name)
    outputs: list[str] = []
    for frame in range(3):
        output = SPRITES / f"{prefix}_{frame}.png"
        image.crop((frame * 32, 0, frame * 32 + 32, 32)).save(output, "PNG")
        outputs.append(output.name)
    return outputs


def copy_world_asset(file_name: str) -> str:
    source = SOURCE / file_name
    target = WORLD / file_name
    shutil.copy2(source, target)
    return str(target.relative_to(ROOT))


def main() -> None:
    if not SOURCE.is_dir():
        raise SystemExit(f"Pasta de entrada ausente: {SOURCE}")
    SPRITES.mkdir(parents=True, exist_ok=True)
    WORLD.mkdir(parents=True, exist_ok=True)

    derived_hero = [name for sheet in HERO_SHEETS for name in split_three_frames(sheet)]
    derived_wolf = [name for sheet in WOLF_SHEETS for name in split_three_frames(sheet)]

    # O primeiro frame de caminhada para baixo também mantém o fallback de sprite-base do herói alinhado ao ciclo.
    shutil.copy2(SPRITES / "hero_walk_down_0.png", SPRITES / "hero.png")
    copied_world = [copy_world_asset(name) for name in WORLD_FILES]

    catalog = {
        "schema": 1,
        "source": "Lote 1 do titular do projeto",
        "policy": "Derivados podem ser substituídos; arquivos em res/assets/incoming/lote1 nunca são alterados.",
        "approved": {
            "heroFrames": derived_hero,
            "enemyMossWolfFrames": derived_wolf,
            "worldAssets": copied_world,
        },
        "provisional": [
            "hero_walk_left_sheet.png",
            "hero_walk_right_sheet.png",
        ],
        "blocked": [path.name for path in BLOCKED_FILES],
        "sourceHashes": {
            path.name: file_hash(path)
            for path in sorted(SOURCE.glob("*.png"))
        },
    }
    (WORLD / "lote1_catalog.json").write_text(
        json.dumps(catalog, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    print(f"Frames do herói: {len(derived_hero)}")
    print(f"Frames do lobo: {len(derived_wolf)}")
    print(f"Assets de mundo: {len(copied_world)}")
    print(f"Boss bloqueado: {len(BLOCKED_FILES)} sheets")


if __name__ == "__main__":
    main()
