"""Adiciona pixels de spawn de NPCs temáticos nos mapas de nível.

Cada fase ganha um ponto de encontro narrativo: a Comandante Ava espera no
centro de comando da fase 1, o Pesquisador Ivo está no laboratório da fase 3,
o Armeiro Mercúrio forja na fase 4, e assim por diante. O pixel é colocado
apenas sobre chão caminhável (cor != parede branca 0xFFFFFFFF e != pedra).
"""
from PIL import Image

WALL = (255, 255, 255)
STONE = (128, 128, 128)
AVATAR_COLORS = {
    # pixel spawns existentes
    0xE649B1: "Ava", 0x66BB6A: "Nia", 0x5E35B1: "Ivo",
    0xFF9800: "Mercurio", 0xA1887F: "Helio",
}

# Fase -> [(cor_rgb, tx, ty)] — NPCs temáticos a garantir por fase
SPAWNS = {
    1: [(0xE6, 0x49, 0xB1, 16, 11)],              # Ava no centro de comando
    2: [(0x66, 0xBB, 0x6A, 28, 6)],               # Nia no esconderijo técnico
    3: [(0x5E, 0x35, 0xB1, 18, 11)],              # Ivo no laboratório
    4: [(0xFF, 0x98, 0x00, 22, 14)],              # Mercúrio na forja
    5: [(0x5E, 0x35, 0xB1, 22, 8)],               # Ivo no posto avançado
    6: [(0xFF, 0x98, 0x00, 14, 14)],              # Mercúrio na base da torre
    7: [(0xA1, 0x88, 0x7F, 20, 14)],              # Hélio no coração do subsolo
    8: [(0xFF, 0x98, 0x00, 40, 24)],              # Mercúrio no refúgio final
}

for lvl, pixels in SPAWNS.items():
    path = f"res/level{lvl}.png"
    im = Image.open(path).convert("RGB")
    w, h = im.size
    for (r, g, b, tx, ty) in pixels:
        if not (0 <= tx < w and 0 <= ty < h):
            print(f"L{lvl}: tile fora do mapa ({tx},{ty}) — pulando")
            continue
        cur = im.getpixel((tx, ty))
        if cur == WALL or cur == STONE:
            # procura tile caminhável próximo (varredura em anel)
            found = None
            for radius in range(1, 9):
                for dx in range(-radius, radius + 1):
                    for dy in range(-radius, radius + 1):
                        if max(abs(dx), abs(dy)) != radius:
                            continue
                        nx, ny = tx + dx, ty + dy
                        if 0 <= nx < w and 0 <= ny < h:
                            c = im.getpixel((nx, ny))
                            if c != WALL and c != STONE:
                                found = (nx, ny)
                                break
                    if found:
                        break
                if found:
                    break
            if not found:
                print(f"L{lvl}: sem tile livre perto de ({tx},{ty}) — pulando")
                continue
            tx, ty = found
        # Evita sobrepor spawn existente de outro NPC: conta pixels da cor
        count = sum(1 for y in range(h) for x in range(w)
                    if im.getpixel((x, y)) == (r, g, b))
        if count == 0:
            im.putpixel((tx, ty), (r, g, b))
            print(f"L{lvl}: spawn {AVATAR_COLORS.get((r << 16) | (g << 8) | b, '?')} -> ({tx},{ty})")
        else:
            print(f"L{lvl}: spawn {AVATAR_COLORS.get((r << 16) | (g << 8) | b, '?')} já existe ({count}x) — mantido")
    im.save(path)
    print(f"  {path} salvo ({w}x{h})")
print("OK")
