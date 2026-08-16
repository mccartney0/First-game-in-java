#!/usr/bin/env python3
"""Adiciona NPCs interativos aos mapas das fases 1-5.

Cores (ARGB) reservadas para NPCs interativos do novo sistema:
- 0xFF00897C (teal escuro)    -> Comandante Ava
- 0xFF4CAF50 já usado por beacon — usar 0xFF66BB6A (verde claro) para Engenheira Nia
- 0xFF7E57C2 já usado por ResearcherNPC antigo — usar 0xFF5E35B1 (roxo escuro) p/ Pesquisador Ivo
- 0xFFFF9800 (laranja)        -> Armeiro Mercúrio

Coloca os NPCs em tiles de chão acessíveis, próximos ao spawn do jogador,
sem sobrepor paredes, inimigos ou itens existentes.
"""
from PIL import Image
import os

BASE = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "bin")

# O jogo compara pixels via BufferedImage.getRGB, que retorna ARGB como int.
# Os PNGs podem estar salvos em modo RGB ou RGBA; para garantir o int ARGB
# esperado (ex.: 0xFF00897C), gravamos os pixels em RGBA — ImageIO converte
# de volta corretamente. A detecção usa o valor ARGB real lido por PIL
# (R<<24 | G<<16 | B<<8 | A).


def pixel_argb(px, x, y):
    """Lê o pixel como int ARGB igual ao que BufferedImage.getRGB retorna."""
    c = px[x, y]
    if len(c) == 3:
        r, g, b = c
        a = 255
    else:
        r, g, b, a = c
    return (a << 24) | (r << 16) | (g << 8) | b


FLOOR = 0xFF000000
WALL = 0xFFFFFFFF
SPAWN = 0xFF0026FF
AVATAR_COLORS = {
    "ava": 0xFF00897C,
    "nia": 0xFF66BB6A,
    "ivo": 0xFF5E35B1,
    "merc": 0xFFFF9800,
}

FLOOR = 0xFF000000
WALL = 0xFFFFFFFF
SPAWN = 0xFF0026FF
RED = 0xFFFF0000  # inimigo


def load(path):
    # Preserva o modo original (RGB ou RGBA) para não corromper outros tiles.
    img = Image.open(path)
    if img.mode != "RGBA":
        img = img.convert("RGBA")
    px = img.load()
    return img, px, img.size


def is_floor(px, x, y):
    if x < 0 or y < 0 or x >= len(px) or y >= len(px[0]):
        return False
    c = pixel_argb(px, x, y)
    return c == FLOOR or c == SPAWN


def map_dims(img):
    """Retorna (largura_tiles, altura_tiles) de um mapa PNG."""
    # Cada pixel do PNG corresponde a um tile de 16px no mundo.
    return img.size[0], img.size[1]


def place(img_path, placements):
    """placements: list of (key, tile_x, tile_y) — coords em tiles, relativas
    ao canto do mapa, independentemente do tamanho real (o tile 3,2 fica
    sempre perto do spawn, que costuma ser o 2,2 do mapa)."""
    img, px, size = load(img_path)
    width_tiles, height_tiles = map_dims(img)
    for key, tx, ty in placements:
        if tx < 0 or ty < 0 or tx >= width_tiles or ty >= height_tiles:
            raise SystemExit(f"tile fora do mapa {key} em {tx},{ty} ({img_path})")
        # Verifica se o tile é chão/spawn ou já contém o mesmo NPC (idempotente).
        r = pixel_argb(px, tx, ty)
        color = AVATAR_COLORS[key]
        if r == color:
            continue  # NPC já presente nessa posição — nada a fazer
        if r not in (FLOOR, SPAWN):
            c = px[tx, ty]
            raise SystemExit(f"tile {tx},{ty} não é chão/spawn (0x{r:08X}, raw={c}) em {img_path}")
        color = AVATAR_COLORS[key]
        px[tx, ty] = (
            (color >> 16) & 0xFF,
            (color >> 8) & 0xFF,
            color & 0xFF,
            0xFF,
        )
    img.save(img_path)
    print(f"{img_path}: adicionados {[k for k, _, _ in placements]}")


def main():
    # Posições relativas ao canto superior esquerdo (perto do spawn 2,2).
    near_spawn = [("ava", 3, 2)]
    for level in range(1, 7):
        place(os.path.join(BASE, f"level{level}.png"), near_spawn)
    # NPCs de apoio adicionais nas fases 2-6, posicionados logo ao lado.
    place(os.path.join(BASE, "level2.png"), [("nia", 4, 2)])
    place(os.path.join(BASE, "level3.png"), [("ivo", 5, 2)])
    place(os.path.join(BASE, "level4.png"), [("merc", 5, 2)])
    place(os.path.join(BASE, "level5.png"), [("ivo", 5, 2)])
    place(os.path.join(BASE, "level6.png"), [("merc", 25, 1)])


if __name__ == "__main__":
    main()
