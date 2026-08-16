"""Gera level7.png (Subsolo da Colônia) e level8.png (Núcleo Central).

Padrão da engine (World.java): 1 tile = 1 pixel de 16 px.
Cores de terreno: 0=chão preto (FF000000), 255=parede branca (FFFFFFFF),
128=parede destrutível (FF808080).
Cores de entidades:
  255,0,0   inimigo aleatório
  63,81,181 WARDEN      | 0,150,136 SENTINEL   | 244,81,30 RAVAGER
  233,30,99 WARBRINGER  | 121,134,203 OVERSEER | 208,25,55 OVERSEER_PRIME
  129,199,132 PHANTOM
  255,87,34 GUARDIAN    | 156,39,176 TELEPORTER| 3,169,244 ARTILLERY
  76,255,0  LifePack    | 255,82,82 NanoMedkit | 0,172,193 DataCore
  0,229,255 Overclock   | 255,193,7 QuestItem  | 0,38,255 player spawn
  0,137,124 Ava         | 102,187,106 Nia      | 94,53,177 Ivo
  255,152,0 Mercúrio    | 103,58,183 TeleportPad| 255,106,0 Weapon
  74,222,128 shield orb | 29,233,182 energy    | 255,216,0 bullet pickup
"""
from PIL import Image

BLACK = (0, 0, 0, 255)
WALL = (255, 255, 255, 255)
DESTRUCT = (128, 128, 128, 255)

ENEMY = (255, 0, 0, 255)
WARDEN = (63, 81, 181, 255)
SENTINEL = (0, 150, 136, 255)
RAVAGER = (244, 81, 30, 255)
GUARDIAN = (255, 87, 34, 255)
PHANTOM = (129, 199, 132, 255)
WARBRINGER = (233, 30, 99, 255)
OVERSEER = (121, 134, 203, 255)
OVERSEER_PRIME = (208, 25, 55, 255)
TELEPORTER = (156, 39, 176, 255)
ARTILLERY = (3, 169, 244, 255)
LIFEPACK = (76, 255, 0, 255)
NANO = (255, 82, 82, 255)
DATACORE = (0, 172, 193, 255)
OVERCLOCK = (0, 229, 255, 255)
QUEST = (255, 193, 7, 255)
PLAYER = (0, 38, 255, 255)
AVA = (0, 137, 124, 255)
NIA = (102, 187, 106, 255)
IVO = (94, 53, 177, 255)
MERC = (255, 152, 0, 255)
PAD = (103, 58, 183, 255)
WEAPON = (255, 106, 0, 255)
SHIELD = (74, 222, 128, 255)
ENERGY = (29, 233, 182, 255)


def new_map(w, h, fill=BLACK):
    return [[fill] * w for _ in range(h)]


def box(m, x, y, w, h, c):
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            m[yy][xx] = c


def setcell(m, x, y, c):
    m[y][x] = c


def save(m, path):
    h = len(m)
    w = len(m[0])
    im = Image.new("RGBA", (w, h))
    im.putdata([m[yy][xx] for yy in range(h) for xx in range(w)])
    im.save(path)
    print("saved", path, w, "x", h)


def border(m, thick=1, c=WALL):
    h, w = len(m), len(m[0])
    box(m, 0, 0, w, thick, c)
    box(m, 0, h - thick, w, thick, c)
    box(m, 0, 0, thick, h, c)
    box(m, w - thick, 0, thick, h, c)


# ----------------------------------------------------------------------------
# LEVEL 7 — Subsolo da Colônia (42 x 28)
# Tema: túneis de manutenção escuros, câmaras com geradores, labirinto de
# corredores. Objetivo: sabotar 3 geradores (quest items vermelhos) e falar
# com o Deserto "Técnico Hélio" (usando EngineerNPC antigo) — aqui usamos
# Ava + Ivo como suporte e um NPC especial via QuestNPC com a cor 795548
# ----------------------------------------------------------------------------
W7, H7 = 42, 28
m7 = new_map(W7, H7)
border(m7)

