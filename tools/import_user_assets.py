#!/usr/bin/env python3
"""Importa assets visuais do Content Studio sem editar manualmente cada PNG.

Entrada padrão: res/assets/incoming/user_uploads/
Saída runtime:   res/assets/generated/
Saída de apoio:  res/assets/generated/references/ e res/assets/generated/atlas_cells/

O algoritmo usa apenas PIL: remove fundos conectados às bordas, preserva os
pixels internos dos sprites, recorta o envelope alfa e mantém a grade original
dos atlases para que AssetCatalog continue usando os mesmos índices.
"""
from __future__ import annotations

import json
import shutil
from pathlib import Path
from typing import Callable, Iterable

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
INPUT = ROOT / "res/assets/incoming/user_uploads"
GENERATED = ROOT / "res/assets/generated"


def find_asset(suffix: str) -> Path | None:
    matches = sorted(INPUT.glob(f"*{suffix}"))
    return matches[0] if matches else None


def rgba(path: Path) -> Image.Image:
    return Image.open(path).convert("RGBA")


def is_background_candidate(pixel: tuple[int, int, int, int]) -> bool:
    r, g, b, a = pixel
    if a <= 12:
        return True
    # Quadriculado branco/cinza típico dos exports de referência.
    if r >= 185 and g >= 185 and b >= 185 and max(r, g, b) - min(r, g, b) <= 26:
        return True
    # Chroma verde usado em referências de nave e portal.
    if g >= 60 and g > r * 1.16 and g > b * 1.08 and r < 155:
        return True
    # Magenta de proteção usado em alguns exports com transparência defeituosa.
    if r >= 160 and b >= 125 and g <= 115 and r + b >= 360:
        return True
    return False


def flood_remove_background(image: Image.Image) -> Image.Image:
    """Remove somente regiões de fundo conectadas à borda.

    Isso evita apagar brilhos brancos, azuis, roxos ou verdes que estejam
    protegidos pelo contorno do pixel art no interior do sprite.
    """
    source = image.convert("RGBA")
    width, height = source.size
    pixels = source.load()
    remove = bytearray(width * height)
    queue: list[tuple[int, int]] = []

    def enqueue(x: int, y: int) -> None:
        index = y * width + x
        if remove[index]:
            return
        if is_background_candidate(pixels[x, y]):
            remove[index] = 1
            queue.append((x, y))

    for x in range(width):
        enqueue(x, 0)
        enqueue(x, height - 1)
    for y in range(height):
        enqueue(0, y)
        enqueue(width - 1, y)

    cursor = 0
    while cursor < len(queue):
        x, y = queue[cursor]
        cursor += 1
        if x > 0:
            enqueue(x - 1, y)
        if x + 1 < width:
            enqueue(x + 1, y)
        if y > 0:
            enqueue(x, y - 1)
        if y + 1 < height:
            enqueue(x, y + 1)

    output = source.copy()
    out_pixels = output.load()
    for index, marked in enumerate(remove):
        if marked:
            x = index % width
            y = index // width
            out_pixels[x, y] = (0, 0, 0, 0)
    return output


def alpha_bbox(image: Image.Image) -> tuple[int, int, int, int] | None:
    return image.getchannel("A").getbbox()


def trim(image: Image.Image, padding: int = 0) -> Image.Image:
    box = alpha_bbox(image)
    if box is None:
        return image.copy()
    left, top, right, bottom = box
    left = max(0, left - padding)
    top = max(0, top - padding)
    right = min(image.width, right + padding)
    bottom = min(image.height, bottom + padding)
    return image.crop((left, top, right, bottom))


def save_png(image: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, "PNG", optimize=True)


def copy_reference(source: Path | None, relative: str) -> str | None:
    if source is None:
        return None
    destination = GENERATED / "references" / relative
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, destination)
    return str(destination.relative_to(ROOT))


def import_single(source: Path | None, destination: str, padding: int = 8,
                  remove_background: bool = True) -> str | None:
    if source is None:
        return None
    image = rgba(source)
    if remove_background:
        image = flood_remove_background(image)
    image = trim(image, padding)
    output = GENERATED / destination
    save_png(image, output)
    return str(output.relative_to(ROOT))


def import_atlas(source: Path | None, destination: str) -> str | None:
    if source is None:
        return None
    image = flood_remove_background(rgba(source))
    output = GENERATED / destination
    save_png(image, output)
    return str(output.relative_to(ROOT))


def split_atlas(source: Path | None, output_dir: str, prefix: str, columns: int,
                rows: int, names: Iterable[str]) -> list[str]:
    if source is None:
        return []
    image = flood_remove_background(rgba(source))
    out: list[str] = []
    names_list = list(names)
    cell_width = image.width // columns
    cell_height = image.height // rows
    for index, name in enumerate(names_list):
        if index >= columns * rows:
            break
        column = index % columns
        row = index // columns
        cell = image.crop((column * cell_width, row * cell_height,
                           (column + 1) * cell_width if column + 1 < columns else image.width,
                           (row + 1) * cell_height if row + 1 < rows else image.height))
        cell = trim(cell, 8)
        destination = GENERATED / output_dir / f"{prefix}{name}.png"
        save_png(cell, destination)
        out.append(str(destination.relative_to(ROOT)))
    return out


def add_record(records: list[dict[str, object]], source: Path | None, category: str,
               runtime: bool, outputs: list[str], usage: str) -> None:
    if source is None:
        return
    records.append({
        "source": source.name,
        "category": category,
        "runtime_loaded": runtime,
        "outputs": outputs,
        "usage": usage,
    })


