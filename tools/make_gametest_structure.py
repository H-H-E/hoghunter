"""Generate the empty GameTest structure template used by com.hoghunter.test.

Minecraft's release jars do not ship any gametest structure template, so a mod that
wants GameTests to run has to provide one. This writes
src/main/resources/data/hoghunter/structure/empty.nbt: a 7x7x7 stone floor with air
above it. The floor matters -- GameTest entities are spawned inside the structure
bounds, and without it they fall out of the world before the test can assert on them.

Pure-stdlib NBT writer, so the build has no new Python dependency.
"""

import gzip
import struct
from pathlib import Path

TAG_END = 0
TAG_INT = 3
TAG_STRING = 8
TAG_LIST = 9
TAG_COMPOUND = 10

DATA_VERSION = 3955  # Minecraft 1.21.1
SIZE = 7
OUT = Path(__file__).resolve().parent.parent / "src/main/resources/data/hoghunter/structure/empty.nbt"


def _string_payload(value):
    raw = value.encode("utf-8")
    return struct.pack(">H", len(raw)) + raw


def tag(tag_type, payload=b""):
    return struct.pack(">B", tag_type) + payload


def named(tag_type, name, payload):
    return tag(tag_type, _string_payload(name) + payload)


def compound(name, entries):
    body = b"".join(entries) + tag(TAG_END)
    return named(TAG_COMPOUND, name, body)


def int_list(name, values):
    payload = struct.pack(">Bi", TAG_INT, len(values))
    payload += b"".join(struct.pack(">i", v) for v in values)
    return named(TAG_LIST, name, payload)


def compound_list(name, items):
    payload = struct.pack(">Bi", TAG_COMPOUND, len(items))
    for item in items:
        payload += item + tag(TAG_END)
    return named(TAG_LIST, name, payload)


def string_list(name, values):
    payload = struct.pack(">Bi", TAG_STRING, len(values))
    for value in values:
        payload += _string_payload(value)
    return named(TAG_LIST, name, payload)


def main():
    # Index 0 = stone (the floor), index 1 = air.
    palette = [
        compound("", [named(TAG_STRING, "Name", _string_payload("minecraft:stone"))]),
        compound("", [named(TAG_STRING, "Name", _string_payload("minecraft:air"))]),
    ]

    blocks = []
    for x in range(SIZE):
        for z in range(SIZE):
            blocks.append(
                compound("", [int_list("pos", [x, 0, z]), named(TAG_INT, "state", struct.pack(">i", 0))])
            )

    root = compound("", [
        int_list("size", [SIZE, SIZE, SIZE]),
        compound_list("palette", palette),
        compound_list("blocks", blocks),
        string_list("entities", []),
        named(TAG_INT, "DataVersion", struct.pack(">i", DATA_VERSION)),
    ])

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_bytes(gzip.compress(root, mtime=0))
    print(f"wrote {OUT} ({OUT.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
