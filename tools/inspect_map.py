#!/usr/bin/env python3
"""Inspeciona as primeiras linhas de um mapa para encontrar tiles livres."""
from PIL import Image
import sys

names = {
    0xFF000000: "floor",
    0xFFFFFFFF: "wall",
    0xFF0026FF: "spawn",
    0xFFFF0000: "enemy",
    0xFFFFC107: "artifact",
    0xFF4CAF50: "beacon",
    0xFF673AB7: "teleport",
    0xFF808080: "dest_wall",
    0xFF7CB342: "grass",
    0xFF6D4C41: "mud",
    0xFFB0BEC5: "ice",
    0xFFE91E63: "warbringer",
    0xFF7986CB: "overseer",
    0xFF795548: "quest_npc",
    0xFFFFB74D: "eng_npc_old",
    0xFF7E57C2: "res_npc_old",
    0xFF00897C: "ava",
    0xFF66BB6A: "nia",
    0xFF5E35B1: "ivo",
    0xFFFF9800: "merc",
}


def argb(px, x, y):
    c = px[x, y]
    r, g, b = c[:3]
    a = c[3] if len(c) == 4 else 255
    return (a << 24) | (r << 16) | (g << 8) | b


for path in sys.argv[1:]:
    img = Image.open(path).convert("RGB")
    px = img.load()
    print(f"=== {path} {img.size} ===")
    for y in range(min(6, img.size[1])):
        row = []
        for x in range(img.size[0]):
            v = argb(px, x, y)
            row.append(names.get(v, f"0x{v:08X}"[-8:]))
        print(f"y={y}: " + " ".join(f"{c[:4]:>11}" for c in row))
    # contagem geral
    c = {}
    for x in range(img.size[0]):
        for y in range(img.size[1]):
            v = argb(px, x, y)
            c[v] = c.get(v, 0) + 1
    print("contagem:", {names.get(k, hex(k)): v for k, v in sorted(c.items(), key=lambda kv: -kv[1])[:12]})
