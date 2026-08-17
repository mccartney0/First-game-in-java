from PIL import Image
im = Image.open('res/level3.png').convert('RGBA')
W, H = im.size
for y in range(H):
    row = ''
    for x in range(W):
        p = im.getpixel((x, y))
        if p == (0, 0, 0, 255):
            row += '.'
        elif p == (255, 255, 255, 255):
            row += '#'
        else:
            row += '?'
    print(y, row)
