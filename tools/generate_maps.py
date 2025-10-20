#!/usr/bin/env python3
"""Procedurally generate RPG-inspired stage layouts for the Java game."""

import os
import random
import struct
import zlib
from typing import Dict, Iterable, List, Sequence

Color = Sequence[int]

PALETTE: Dict[str, Color] = {
    '.': (0, 0, 0),
    '#': (255, 255, 255),
    'P': (0, 38, 255),
    'E': (255, 0, 0),
    'T': (156, 39, 176),
    'A': (0, 188, 212),
    'G': (63, 81, 181),
    'B': (233, 30, 99),
    'Y': (0, 150, 136),
    'Z': (244, 81, 30),
    'K': (121, 134, 203),
    'W': (255, 106, 0),
    'H': (76, 255, 0),
    'X': (128, 128, 128),
    'L': (255, 216, 0),
    'Q': (255, 193, 7),
    'O': (76, 175, 80),
    'N': (121, 85, 72),
    'C': (29, 233, 182),
    'M': (255, 82, 82),
    'R': (0, 229, 255),
    'D': (0, 172, 193),
    'S': (126, 87, 194),
    'I': (255, 183, 77),
    'U': (103, 58, 183),
}


def create_grid(width: int, height: int, fill: str = '.') -> List[List[str]]:
    grid = [[fill for _ in range(width)] for _ in range(height)]
    for x in range(width):
        grid[0][x] = '#'
        grid[height - 1][x] = '#'
    for y in range(height):
        grid[y][0] = '#'
        grid[y][width - 1] = '#'
    return grid


def add_horizontal_wall(grid: List[List[str]], y: int, x_start: int, x_end: int, openings: Iterable[int] = ()) -> None:
    for x in range(x_start, x_end + 1):
        grid[y][x] = '#'
    for opening in openings:
        if x_start <= opening <= x_end:
            grid[y][opening] = '.'


def add_vertical_wall(grid: List[List[str]], x: int, y_start: int, y_end: int, openings: Iterable[int] = ()) -> None:
    for y in range(y_start, y_end + 1):
        grid[y][x] = '#'
    for opening in openings:
        if y_start <= opening <= y_end:
            grid[opening][x] = '.'


def place(grid: List[List[str]], x: int, y: int, token: str) -> None:
    grid[y][x] = token


