#!/usr/bin/env python3
"""Verifica as cores dos tiles de terreno ao redor de uma posição (para saber se
placeStoryNpcs consegue mover o NPC para lá ou mantém o pixel original)."""
import sys
from PIL import Image

path = sys.argv[1]
tx, ty = int(sys.argv[2]), int(sys.argv[3])
im = Image.open(path).convert("RGB")
w, h = im.size
for y in range(ty - 4, ty + 5):
    row = []
    for x in range(tx - 4, tx + 5):
        if 0 <= x < w and 0 <= y < h:
            r, g, b = im.getpixel((x, y))
            row.append("#%02X%02X%02X" % (r, g, b))
        else:
            row.append("   OUT   ")
    print("y=%2d: %s" % (y, " ".join(row)))
