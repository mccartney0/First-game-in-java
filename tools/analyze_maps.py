"""Analisa a paleta e distribuição de cores dos mapas level1-6."""
from PIL import Image
from collections import Counter

for i in range(1, 7):
    im = Image.open(f"res/level{i}.png").convert("RGB")
    w, h = im.size
    px = im.getdata()
    c = Counter(px)
    top = c.most_common(6)
    print(f"level{i}.png {w}x{h} — {len(c)} cores")
    for color, n in top:
        print(f"  rgb{color}: {n} px ({n*100/(w*h):.1f}%)")
