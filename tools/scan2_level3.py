"""Identifica a fronteira entre as duas ilhas do level3 para escolher
os pontos de abertura corretos."""
from PIL import Image
from collections import deque

im = Image.open('res/level3.png').convert('RGBA')
W, H = im.size

def isfloor(x, y):
    return 0 <= x < W and 0 <= y < H and im.getpixel((x, y)) == (0, 0, 0, 255)

visited = set()
comp = set()
# componente da (1,1)
q = deque([(1, 1)])
while q:
    cx, cy = q.popleft()
    if (cx, cy) in comp:
        continue
    comp.add((cx, cy))
    visited.add((cx, cy))
    for nx, ny in [(cx + 1, cy), (cx - 1, cy), (cx, cy + 1), (cx, cy - 1)]:
        if isfloor(nx, ny) and (nx, ny) not in comp:
            q.append((nx, ny))

print("ilha principal (spawn), tamanho:", len(comp))

# Parede adjacente a ambas as ilhas: para cada parede, checar vizinhos de chão
# em componentes diferentes.
walls_ok = []
for y in range(1, H - 1):
    for x in range(1, W - 1):
        if im.getpixel((x, y)) != (255, 255, 255, 255):
            continue
        seen = set()
        for nx, ny in [(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)]:
            if isfloor(nx, ny):
                seen.add("A" if (nx, ny) in comp else "B")
        if seen == {"A", "B"}:
            walls_ok.append((x, y))

print("paredes que tocam as duas ilhas:")
for w in walls_ok:
    print(" ", w)
