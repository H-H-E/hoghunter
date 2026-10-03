"""Generate the deterministic, floored Hog Hunter GameTest arena (stdlib only).

The 33 x 8 x 33 arena keeps 16-block boss effects inside each test's bounds.
Every air block is explicit so a repeated run clears earlier obstacles. List
entries contain NBT payloads, not named compound tags: the latter silently nest
all palette/block properties under an empty key and produce an unusable floor.

Run without arguments to regenerate, or with --check for a nonmutating CI check.
"""

import argparse
import gzip
import struct
from pathlib import Path

TAG_END, TAG_INT, TAG_STRING, TAG_LIST, TAG_COMPOUND = 0, 3, 8, 9, 10
DATA_VERSION = 3955  # Minecraft 1.21.1
SIZE = (33, 8, 33)
OUT = Path(__file__).resolve().parent.parent / "src/main/resources/data/hoghunter/structure/empty.nbt"


def string_payload(value):
    raw = value.encode("utf-8")
    return struct.pack(">H", len(raw)) + raw


def named(tag_type, name, payload):
    return bytes([tag_type]) + string_payload(name) + payload


def compound_payload(entries):
    return b"".join(entries) + bytes([TAG_END])


def int_tag(name, value):
    return named(TAG_INT, name, struct.pack(">i", value))


def int_list(name, values):
    return named(TAG_LIST, name, struct.pack(">Bi", TAG_INT, len(values))
                 + b"".join(struct.pack(">i", value) for value in values))


def compound_list(name, entries):
    return named(TAG_LIST, name, struct.pack(">Bi", TAG_COMPOUND, len(entries)) + b"".join(entries))


def generate():
    palette = [compound_payload([named(TAG_STRING, "Name", string_payload(block))])
               for block in ("minecraft:stone", "minecraft:air")]
    blocks = [compound_payload([int_list("pos", [x, y, z]), int_tag("state", 0 if y == 0 else 1)])
              for x in range(SIZE[0]) for y in range(SIZE[1]) for z in range(SIZE[2])]
    root = named(TAG_COMPOUND, "", compound_payload([
        int_tag("DataVersion", DATA_VERSION), int_list("size", SIZE),
        compound_list("palette", palette), compound_list("blocks", blocks),
        compound_list("entities", []),
    ]))
    encoded = bytearray(gzip.compress(root, mtime=0))
    # Python/zlib versions may write the host OS into byte 9. Canonicalize it so
    # --check compares the same template on Windows, Linux and macOS.
    encoded[9] = 255
    return bytes(encoded)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="fail if the checked-in template differs")
    args = parser.parse_args()
    data = generate()
    if args.check:
        if not OUT.exists() or OUT.read_bytes() != data:
            parser.exit(1, "GameTest template is stale; run python tools/make_gametest_structure.py\n")
        print(f"GameTest template verified ({' x '.join(map(str, SIZE))}, {len(data)} bytes)")
    else:
        OUT.parent.mkdir(parents=True, exist_ok=True)
        OUT.write_bytes(data)
        print(f"wrote {OUT} ({len(data)} bytes)")


if __name__ == "__main__":
    main()
