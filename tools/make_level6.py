"""Gera level6.png com a paleta correta da engine:
preto = chão, branco = parede, 0x0026FF = spawn do jogador, 0x7986CB = OVERSEER (chefe).
Layout: arena aberta com pilares, boss no topo e jogador na base."""
from PIL import Image

W, H = 40, 26
BLACK = (0, 0, 0)        # chão
WHITE = (255, 255, 255)  # parede
PLAYER = (0x00, 0x26, 0xFF)
BOSS = (0x79, 0x86, 0xCB)
FLOOR_GREEN = (0x4C, 0xFF, 0x00)  # chão verde decorado

img = Image.new("RGB", (W, H), BLACK)
px = img.load()

# Moldura de paredes (1 tile de espessura)
for x in range(W):
    for y in range(H):
        if x == 0 or x == W - 1 or y == 0 or y == H - 1:
            px[x, y] = WHITE

# Pilares isolados (sala aberta para o combate contra o chefe)
pillars = [(8, 7), (12, 7), (8, 11), (12, 11),
           (27, 7), (31, 7), (27, 11), (31, 11),
           (14, 15), (25, 15), (14, 19), (25, 19),
           (19, 13), (20, 13), (19, 14), (20, 14)]
for (x, y) in pillars:
    px[x, y] = WHITE

# Alguns patches de chão verde decorado
for (x, y) in [(6, 20), (10, 21), (29, 20), (33, 21), (18, 22), (22, 22)]:
    px[x, y] = FLOOR_GREEN

# Spawn do jogador no canto inferior esquerdo (área limpa)
px[3, H - 3] = PLAYER
# Boss OVERSEER no centro superior (área limpa)
px[W // 2, 5] = BOSS

img.save("bin/level6.png")
print("level6.png gerado:", img.size)
