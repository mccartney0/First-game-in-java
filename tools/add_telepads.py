#!/usr/bin/env python3
"""Adiciona pares de TeleportPad (cor 0xFF673AB7) aos mapas level2 e level5.

Os pads são colocados em células de chão válidas (verde 0xFF7CB342) e
emparelhados um-para-um dentro do mesmo mapa, permitindo teste do
cooldown global e do teleporte pad->par.
"""
from PIL import Image

PAD = (0x67, 0x3A, 0xB7)


def set_pad(path, cells):
    img = Image.open(path).convert("RGBA")
    for cx, cy in cells:
        img.putpixel((cx, cy), PAD + (255,))
    img.save(path)
    print(f"{path}: pads em {cells}")


def main():
    # level2 (33x23 aprox): par em câmaras opostas
    img2 = Image.open("bin/level2.png").convert("RGBA")
    print("level2 size:", img2.size)
    # level5: par conectando ala esquerda e ala direita
    img5 = Image.open("bin/level5.png").convert("RGBA")
    print("level5 size:", img5.size)

    # Encontrar células de chão válidas (verde) próximas às posições desejadas
    def valid_near(img, cx, cy, radius=4):
        for dx in range(-radius, radius + 1):
            for dy in range(-radius, radius + 1):
                x, y = cx + dx, cy + dy
                if 0 <= x < img.width and 0 <= y < img.height:
                    if img.getpixel((x, y))[:3] == (0x00, 0x00, 0x00):
                        return (x, y)
        return None

    img2p = [
        valid_near(img2, 5, 5),
        valid_near(img2, img2.width - 6, img2.height - 6),
    ]
    img5p = [
        valid_near(img5, 6, 6),
        valid_near(img5, img5.width - 7, img5.height - 7),
    ]

    print("level2 pads:", img2p)
    print("level5 pads:", img5p)

    for path, cells, label in [("bin/level2.png", img2p, "level2"),
                               ("res/level2.png", img2p, "res/level2"),
                               ("bin/level5.png", img5p, "level5"),
                               ("res/level5.png", img5p, "res/level5")]:
        if all(cells):
            set_pad(path, cells)
        else:
            print(f"{label}: sem células de chão válidas ({cells})")


if __name__ == "__main__":
    main()
