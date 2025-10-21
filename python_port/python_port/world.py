"""Carregamento de mapas e gerenciamento de tiles."""
from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Dict, Iterator, List, Sequence

from PIL import Image

from . import settings


@dataclass(frozen=True)
class Tile:
    x: int
    y: int
    walkable: bool
    color: tuple[int, int, int]

    @property
    def rect(self) -> tuple[int, int, int, int]:
        return (
            self.x * settings.TILE_SIZE,
            self.y * settings.TILE_SIZE,
            settings.TILE_SIZE,
            settings.TILE_SIZE,
        )


@dataclass(frozen=True)
class SpawnInstruction:
    kind: str
    position: tuple[int, int]
    variant: str | None = None


class World:
    """Representa o mapa carregado a partir de um arquivo PNG."""

    def __init__(self, width: int, height: int, tiles: Sequence[Tile], spawns: List[SpawnInstruction], player_spawn: tuple[int, int]):
        self.width = width
        self.height = height
        self.tiles = list(tiles)
        self._grid: Dict[tuple[int, int], Tile] = {(tile.x, tile.y): tile for tile in self.tiles}
        self.spawns = spawns
        self.player_spawn = player_spawn

    @classmethod
    def from_level(cls, level: int, *, resources_path: Path | None = None) -> "World":
        resources = resources_path or settings.RESOURCES_PATH
        image_path = resources / settings.MAP_PATTERN.format(level=level)
        if not image_path.exists():
            raise FileNotFoundError(f"Mapa não encontrado: {image_path}")

        image = Image.open(image_path).convert("RGBA")
        width, height = image.size
        pixels = image.load()

        tiles: List[Tile] = []
        spawns: List[SpawnInstruction] = []
        player_spawn = (0, 0)

        def spawn(kind: str, x: int, y: int, variant: str | None = None) -> None:
            spawns.append(SpawnInstruction(kind, (x * settings.TILE_SIZE, y * settings.TILE_SIZE), variant))

        for y in range(height):
            for x in range(width):
                r, g, b, a = pixels[x, y]
                walkable = True
                tile_color = settings.COLOR_FLOOR

                color_value = (r << 16) + (g << 8) + b

                if color_value == 0xFFFFFFFF:
                    walkable = False
                    tile_color = settings.COLOR_WALL
                elif color_value == 0xFF808080:
                    walkable = False
                    tile_color = (110, 110, 110)
                elif color_value == 0xFF0026FF:
                    player_spawn = (x * settings.TILE_SIZE, y * settings.TILE_SIZE)
                elif color_value in ENEMY_COLOR_TABLE:
                    walkable = True
                    spawn("enemy", x, y, ENEMY_COLOR_TABLE[color_value])
                elif color_value == 0xFFFF6A00:
                    spawn("weapon", x, y)
                elif color_value == 0xFF4CFF00:
                    spawn("lifepack", x, y)
                elif color_value == 0xFFFFD800:
                    spawn("ammo", x, y)
                elif color_value == 0xFF8E24AA:
                    spawn("shield", x, y)
                elif color_value == 0xFF1DE9B6:
                    spawn("energy", x, y)
                elif color_value == 0xFFFF5252:
                    spawn("nanomedkit", x, y)
                elif color_value == 0xFF00E5FF:
                    spawn("overclock", x, y)
                elif color_value == 0xFFFFC107:
                    spawn("quest_item", x, y)
                elif color_value == 0xFF00ACC1:
                    spawn("data_core", x, y)
                elif color_value == 0xFF4CAF50:
                    spawn("quest_beacon", x, y)
                elif color_value == 0xFF795548:
                    spawn("quest_npc", x, y)
                elif color_value == 0xFFFFB74D:
                    spawn("engineer", x, y)
                elif color_value == 0xFF7E57C2:
                    spawn("researcher", x, y)
                elif color_value == 0xFF673AB7:
                    spawn("teleport_pad", x, y)

                tiles.append(Tile(x, y, walkable, tile_color))

        image.close()
        return cls(width, height, tiles, spawns, player_spawn)

    def is_walkable(self, pixel_x: int, pixel_y: int) -> bool:
        tile_x = pixel_x // settings.TILE_SIZE
        tile_y = pixel_y // settings.TILE_SIZE
        tile = self._grid.get((tile_x, tile_y))
        return tile.walkable if tile else False

    def rect_collides(self, rect: tuple[int, int, int, int]) -> bool:
        x, y, w, h = rect
        for sample_x in (x, x + w - 1):
            for sample_y in (y, y + h - 1):
                if not self.is_walkable(sample_x, sample_y):
                    return True
        return False

    def iter_tiles(self) -> Iterator[Tile]:
        return iter(self.tiles)


ENEMY_COLOR_TABLE: Dict[int, str] = {
    0xFFFF0000: "scout",
    0xFF9C27B0: "teleporter",
    0xFF00BCD4: "artillery",
    0xFF3F51B5: "warden",
    0xFF009688: "sentinel",
    0xFFF4511E: "ravager",
    0xFFE91E63: "warbringer",
    0xFF7986CB: "overseer",
}
