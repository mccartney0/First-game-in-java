#!/usr/bin/env python3
"""Mostra as cores mais comuns de cada mapa para identificar o chão."""
from PIL import Image
from collections import Counter

for name in ["bin/level2.png", "bin/level5.png"]:
    img = Image.open(name).convert("RGBA")
    c = Counter(img.getdata())
    print(name, img.size)
    for color, count in c.most_common(8):
        print("  %02X%02X%02X %d" % (color[0], color[1], color[2], count))
