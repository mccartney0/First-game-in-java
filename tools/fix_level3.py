"""Conecta as ilhas do level3.png.

Diagnóstico: as salas são separadas pela PAREDE VERTICAL em x=12 (a coluna
de paredes que desce de y=1 a y=22). As aberturas existentes nas linhas 6,
12 e 18 (x=12 já aberto nessas linhas) conectam fatias, mas a coluna x=12
continua intacta nos trechos y=1-5, 7-11, 13-17 e 19-22, isolando as salas.

Correção: abrir a coluna x=12 em três pontos intermediários, evitando os
tiles de itens/inimigos ('?') na linha:
- (12, 3): sala superior  (linha 3 sem itens: '?' em 3,3 e 5,2 -> ok, 3,3 é '?')
  -> usar (12, 4)? linha 4: '?' em 8,4. (12,4) ok parede.
- (12, 9): sala central superior -> linha 9 tem '?' em 6,9 e 14,9; (12,9) ok.
- (12, 15): sala central inferior -> linha 15: '?' em 15,16? não, linha 15
  sem itens perto de x=12; (12,15) ok.
- (12, 20): sala inferior -> linha 20 tem '?' em 30,20; (12,20) ok.
"""
from PIL import Image
from collections import deque

SRC = 'res/level3.png'
im = Image.open(SRC).convert('RGBA')
W, H = im.size

openings = [(12, 4), (12, 9), (12, 15), (12, 20)]
for x, y in openings:
    p = im.getpixel((x, y))
    if p == (255, 255, 255, 255):
        im.putpixel((x, y), (0, 0, 0, 255))
    else:
        assert p == (0, 0, 0, 255), f"({x},{y}) expected wall/floor, got {p}"

im.save(SRC)


def isfloor(x, y):
    return 0 <= x < W and 0 <= y < H and im.getpixel((x, y)) == (0, 0, 0, 255)


visited = set()
comps = []
for y in range(H):
    for x in range(W):
        if isfloor(x, y) and (x, y) not in visited:
            q = deque([(x, y)])
            comp = set()
            while q:
                cx, cy = q.popleft()
                if (cx, cy) in comp:
                    continue
                comp.add((cx, cy))
                visited.add((cx, cy))
                for nx, ny in [(cx + 1, cy), (cx - 1, cy), (cx, cy + 1), (cx, cy - 1)]:
                    if isfloor(nx, ny) and (nx, ny) not in comp:
                        q.append((nx, ny))
            comps.append(len(comp))

print("corredores abertos:", openings)
print("componentes de chão agora:", comps)
big = [c for c in comps if c > 10]
small = [c for c in comps if c <= 10]
assert len(big) == 1, "mapa ainda desconectado!"
print("tiles órfãos (itens isolados, esperados):", small)
print("OK: level3 totalmente conectado")