# Sala de entrada (esquerda): spawn do jogador
box(m7, 1, 1, 9, 7, BLACK)

# Corredor horizontal principal no meio
box(m7, 1, 13, W7 - 2, 3, BLACK)

# Câmaras norte (3 salas) conectadas por corredores
box(m7, 4, 1, 7, 9, BLACK)     # câmara 1 — primeiro gerador
box(m7, 15, 1, 7, 9, BLACK)    # câmara 2 — NPC traidor
box(m7, 26, 1, 7, 9, BLACK)    # câmara 3 — segundo gerador
box(m7, 37, 1, 4, 9, BLACK)    # câmara lateral

# Câmaras sul (2 salas grandes)
box(m7, 4, 19, 8, 8, BLACK)    # câmara sul 1 — terceiro gerador
box(m7, 17, 19, 10, 8, BLACK)  # câmara sul 2 — área do chefe
box(m7, 31, 19, 10, 8, BLACK)  # câmara sul 3 — suporte + loot

# Conectores verticais (deixar passagem nas paredes horizontais)
for x in (6, 20, 33, 40):
    box(m7, x, 10, 2, 3, BLACK)
for x in (7, 21, 36):
    box(m7, x, 16, 2, 3, BLACK)

# Pilares decorativos/desviáveis em volta das câmaras
for x, y in [(13, 3), (24, 4), (35, 2), (5, 20), (29, 21), (39, 20)]:
    setcell(m7, x, y, DESTRUCT)

# Player na sala de entrada
setcell(m7, 3, 3, PLAYER)

# Ava na sala de entrada (objetivo: falar antes de descer)
setcell(m7, 7, 3, AVA)

# Câmera 1 — gerador (quest item) + inimigos leves
setcell(m7, 7, 2, QUEST)
setcell(m7, 5, 4, WARDEN)
setcell(m7, 9, 5, SENTINEL)

# Câmera 2 — NPC traidor (QuestNPC marrom) + guardas
setcell(m7, 18, 2, (161, 136, 127, 255))  # Técnico Hélio (TraitorNpc — cor A1887F)
setcell(m7, 16, 5, WARDEN)
setcell(m7, 20, 6, WARDEN)

# Câmera 3 — gerador 2 + sentinelas
setcell(m7, 29, 2, QUEST)
setcell(m7, 27, 4, SENTINEL)
setcell(m7, 31, 5, SENTINEL)
setcell(m7, 28, 7, PHANTOM)

# Câmera lateral — phantoms e loot
setcell(m7, 39, 2, PHANTOM)
setcell(m7, 38, 4, PHANTOM)
setcell(m7, 40, 5, LIFEPACK)

# Câmera sul 1 — gerador 3 + ravagers
setcell(m7, 8, 20, QUEST)
setcell(m7, 5, 22, RAVAGER)
setcell(m7, 10, 24, RAVAGER)
setcell(m7, 6, 25, NANO)

# Câmera sul 2 — CHEFE do subsolo (GUARDIAN reforçado) + Ivo (NPC de apoio)
setcell(m7, 22, 23, GUARDIAN)
setcell(m7, 18, 20, IVO)
setcell(m7, 25, 25, TELEPORTER)

# Câmera sul 3 — suporte (Nia + Mercúrio), weapon e suprimentos
setcell(m7, 32, 20, NIA)
setcell(m7, 34, 20, MERC)
setcell(m7, 38, 21, WEAPON)
setcell(m7, 39, 23, LIFEPACK)
setcell(m7, 37, 24, SHIELD)
setcell(m7, 39, 25, ENERGY)

# Inimigos espalhados no corredor principal e corredores verticais
# (afastados do conector x=6 que leva ao spawn, para o jogador não ser
# emboscado logo na entrada da fase)
for x in (18, 24, 30, 36):
    setcell(m7, x, 14, ENEMY)
setcell(m7, 21, 17, RAVAGER)
setcell(m7, 36, 11, PHANTOM)

save(m7, "res/level7.png")