def main() -> None:
    if not INPUT.is_dir():
        raise SystemExit(f"Diretório de entrada não encontrado: {INPUT}")

    records: list[dict[str, object]] = []

    weapon_specs = [
        ("blaster", "pasted_file_vTJGiV_blaster_clean.png", "weapons/blaster_clean.png"),
        ("ion_rifle", "pasted_file_eGVrkj_ion_rifle_clean.png", "weapons/ion_rifle_clean.png"),
        ("scatter_cannon", "pasted_file_5Xj8uT_scatter_cannon_clean.png", "weapons/scatter_cannon_clean.png"),
        ("fusion_lance", "pasted_file_PSgahn_fusion_lance_clean.png", "weapons/fusion_lance_clean.png"),
        ("void_mortar", "pasted_file_pFl32X_void_mortar_clean.png", "weapons/void_mortar_clean.png"),
    ]
    for weapon, suffix, destination in weapon_specs:
        source = find_asset(suffix)
        output = import_single(source, destination, padding=12)
        add_record(records, source, "weapon_icon", True, [output] if output else [],
                   f"AssetCatalog.weaponIcon({weapon.upper()}) e HUD/seleção de arma")

    effect_specs = [
        ("fusion_lance", "pasted_file_dskvdQ_fusion_lance.png"),
        ("ion_rifle", "pasted_file_sm9Uux_ion_rifle.png"),
        ("scatter_cannon", "pasted_file_y4UOo1_scatter_cannon.png"),
        ("blaster", "pasted_file_i1drWm_blaster.png"),
        ("void_mortar", "pasted_file_LxubVF_void_mortar.png"),
    ]
    for weapon, suffix in effect_specs:
        source = find_asset(suffix)
        output = import_single(source, f"effects/{weapon}_shot.png", padding=4)
        add_record(records, source, "weapon_shot_effect", False, [output] if output else [],
                   "Biblioteca de efeito de disparo; pronta para muzzle flash/trail, não carregada hoje pelo runtime")

    scout = find_asset("pasted_file_vIjJqn_scout_ref.png")
    scout_output = import_single(scout, "enemies/scout_ref.png", padding=6)
    add_record(records, scout, "enemy_sprite", True, [scout_output] if scout_output else [],
               "AssetCatalog.enemySprite(SCOUT) e fallback de variantes")

    portal = find_asset("pasted_file_XvdE6W_dungeon_portal.png")
    portal_output = import_single(portal, "world/dungeon_portal.png", padding=8)
    add_record(records, portal, "world_portal", True, [portal_output] if portal_output else [],
               "AssetCatalog.dungeonPortal(); disponível para cenas de dungeon")

    companion = find_asset("pasted_file_XPjC2J_companion_set_clean.webp")
    companion_atlas = import_atlas(companion, "companions/companion_set_clean.png")
    companion_cells = split_atlas(companion, "atlas_cells/companions", "companion_", 3, 1,
                                  ["scout", "shield_bot", "fairy"])
    add_record(records, companion, "companion_atlas", True,
               [p for p in [companion_atlas] if p] + companion_cells,
               "AssetCatalog.companionSprite(); 3 colunas: SCOUT, SHIELD_BOT e FAIRY")

    enemy = find_asset("pasted_file_M71Xov_enemy_set_clean.webp")
    enemy_atlas = import_atlas(enemy, "enemies/enemy_set_clean.png")
    enemy_cells = split_atlas(enemy, "atlas_cells/enemies", "enemy_", 3, 2,
                              ["scout", "bomber", "shielder", "artillery", "swarm", "guardian"])
    add_record(records, enemy, "enemy_atlas", True,
               [p for p in [enemy_atlas] if p] + enemy_cells,
               "AssetCatalog.enemySprite(); grade 3×2 usada como fallback por variante")

    # Versões com alpha/trilhas e pranchas promocionais ficam disponíveis como
    # referências, mas não entram no carregamento da partida automaticamente.
    for suffix, target, usage in [
        ("pasted_file_oWYxmX_companion_set.webp", "companion_set_effects.webp", "Referência de direção de arte"),
        ("pasted_file_bDlYOO_enemy_set.webp", "enemy_set_effects.webp", "Referência de direção de arte"),
        ("pasted_file_viQa0x_visual_target.webp", "visual_target.webp", "Imagem-alvo de composição; não é sprite de runtime"),
    ]:
        source = find_asset(suffix)
        output = copy_reference(source, target)
        add_record(records, source, "reference", False, [output] if output else [], usage)

    manifest = {
        "schema": "first-game-user-assets/v1",
        "input": str(INPUT.relative_to(ROOT)),
        "output": str(GENERATED.relative_to(ROOT)),
        "notes": [
            "Os arquivos originais permanecem intactos em res/assets/incoming/user_uploads.",
            "Os atlases mantêm a dimensão e a grade; células individuais são geradas apenas para inspeção e reuso futuro.",
            "Os efeitos de disparo são importados, mas o jogo atual ainda usa desenho procedural em BulletShoot.",
        ],
        "assets": records,
    }
    manifest_path = GENERATED / "user_asset_manifest.json"
    manifest_path.parent.mkdir(parents=True, exist_ok=True)
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"manifest": str(manifest_path.relative_to(ROOT)), "records": len(records)}, ensure_ascii=False))


if __name__ == "__main__":
    main()
