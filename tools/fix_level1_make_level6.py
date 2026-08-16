"""Remove os Phantoms e o Guardian do level1 (deixando apenas inimigos mais
fracos para a primeira fase) e gera o mapa level6.png com o chefe OVERSEER."""
import os
from PIL import Image

BASE = os.path.dirname(os.path.abspath(__file__)) + "/../bin"

# --- Ajustar level1: remover PHANTOM (verde 81C784) e GUARDIAN (laranja FF5722) ---
p1 = os.path.join(BASE, "level1.png")
img = Image.open(p1).convert("RGB")
px = img.load()
removed = []
for x in range(img.width):
    for y in range(img.height):
        rgb = px[x, y]
        if rgb == (0x81, 0xC7, 0x84) or rgb == (0xFF, 0x57, 0x22):
            # Pinta como chão verde comum do mapa (mesma cor do tile vizinho)
            # usando um tile de chão adjacente quando possível
            nx = x + 1 if x + 1 < img.width else x
            neighbor = px[nx, y]
            px[x, y] = neighbor
            removed.append((x, y, rgb))
img.save(p1)
print("level1.png:", img.size, "removidos:", [(f"({x},{y}) 0x{r:02X}{g:02X}{b:02X}") for (x, y, (r, g, b)) in removed])

# --- Gerar level6.png (arena com chefe OVERSEER) ---
# O mapa level1 tem 32x22; o level6 terá 40x26 com layout de arena com pilares.
W, H = 40, 26
img6 = Image.new("RGB", (W, H), (0x05, 0x0F, 0x05))  # fundo escuro
px6 = img6.load()

WALL = (0x37, 0x47, 0x4F)   # parede cinza-azulada (mesma família dos outros mapas)
WALL2 = (0x45, 0x5A, 0x64)  # parede variante
FLOOR = (0x1B, 0x5E, 0x20)  # chão verde escuro
FLOOR2 = (0x2E, 0x7D, 0x32) # chão verde
BOSS = (0x79, 0x86, 0xCB)   # OVERSEER

# Moldura de paredes
for x in range(W):
    for y in range(H):
        if x in (0, 1, W - 1, W - 2) or y in (0, 1, H - 1, H - 2):
            px6[x, y] = WALL
# Corredores e pilares — padrão cruzado com salas
for bx in range(8, W - 8, 8):
    for by in range(6, H - 6, 6):
        for dx in range(3):
            for dy in range(3):
                px6[bx + dx, by + dy] = WALL2
# Pequenos obstáculos isolados
for bx, by in [(10, 10), (22, 14), (30, 8), (16, 18), (26, 20), (12, 20), (34, 16)]:
    px6[bx, by] = WALL2
# Chão no miolo (aberto para arena)
for x in range(4, W - 4):
    for y in range(4, H - 4):
        if px6[x, y] == (0x05, 0x0F, 0x05):
            px6[x, y] = FLOOR if (x + y) % 3 else FLOOR2
# Spawn do jogador no canto inferior esquerdo (área limpa)
# Boss OVERSEER no centro superior
px6[W // 2, 8] = BOSS
img6.save(os.path.join(BASE, "level6.png"))
print("level6.png gerado:", img6.size)
