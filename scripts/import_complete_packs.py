"""Cataloga assets aprovados e gera derivados de runtime sem alterar os ZIPs nem a quarentena."""

from __future__ import annotations

import hashlib
import json
import shutil
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
REVIEW_ROOT = ROOT / "build" / "asset-review" / "complete-packs"
INCOMING = ROOT / "res" / "assets" / "incoming" / "complete-pack"
SPRITES = ROOT / "res" / "assets" / "generated" / "rpg_sprites"
WORLD = ROOT / "res" / "assets" / "generated" / "rpg_world" / "complete-pack"

EXCLUDED_NAMES = {"preview_contact_sheet.png", "complete_packs_contact_sheet.png"}
WORLD_RUNTIME_NAMES = {
    "mist_clearing_tileset.png",
    "wood_bridge_start.png",
    "wood_bridge_middle.png",
    "wood_bridge_end.png",
    "pine_tree.png",
    "ruin_arch.png",
    "ruin_pillar.png",
    "camp_wagon.png",
    "campfire.png",
    "chest.png",
}


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def source_pngs() -> list[Path]:
    return sorted(
        path for path in REVIEW_ROOT.rglob("*.png")
        if path.name not in EXCLUDED_NAMES and "contact_sheet" not in path.name
    )


def safe_relative(path: Path) -> Path:
    relative = path.relative_to(REVIEW_ROOT)
    return Path(*(part.replace(" ", "_") for part in relative.parts))


def catalog_sources(paths: list[Path]) -> dict[str, str]:
    copied: dict[str, str] = {}
    for source in paths:
        target = INCOMING / safe_relative(source)
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, target)
        copied[str(safe_relative(source))] = sha256(source)
    return copied


def sheet_layout(image: Image.Image) -> tuple[int, int] | None:
    if image.height == 32 and image.width % 32 == 0:
        return (32, image.width // 32)
    if image.height == 96 and image.width % 96 == 0:
        return (96, image.width // 96)
    return None


def split_sheet(source: Path) -> list[str]:
    with Image.open(source) as raw:
        image = raw.convert("RGBA")
    layout = sheet_layout(image)
    if layout is None:
        return []

    cell_size, frame_count = layout
    prefix = source.name.removesuffix("_sheet.png")
    outputs: list[str] = []
    for index in range(frame_count):
        output = SPRITES / f"{prefix}_{index}.png"
        image.crop((index * cell_size, 0, (index + 1) * cell_size, cell_size)).save(output, "PNG")
        outputs.append(output.name)
    return outputs


def copy_world_asset(source: Path, seen_names: set[str]) -> str | None:
    if source.name not in WORLD_RUNTIME_NAMES or source.name in seen_names:
        return None
    target = WORLD / source.name
    shutil.copy2(source, target)
    seen_names.add(source.name)
    return str(target.relative_to(ROOT))


def main() -> None:
    if not REVIEW_ROOT.is_dir():
        raise SystemExit(f"Quarentena ausente: {REVIEW_ROOT}")

    paths = source_pngs()
    if not paths:
        raise SystemExit("Nenhum PNG aprovado encontrado na quarentena")

    INCOMING.mkdir(parents=True, exist_ok=True)
    SPRITES.mkdir(parents=True, exist_ok=True)
    WORLD.mkdir(parents=True, exist_ok=True)

    source_hashes = catalog_sources(paths)
    frames: dict[str, list[str]] = {}
    world_assets: list[str] = []
    seen_world_names: set[str] = set()

    for source in paths:
        if source.name.endswith("_sheet.png"):
            derived = split_sheet(source)
            if derived:
                frames[source.name.removesuffix("_sheet.png")] = derived
        copied = copy_world_asset(source, seen_world_names)
        if copied:
            world_assets.append(copied)

    if "hero_walk_down" in frames:
        shutil.copy2(SPRITES / "hero_walk_down_0.png", SPRITES / "hero.png")

    catalog = {
        "schema": 2,
        "source": "Pacotes completo e complementar do titular do projeto",
        "policy": "Arquivos em res/assets/incoming/complete-pack são cópias catalogadas e nunca são editados; somente derivados podem ser regenerados.",
        "approval": {
            "excludedPreviews": sorted(EXCLUDED_NAMES),
            "actorAndEffectFrames": frames,
            "worldRuntimeAssets": world_assets,
        },
        "sourceHashes": source_hashes,
    }
    (WORLD / "complete_pack_catalog.json").write_text(
        json.dumps(catalog, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )

    print(f"Fontes catalogadas: {len(paths)}")
    print(f"Sheets divididas: {len(frames)}")
    print(f"Frames gerados: {sum(len(values) for values in frames.values())}")
    print(f"Assets de mundo copiados: {len(world_assets)}")


if __name__ == "__main__":
    main()