# ----------------------------------------------------------------------------
# LEVEL 8 — Núcleo Central / A Mente da Colônia (46 x 30)
# Tema: salão do núcleo da IA — sala circular simulada com paredes, câmaras
# de servidores laterais. Boss final: OVERSEER PRIME (OVERSEER no centro) +
# sentinelas. Ava final e loot no topo.
# ----------------------------------------------------------------------------
W8, H8 = 46, 30
m8 = new_map(W8, H8)
border(m8, 1, WALL)

# Grande salão central aberto
box(m8, 1, 1, W8 - 2, H8 - 2, BLACK)

# Anel de colunas/paredes destrutíveis simulando o núcleo
for a in range(6, 40, 4):
    setcell(m8, a, 5, DESTRUCT)
    setcell(m8, a, H8 - 6, DESTRUCT)
for b in range(6, H8 - 6, 4):
    setcell(m8, 6, b, DESTRUCT)
    setcell(m8, W8 - 7, b, DESTRUCT)

# Câmaras de servidores nos cantos superiores (salas fechadas com porta)
box(m8, 1, 1, 10, 8, WALL)
box(m8, 1, 1, 10, 8, BLACK)
box(m8, W8 - 11, 1, 10, 8, WALL)
box(m8, W8 - 11, 1, 10, 8, BLACK)
# portas
for x in (5, 6):
    setcell(m8, x, 8, BLACK)
    setcell(m8, W8 - 1 - x, 8, BLACK)

# Câmaras inferiores (apoio)
box(m8, 1, H8 - 9, 9, 8, WALL)
box(m8, 1, H8 - 9, 9, 8, BLACK)
box(m8, W8 - 10, H8 - 9, 9, 8, WALL)
box(m8, W8 - 10, H8 - 9, 9, 8, BLACK)
for x in (4, 5):
    setcell(m8, x, H8 - 9, BLACK)
    setcell(m8, W8 - 1 - x, H8 - 9, BLACK)

# Player na câmara superior esquerda
setcell(m8, 3, 3, PLAYER)
# Ava — briefing final
setcell(m8, 8, 3, AVA)

# Câmaras de servidores: teletransportadores e dados
setcell(m8, 5, 5, TELEPORTER)
setcell(m8, 7, 6, DATACORE)
setcell(m8, 9, 6, OVERCLOCK)
setcell(m8, W8 - 6, 5, TELEPORTER)
setcell(m8, W8 - 8, 6, DATACORE)
setcell(m8, W8 - 5, 6, NANO)

# Boss central: OVERSEER-PRIME (chefe final da campanha) cercado de sentinelas
setcell(m8, 23, 14, OVERSEER_PRIME)
for dx, dy in [(-4, -3), (4, -3), (-4, 3), (4, 3), (-6, 0), (6, 0)]:
    setcell(m8, 23 + dx, 14 + dy, SENTINEL)
# WARDENs patrulhando o salão
for x in (12, 18, 28, 34):
    setcell(m8, x, 14, WARDEN)
# Ravagers nos flancos
setcell(m8, 13, 9, RAVAGER)
setcell(m8, 33, 9, RAVAGER)
setcell(m8, 13, 20, RAVAGER)
setcell(m8, 33, 20, RAVAGER)
# Phantoms furtivos perto das câmaras
setcell(m8, 9, 17, PHANTOM)
setcell(m8, 37, 17, PHANTOM)
setcell(m8, 23, 4, PHANTOM)
setcell(m8, 23, 25, PHANTOM)
# Inimigos genéricos
for x in (10, 16, 30, 36):
    setcell(m8, x, 20, ENEMY)

# Câmaras inferiores: loot e último suporte (Mercúrio)
setcell(m8, 3, H8 - 4, MERC)
setcell(m8, 6, H8 - 5, WEAPON)
setcell(m8, 8, H8 - 3, LIFEPACK)
setcell(m8, W8 - 4, H8 - 4, SHIELD)
setcell(m8, W8 - 6, H8 - 5, ENERGY)
setcell(m8, W8 - 8, H8 - 3, NANO)

save(m8, "res/level8.png")
