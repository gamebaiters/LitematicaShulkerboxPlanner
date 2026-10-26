# Generates the 128x128 mod icon (pixel art shulker box with a checklist). Run: python tools/gen_icon.py
import os, struct, zlib

# 16x16 pixel art, upscaled 8x
P = {
    '.': (0, 0, 0, 0),
    'k': (40, 20, 52, 255),     # outline
    'd': (100, 58, 128, 255),   # dark purple
    'm': (139, 87, 168, 255),   # purple
    'l': (176, 128, 200, 255),  # light purple
    'y': (240, 214, 120, 255),  # shulker face/eye
    'w': (245, 245, 245, 255),  # paper
    'g': (60, 190, 80, 255),    # green check
    's': (150, 150, 160, 255),  # paper lines
}

ART = [
    "................",
    ".kkkkkkkkkkkkkk.",
    ".kllllllllllllk.",
    ".kmmmmmmmmmmmmk.",
    ".kmmmmmmmmmmmmk.",
    ".kddddddddddddk.",
    ".kkkkkkyykkkkkk.",
    ".kmmmmmyymmmmmk.",
    ".kmmmmmmmmkkkkkk",
    ".kmmmmmmmmkwwwwk",
    ".kmmmmmmmmkgwssk",
    ".kddddddddkwgwwk",
    ".kddddddddkgwssk",
    ".kddddddddkwwwwk",
    ".kkkkkkkkkkkkkkk",
    "................",
]

def chunk(tag, data):
    c = struct.pack(">I", len(data)) + tag + data
    return c + struct.pack(">I", zlib.crc32(tag + data) & 0xffffffff)

def write_png(path, scale):
    size = 16 * scale
    rows = []

    for y in range(size):
        row = bytearray([0])
        line = ART[y // scale]

        for x in range(size):
            row.extend(P[line[x // scale]])

        rows.append(bytes(row))

    png = bytes([0x89]) + b"PNG" + bytes([13, 10, 26, 10])
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(b"".join(rows), 9))
    png += chunk(b"IEND", b"")

    os.makedirs(os.path.dirname(path), exist_ok=True)

    with open(path, "wb") as f:
        f.write(png)

    print("written", path, len(png), "bytes")

root = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
write_png(os.path.join(root, "src", "main", "resources", "assets", "shulkerplanner", "icon.png"), 8)
write_png(os.path.join(root, "docs", "images", "icon.png"), 32)