def place_random_teleporters(
    grid: List[List[str]], count: int = 3, radius: int | None = None
) -> None:
    """Scatter teleporters near the center of the map."""

    height = len(grid)
    if height == 0:
        return
    width = len(grid[0])
    cx, cy = width // 2, height // 2

    if radius is None:
        radius = max(2, min(width, height) // 5)

    left = max(1, cx - radius)
    right = min(width - 2, cx + radius)
    top = max(1, cy - radius)
    bottom = min(height - 2, cy + radius)

    attempts = 0
    placed = 0
    max_attempts = max(10, count * 12)
    while placed < count and attempts < max_attempts:
        attempts += 1
        x = random.randint(left, right)
        y = random.randint(top, bottom)
        if grid[y][x] != '.':
            continue
        grid[y][x] = 'U'
        placed += 1

    if placed == 0:
        if grid[cy][cx] == '.':
            grid[cy][cx] = 'U'
        else:
            best_pos: tuple[int, int] | None = None
            best_distance = None
            for y in range(height):
                for x in range(width):
                    if grid[y][x] != '.':
                        continue
                    distance = abs(x - cx) + abs(y - cy)
                    if best_pos is None or distance < best_distance:
                        best_pos = (x, y)
                        best_distance = distance
            if best_pos is not None:
                grid[best_pos[1]][best_pos[0]] = 'U'


def level_one() -> List[str]:
    width, height = 32, 22
    grid = create_grid(width, height)

    add_horizontal_wall(grid, 4, 1, width - 2, openings=[6, 14, 22, 27])
    add_horizontal_wall(grid, 10, 1, width - 2, openings=[5, 16, 24])
    add_horizontal_wall(grid, 15, 1, width - 2, openings=[7, 20, 26])
    add_vertical_wall(grid, 8, 1, height - 2, openings=[3, 9, 17])
    add_vertical_wall(grid, 16, 5, height - 3, openings=[9, 12, 18])
    add_vertical_wall(grid, 24, 2, height - 3, openings=[6, 13, 19])

    place(grid, 2, 2, 'P')
    for x, y in [(6, 2), (13, 6), (21, 8), (10, 18), (26, 12)]:
        place(grid, x, y, 'E')
    for x, y in [(18, 14), (22, 5)]:
        place(grid, x, y, 'G')
    for x, y in [(27, 3), (14, 9), (22, 16), (6, 17)]:
        place(grid, x, y, 'Q')
    for x, y in [(5, 6), (26, 11), (15, 17)]:
        place(grid, x, y, 'H')
    for x, y in [(12, 13), (19, 7)]:
        place(grid, x, y, 'C')
    place(grid, 9, 5, 'W')

    for x, y in [
        (11, 4), (12, 4), (19, 4),
        (8, 7), (8, 8), (8, 14),
        (11, 10), (12, 10), (20, 10), (21, 10),
        (16, 13), (16, 14), (24, 14), (24, 15),
    ]:
        place(grid, x, y, 'X')

    place_random_teleporters(grid)

    return [''.join(row) for row in grid]


def level_two() -> List[str]:
    width, height = 34, 22
    grid = create_grid(width, height)

    add_horizontal_wall(grid, 5, 3, width - 4, openings=[10, 17, 24])
    add_horizontal_wall(grid, 16, 3, width - 4, openings=[7, 17, 27])
    add_vertical_wall(grid, 7, 5, 16, openings=[10])
    add_vertical_wall(grid, 27, 5, 16, openings=[11])
    add_vertical_wall(grid, 17, 1, height - 2, openings=[5, 16])

    place(grid, 3, 3, 'P')
    place(grid, 17, 10, 'B')
    for x, y in [(11, 7), (23, 7), (11, 14), (23, 14)]:
        place(grid, x, y, 'G')
    for x, y in [(8, 9), (26, 9), (8, 12), (26, 12)]:
        place(grid, x, y, 'E')
    place(grid, 17, 6, 'T')
    place(grid, 17, 15, 'A')
    for x, y in [(6, 6), (28, 6), (6, 15), (28, 15)]:
        place(grid, x, y, 'H')
    for x, y in [(13, 5), (21, 16)]:
        place(grid, x, y, 'C')
    place(grid, 17, 3, 'W')

    place_random_teleporters(grid)

    return [''.join(row) for row in grid]


def level_three() -> List[str]:
    width, height = 36, 24
    grid = create_grid(width, height)

    add_horizontal_wall(grid, 6, 2, width - 3, openings=[9, 18, 27])
    add_horizontal_wall(grid, 12, 2, width - 3, openings=[6, 18, 30])
    add_horizontal_wall(grid, 18, 2, width - 3, openings=[8, 20, 28])
    add_vertical_wall(grid, 12, 1, height - 2, openings=[6, 12, 18])
    add_vertical_wall(grid, 24, 1, height - 2, openings=[5, 15, 21])

    place(grid, 3, 3, 'P')
    for x, y in [(8, 4), (20, 9), (28, 5), (30, 17), (15, 19)]:
        place(grid, x, y, 'E')
    for x, y in [(13, 7), (25, 13), (33, 9)]:
        place(grid, x, y, 'G')
    for x, y in [(9, 8), (21, 14), (29, 20)]:
        place(grid, x, y, 'O')
    for x, y in [(6, 9), (18, 6), (22, 19)]:
        place(grid, x, y, 'C')
    for x, y in [(11, 11), (27, 7), (14, 21)]:
        place(grid, x, y, 'H')
    place(grid, 5, 18, 'W')

    place_random_teleporters(grid, count=4)

    return [''.join(row) for row in grid]


def level_four() -> List[str]:
    width, height = 34, 24
    grid = create_grid(width, height)

    add_horizontal_wall(grid, 5, 2, width - 3, openings=[8, 15, 24])
    add_horizontal_wall(grid, 11, 2, width - 3, openings=[6, 20, 28])
    add_horizontal_wall(grid, 17, 2, width - 3, openings=[10, 17, 25])
    add_vertical_wall(grid, 10, 1, height - 2, openings=[5, 11, 19])
    add_vertical_wall(grid, 20, 1, height - 2, openings=[6, 16])
    add_vertical_wall(grid, 28, 4, height - 3, openings=[9, 15, 21])

    place(grid, 3, 3, 'P')
    for x, y in [(7, 4), (18, 4), (26, 6), (12, 14), (22, 18)]:
        place(grid, x, y, 'E')
    for x, y in [(14, 8), (24, 12)]:
        place(grid, x, y, 'G')
    for x, y in [(30, 8), (16, 20)]:
        place(grid, x, y, 'N')
    for x, y in [(8, 9), (21, 6), (25, 17)]:
        place(grid, x, y, 'H')
    for x, y in [(6, 13), (27, 10)]:
        place(grid, x, y, 'C')
    place(grid, 12, 19, 'W')
    place(grid, 30, 19, 'Q')

    place_random_teleporters(grid, count=4)

    return [''.join(row) for row in grid]


def level_five() -> List[str]:
    width, height = 36, 26
    grid = create_grid(width, height)

    add_horizontal_wall(grid, 5, 2, width - 3, openings=[9, 18, 26])
    add_horizontal_wall(grid, 11, 2, width - 3, openings=[8, 18, 30])
    add_horizontal_wall(grid, 17, 2, width - 3, openings=[6, 20, 28])
    add_horizontal_wall(grid, 21, 3, width - 4, openings=[12, 24])
    add_vertical_wall(grid, 10, 1, height - 2, openings=[5, 12, 20])
    add_vertical_wall(grid, 18, 1, height - 2, openings=[7, 15, 23])
    add_vertical_wall(grid, 26, 3, height - 3, openings=[9, 17, 22])

    place(grid, 3, 3, 'P')
    for x, y in [(9, 6), (21, 10), (29, 16), (15, 20)]:
        place(grid, x, y, 'D')
    place(grid, 30, 21, 'S')
    place(grid, 7, 19, 'I')
    for x, y in [(12, 8), (24, 14), (28, 22)]:
        place(grid, x, y, 'Q')
    for x, y in [(11, 4), (23, 12), (18, 18)]:
        place(grid, x, y, 'M')
    for x, y in [(20, 7), (27, 13), (14, 17)]:
        place(grid, x, y, 'R')
    for x, y in [(13, 6), (22, 6), (12, 15), (24, 18), (31, 9)]:
        place(grid, x, y, 'E')
    for x, y in [(16, 9), (25, 11)]:
        place(grid, x, y, 'G')
    place(grid, 32, 19, 'B')

    place_random_teleporters(grid, count=5)

    return [''.join(row) for row in grid]


def write_png(path: str, grid: Sequence[str]) -> None:
    height = len(grid)
    width = len(grid[0]) if height > 0 else 0
    raw = bytearray()
    for row in grid:
        if len(row) != width:
            raise ValueError("All rows must have the same width")
        raw.append(0)
        for token in row:
            if token not in PALETTE:
                raise ValueError(f"No palette entry for token '{token}'")
            r, g, b = PALETTE[token]
            raw.extend([r, g, b])

    signature = b"\x89PNG\r\n\x1a\n"
    ihdr = struct.pack("!IIBBBBB", width, height, 8, 2, 0, 0, 0)
    idat = zlib.compress(bytes(raw), 9)

    def chunk(tag: bytes, data: bytes) -> bytes:
        return struct.pack("!I", len(data)) + tag + data + struct.pack("!I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    png_data = bytearray()
    png_data.extend(signature)
    png_data.extend(chunk(b"IHDR", ihdr))
    png_data.extend(chunk(b"IDAT", idat))
    png_data.extend(chunk(b"IEND", b""))

    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as handle:
        handle.write(png_data)


def main() -> None:
    outputs = {
        "res/level1.png": level_one(),
        "res/level2.png": level_two(),
        "res/level3.png": level_three(),
        "res/level4.png": level_four(),
        "res/level5.png": level_five(),
    }
    for path, grid in outputs.items():
        write_png(path, grid)
        print(f"Generated {path} ({len(grid[0])}x{len(grid)})")


if __name__ == "__main__":
    main()
