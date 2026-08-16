#!/usr/bin/env python3
"""Gera bin/training.png: arena de treino do onboarding.

Paleta (a mesma do parse do World):
- preto   0x000000 -> floor
- branco  0xFFFFFF -> parede
- 0026FF  -> spawn do jogador (conforme o parse do World.java)
"""
from PIL import Image

W, H = 28, 18
TS = 16  # cada pixel do PNG = 1 tile de 16x16

img = Image.new("RGB", (W, H), (0, 0, 0))

PIXEL = img.load()


def wall(x, y):
    PIXEL[x, y] = (0xFF, 0xFF, 0xFF)


def floor(x, y):
    PIXEL[x, y] = (0, 0, 0)


def spawn(x, y):
    PIXEL[x, y] = (0x00, 0x26, 0xFF)


# Moldura de paredes
for x in range(W):
    wall(x, 0)
    wall(x, H - 1)
for y in range(H):
    wall(0, y)
    wall(W - 1, y)

# Piso interno
for x in range(1, W - 1):
    for y in range(1, H - 1):
        floor(x, y)

# Spawn do jogador (canto inferior esquerdo)
spawn(3, H - 3)

# Alguns pilares decorativos para treino de navegação (sem quest itens, sem pads)
for x, y in [(7, 4), (13, 6), (20, 4), (9, 12), (18, 12)]:
    wall(x, y)

img.save("/home/ubuntu/First-game-in-java/bin/training.png")
print("training.png gerado:", W, "x", H)
