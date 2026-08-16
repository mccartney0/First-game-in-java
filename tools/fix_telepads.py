#!/usr/bin/env python3
"""Garante que bin/level2.png e bin/level5.png tenham EXATAMENTE UM PAR
de TeleportPad (cor 0xFF673AB7), em posições acessíveis ao jogador.
Os mapas originais já continham pads (3 no level2, 4 no level5) que o
jogo original usava — mantemos o par mais útil de cada mapa.
"""
from PIL import Image

PAD = (0x67, 0x3A, 0xB7)


def load(path):
    return Image.open(path).convert("RGBA")


def pad_positions(img):
    return [
        (x, y)
        for y in range(img.height)
        for x in range(img.width)
        if img.getpixel((x, y))[:3] == PAD
    ]


def set_pixel(img, x, y, rgba):
    img.putpixel((x, y), rgba)


def clear_all_pads(img):
    for x, y in pad_positions(img):
        # Restaurar para chão preto (floor) removendo o pad
        set_pixel(img, x, y, (0, 0, 0, 255))


def add_pair(img, a, b):
    set_pixel(img, a[0], a[1], PAD + (255,))
    set_pixel(img, b[0], b[1], PAD + (255,))


def cell(img, x, y):
    return img.getpixel((x, y))[:3]


def main():
    maps = {
        # level2 (34x22): par conectando o canto superior-esquerdo à
        # câmara central-direita. Células já validadas como chão preto.
        "bin/level2.png": [(1, 1), (24, 12)],
        # level5 (36x26): par conectando o canto superior-esquerdo à
        # ala inferior-direita.
        "bin/level5.png": [(2, 2), (25, 15)],
    }
    for path, (a, b) in maps.items():
        img = load(path)
        before = pad_positions(img)
        print(f"{path}: pads antes = {before}")
        # As posições alvo devem ser chão preto atualmente (sem pad),
        # ou pad existente — em ambos os casos vamos reescrevê-los.
        for x, y in [a, b]:
            assert cell(img, x, y) == (0, 0, 0) or cell(img, x, y) == PAD, (
                f"alvo ({x},{y}) é {cell(img, x, y)}, não é chão"
            )
        clear_all_pads(img)
        add_pair(img, a, b)
        after = pad_positions(img)
        print(f"{path}: pads depois = {after}")
        assert after == [a, b], f"resultado inesperado: {after}"
        img.save(path)
    print("OK")


if __name__ == "__main__":
    main()
