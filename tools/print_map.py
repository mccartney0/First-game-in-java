#!/usr/bin/env python3
"""Imprime um mapa PNG gerado como grid de texto para conferência."""
import sys
from PIL import Image

TARGETS = {
    (0, 0, 0): '.',
    (255, 255, 255): '#',
    (255, 0, 0): 'E',
    (0, 38, 255): 'P',
    (63, 81, 181): 'G',
    (76, 175, 80): 'Q',
    (128, 128, 128): 'X',
    (233, 30, 99): 'B',
    (156, 39, 176): 'T',
    (0, 188, 212): 'A',
}

def main():
    path = sys.argv[1] if len(sys.argv) > 1 else 'res/level1.png'
    img = Image.open(path).convert('RGB')
    px = img.load()
    for y in range(img.height):
        out = ''
        for x in range(img.width):
            c = px[x, y][:3]
            out += TARGETS.get(c, '?')
        print(out)

if __name__ == '__main__':
    main()
