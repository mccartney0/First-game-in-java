#!/usr/bin/env python3
"""Utility to combine multiple sprite images into a single spritesheet."""

from __future__ import annotations

import argparse
import json
import math
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, List, Optional, Sequence

try:
    from PIL import Image
except ImportError as exc:  # pragma: no cover - defensive import guard
    raise SystemExit(
        "Pillow (PIL) é necessário para gerar spritesheets. Instale com `pip install pillow`."
    ) from exc


IMAGE_EXTENSIONS: Sequence[str] = (".png", ".bmp", ".gif", ".tga", ".jpg", ".jpeg")


@dataclass
class Sprite:
    """Representa um sprite individual a ser empacotado."""

    path: Path
    image: Image.Image
    width: int
    height: int
    x: int = 0
    y: int = 0

    @property
    def name(self) -> str:
        return self.path.stem


def load_sprites(paths: Iterable[Path]) -> List[Sprite]:
    sprites: List[Sprite] = []
    for path in paths:
        with Image.open(path) as img:
            image = img.convert("RGBA")
        sprites.append(Sprite(path=path, image=image, width=image.width, height=image.height))
    return sprites


def iter_sprite_paths(input_dir: Path, recursive: bool) -> Iterable[Path]:
    pattern = "**/*" if recursive else "*"
    for path in sorted(input_dir.glob(pattern)):
        if path.is_file() and path.suffix.lower() in IMAGE_EXTENSIONS:
            yield path


def pack_sprites(sprites: List[Sprite], max_width: Optional[int], padding: int) -> tuple[int, int]:
    x_cursor = 0
    y_cursor = 0
    row_height = 0
    sheet_width = 0

    for sprite in sprites:
        sprite_width = sprite.width
        sprite_height = sprite.height

        if max_width is not None and x_cursor > 0 and x_cursor + sprite_width > max_width:
            sheet_width = max(sheet_width, max(0, x_cursor - padding))
            x_cursor = 0
            y_cursor += row_height + padding
            row_height = 0

        sprite.x = x_cursor
        sprite.y = y_cursor

        x_cursor += sprite_width + padding
        row_height = max(row_height, sprite_height)

    sheet_width = max(sheet_width, x_cursor - padding if sprites else 0)
    sheet_height = y_cursor + row_height

    return sheet_width, sheet_height


def next_power_of_two(value: int) -> int:
    if value <= 0:
        return 1
    return 2 ** math.ceil(math.log2(value))


def create_sheet_image(width: int, height: int) -> Image.Image:
    return Image.new("RGBA", (width, height), (0, 0, 0, 0))


def paste_sprites(sheet: Image.Image, sprites: Sequence[Sprite]) -> None:
    for sprite in sprites:
        sheet.paste(sprite.image, (sprite.x, sprite.y), sprite.image)


def write_metadata(metadata_path: Path, sprites: Sequence[Sprite], sheet_path: Path) -> None:
    payload = {
        "image": sheet_path.name,
        "sprites": [
            {
                "name": sprite.name,
                "x": sprite.x,
                "y": sprite.y,
                "width": sprite.width,
                "height": sprite.height,
            }
            for sprite in sprites
        ],
    }
    metadata_path.write_text(json.dumps(payload, indent=2, ensure_ascii=False), encoding="utf-8")


def parse_args(argv: Optional[Sequence[str]] = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Gera um spritesheet a partir de imagens individuais.",
    )
    parser.add_argument("input", type=Path, help="Diretório contendo as imagens base")
    parser.add_argument("output", type=Path, help="Caminho do arquivo PNG gerado")
    parser.add_argument(
        "-m",
        "--metadata",
        type=Path,
        help="Caminho para salvar o JSON com metadados do spritesheet",
    )
    parser.add_argument(
        "-p",
        "--padding",
        type=int,
        default=2,
        help="Espaçamento em pixels entre os sprites",
    )
    parser.add_argument(
        "-w",
        "--max-width",
        type=int,
        help="Largura máxima da folha (quebra linhas automaticamente)",
    )
    parser.add_argument(
        "-r",
        "--recursive",
        action="store_true",
        help="Busca imagens recursivamente nos subdiretórios",
    )
    parser.add_argument(
        "--power-of-two",
        action="store_true",
        help="Ajusta a largura/altura finais para a próxima potência de dois",
    )
    return parser.parse_args(argv)


def validate_args(args: argparse.Namespace) -> None:
    if not args.input.exists():
        raise SystemExit(f"Diretório de entrada não encontrado: {args.input}")
    if not args.input.is_dir():
        raise SystemExit("O caminho de entrada precisa ser um diretório.")
    if args.padding < 0:
        raise SystemExit("Padding deve ser maior ou igual a zero.")
    if args.max_width is not None and args.max_width <= 0:
        raise SystemExit("max-width deve ser maior que zero.")


def main(argv: Optional[Sequence[str]] = None) -> int:
    args = parse_args(argv)
    validate_args(args)

    sprite_paths = list(iter_sprite_paths(args.input, args.recursive))
    if not sprite_paths:
        print("Nenhuma imagem encontrada para gerar spritesheet.", file=sys.stderr)
        return 1

    sprites = load_sprites(sprite_paths)
    sheet_width, sheet_height = pack_sprites(sprites, args.max_width, args.padding)

    if args.power_of_two:
        sheet_width = next_power_of_two(sheet_width)
        sheet_height = next_power_of_two(sheet_height)

    sheet = create_sheet_image(sheet_width, sheet_height)
    paste_sprites(sheet, sprites)

    output_path = args.output.with_suffix(".png")
    output_path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(output_path)

    if args.metadata:
        args.metadata.parent.mkdir(parents=True, exist_ok=True)
        write_metadata(args.metadata, sprites, output_path)

    print(f"Spritesheet salvo em: {output_path}")
    if args.metadata:
        print(f"Metadados salvos em: {args.metadata}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
