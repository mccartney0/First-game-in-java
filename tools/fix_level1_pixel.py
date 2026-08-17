#!/usr/bin/env python3
"""Substitui o pixel rosa residual (15,11) do level1 por chão explícito
(0x000000), garantindo que o tile tenha semântica correta no World."""
from PIL import Image

path = "res/level1.png"
im = Image.open(path).convert("RGB")
x, y = 15, 11
im.putpixel((x, y), (0, 0, 0))
im.save(path)
r, g, b = Image.open(path).getpixel((x, y))
print("level1 (%d,%d) agora = #%02X%02X%02X" % (x, y, r, g, b))
