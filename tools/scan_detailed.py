#!/usr/bin/env python3
"""Varre um PNG de nível e lista TODAS as posições exatas (x,y) de cada cor de interesse."""
import sys
from PIL import Image

TARGETS = {
    0x00897C: "Ava (Comandante)",
    0x66BB6A: "Nia (Engenheira)",
    0x5E35B1: "Ivo (Pesquisador)",
    0xFF9800: "Mercurio (Armeiro)",
    0x0026FF: "SPAWN jogador",
    0x795548: "QuestNPC",
    0x4CFF00: "Beacon",
    0xE91E63: "WARBRINGER (boss)",
    0x7986CB: "OVERSEER (boss)",
    0xD01937: "PRIME (boss)",
    0x3F51B5: "WARDEN",
    0x009688: "SENTINEL",
    0xF4511E: "RAVAGER",
    0xFFC107: "QuestItem",
    0x673AB7: "Teleporte",
    0xE649B1: "rosa??",
    0xFFB74D: "EngineerNPC",
    0xFF5252: "NanoMedkit",
    0x00E5FF: "OverclockModule",
    0x00ACC1: "DataCore",
}

path = sys.argv[1]
im = Image.open(path).convert("RGB")
w, h = im.size
hits = {}
for y in range(h):
    for x in range(w):
        r, g, b = im.getpixel((x, y))
        p = (r << 16) | (g << 8) | b
        if p in TARGETS:
            hits.setdefault(TARGETS[p], []).append((x, y))
for name, coords in hits.items():
    print(f"{name}: {coords}")
