"""Inspeção das cores especiais (NPCs, itens, inimigos) nos mapas de nível."""
from PIL import Image
import glob

# Cores especiais usadas no World.java
SPECIAL = {
    0xE649B1: "Commander Ava (0xE649B1)",
    0x66BB6A: "Engenheira Nia (0x66BB6A)",
    0x5E35B1: "Pesquisador Ivo (0x5E35B1)",
    0xFF9800: "Armeiro Mercurio (0xFF9800)",
    0xFFB74D: "EngenheiroNPC legacy (0xFFB74D)",
    0x7E57C2: "ResearcherNPC legacy (0x7E57C2)",
    0xA1887F: "Tecnico Helio traidor (0xA1887F)",
    0x3F51B5: "WARDEN",
    0x009688: "SENTINEL",
    0xF4511E: "RAVAGER",
    0xE91E63: "WARBRINGER (boss)",
    0x7986CB: "OVERSEER (boss)",
    0x81C784: "PHANTOM",
    0xFF5722: "GUARDIAN",
    0xD01937: "OVERSEER_PRIME (boss final)",
    0xFFC107: "Quest item",
    0x673AB7: "TeleportPad",
}

for f in sorted(glob.glob('res/level*.png')):
    im = Image.open(f).convert('RGB')
    found = {}
    for y in range(im.height):
        for x in range(im.width):
            r, g, b = im.getpixel((x, y))
            c = (r << 16) | (g << 8) | b
            if c in SPECIAL:
                found.setdefault(c, []).append((x, y))
    print(f"\n== {f} ({im.width}x{im.height}) ==")
    if not found:
        print("  (nenhuma cor especial)")
    for c, pts in sorted(found.items()):
        print(f"  {SPECIAL[c]}: {len(pts)}x -> ex: {pts[0]}")
