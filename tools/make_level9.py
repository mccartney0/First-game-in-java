#!/usr/bin/env python3
"""Gera res/level9.png — Vale dos Refugiados (48x32 tiles, crescendo após level8 46x30).

Paleta (RGB):
  (0,0,0)        fora do mapa (preenchimento padrão)
  0xFFFFFF  parede sólida
  0x808080  parede destrutível
  0x7CB342  grama (chão principal)
  0x6D4C41  lama (áreas de combate/acampamento)
  0x0026FF  spawn do player
  0x3F51B5  Warden (inimigo básico reforçado)
  0x009688  Sentinel
  0xFFF4511E Ravager
  0xFFE91E63 Warbringer boss
  0x4CAF50  QuestBeacon (fardo de refugiados a escoltar)
  0x795548  QuestNPC (lídera dos refugiados)
  0xFFC107  QuestItem (suprimentos)
  0xFFCDDC39 Curandeiro Léo (suporte de cura)
  0x4CFF00  LifePack / 0x8E24AA ShieldOrb / 0x1DE9B6 EnergyCell
"""
from PIL import Image

W, H = 48, 32
img = Image.new('RGB', (W, H), (0, 0, 0))  # pixels fora do mapa

# Pixels
WHITE = (0xFF, 0xFF, 0xFF)
WALLD = (0x80, 0x80, 0x80)
GRASS = (0x7C, 0xB3, 0x42)
MUD = (0x6D, 0x4C, 0x41)
SPAWN = (0x00, 0x26, 0xFF)
WARDEN = (0x3F, 0x51, 0xB5)
SENTINEL = (0x00, 0x96, 0x88)
RAVAGER = (0xF4, 0x51, 0x1E)
BEACON = (0x4C, 0xAF, 0x50)
QNPC = (0x79, 0x55, 0x48)
QITEM = (0xFF, 0xC1, 0x07)
HEALER = (0xCD, 0xDC, 0x39)
LIFE = (0x4C, 0xFF, 0x00)
SHIELD = (0x8E, 0x24, 0xAA)

px = img.load()

# Interior: grama com lama nos bolsões de combate
for y in range(H):
    for x in range(W):
        px[x, y] = GRASS

# Lama: vale central (acampamento) e trilha sul
for y in range(12, 20):
    for x in range(10, 38):
        px[x, y] = MUD
for y in range(24, 29):
    for x in range(14, 34):
        px[x, y] = MUD

# Muros de desfiladeiro (paredes destrutíveis) norte/sul do vale
for x in range(8, 40):
    px[x, 10] = WALLD
    px[x, 21] = WALLD
# Aberturas nos muros para passagem
for x in (16, 17, 30, 31):
    px[x, 10] = GRASS
for x in (16, 17, 30, 31):
    px[x, 21] = GRASS
# Paredes sólidas nas bordas laterais dos muros
for x in (7, 39):
    px[x, 10] = WHITE
    px[x, 21] = WHITE

# Spawn seguro do player no canto noroeste (área de chegada)
for yy in range(2, 5):
    for xx in range(2, 5):
        px[xx, yy] = SPAWN

# Curandeiro Léo em alcova protegida a oeste do vale
px[6, 6] = HEALER
px[5, 6] = WALLD
px[6, 5] = WALLD

# Lídera dos refugiados (QuestNPC) no centro do acampamento
px[24, 15] = QNPC
# Fardo a escoltar (QuestBeacon) perto da líder
px[26, 16] = BEACON

# NPCs de apoio também presentes no acampamento
px[22, 14] = (0x66, 0xBB, 0x6A)  # Engenheira Nia
px[22, 16] = (0x5E, 0x35, 0xB1)  # Pesquisador Ivo

# Suprimentos (QuestItem) e itens de suporte espalhados
px[12, 15] = QITEM
px[34, 14] = QITEM
px[18, 18] = LIFE
px[32, 18] = SHIELD
px[14, 26] = LIFE
px[30, 27] = LIFE

# Inimigos: Wardens e Sentinels nas bordas do vale, Ravagers no sul
# Norte do vale
for x in (10, 14, 20, 26, 32, 36):
    px[x, 4] = WARDEN
# Corredor norte do vale
for x in (12, 18, 24, 30, 36):
    px[x, 7] = WARDEN
# Dentro do vale (sobre lama)
for x in (11, 15, 20, 28, 33, 36):
    px[x, 13] = SENTINEL
for x in (12, 16, 22, 28, 34):
    px[x, 17] = SENTINEL
# Trilha sul
for x in (15, 19, 23, 27, 31):
    px[x, 26] = RAVAGER
for x in (16, 20, 24, 28, 30):
    px[x, 28] = RAVAGER

img.save('/home/ubuntu/First-game-in-java/res/level9.png')
print('level9.png gerado:', img.size)
