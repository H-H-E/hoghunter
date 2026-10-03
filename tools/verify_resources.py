#!/usr/bin/env python3
"""Deterministic resource integrity checks for Minecraft 1.21.1 (stdlib only).

This catches missing client assets and silently ignored legacy data directories
before launching Minecraft. Vanilla model/texture references are checked against
the actual ModDev Minecraft client resource JAR when available, or an explicit
--minecraft-resources JAR. A pre-build run reports that optional check as skipped.
Explicit model geometry must not inherit Minecraft's generated-item marker.
This cannot prove rendering, audibility, vanilla gameplay IDs, or balance.
Run from any cwd.
"""

from __future__ import annotations

import argparse
import gzip
import io
import json
import re
import struct
import sys
import zlib
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
ASSETS = RES / "assets/hoghunter"
DATA = RES / "data/hoghunter"
JAVA = ROOT / "src/main/java/com/hoghunter"
errors: list[str] = []
checks = 0


def check(condition, message):
    global checks
    checks += 1
    if not condition:
        errors.append(message)


def label(path):
    return str(path.relative_to(ROOT))


def object_no_duplicates(pairs):
    out = {}
    for key, value in pairs:
        if key in out:
            raise ValueError(f"duplicate JSON key {key!r}")
        out[key] = value
    return out


def read_json(path):
    try:
        return json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=object_no_duplicates)
    except (ValueError, OSError) as exc:
        check(False, f"{label(path)}: {exc}")
        return {}


def registrations(filename, methods):
    source = (JAVA / "content" / filename).read_text(encoding="utf-8")
    return set(re.findall(r'\b(?:' + "|".join(methods) + r')\(\s*"([a-z0-9_./-]+)"', source))


def local_ref(value, directory, suffix):
    if not isinstance(value, str) or not value.startswith("hoghunter:"):
        return
    path = ASSETS / directory / (value.split(":", 1)[1] + suffix)
    check(path.is_file(), f"missing {directory} resource {value} ({label(path)})")


class VanillaAssets:
    """An index of real Minecraft assets; no guessed resource-name whitelist."""

    def __init__(self, path):
        self.path = path
        with zipfile.ZipFile(path) as archive:
            self.names = set(archive.namelist())
        if "assets/minecraft/models/item/generated.json" not in self.names:
            raise ValueError("JAR does not contain Minecraft client models")
        self.references = set()
        self.models = {}

    def ref(self, value, directory, suffix, source):
        if not isinstance(value, str) or value.startswith("#"):
            return
        namespace, separator, resource = value.partition(":")
        if not separator:
            namespace, resource = "minecraft", value
        if namespace != "minecraft":
            return
        if directory == "models" and resource == "builtin/generated":
            # ModelBakery supplies this marker in code, not as an archive entry.
            return
        name = f"assets/minecraft/{directory}/{resource}{suffix}"
        self.references.add(name)
        check(name in self.names,
              f"{label(source)}: missing vanilla {directory} reference {value!r}; "
              f"{name} is absent from {self.path.name}")

    def model(self, resource, source):
        """Read only the native parents reached by the mod's model graph."""
        if resource not in self.models:
            self.ref("minecraft:" + resource, "models", ".json", source)
            name = f"assets/minecraft/models/{resource}.json"
            data = {}
            if name in self.names:
                try:
                    with zipfile.ZipFile(self.path) as archive:
                        data = json.loads(archive.read(name), object_pairs_hook=object_no_duplicates)
                    if not isinstance(data, dict):
                        raise ValueError("model must be a JSON object")
                except (OSError, ValueError, KeyError, zipfile.BadZipFile) as exc:
                    check(False, f"{label(source)}: cannot read vanilla parent {name}: {exc}")
                    data = {}
            self.models[resource] = data
        return self.models[resource]


def minecraft_assets(explicit=None):
    path = explicit
    if path is None:
        properties = (ROOT / "gradle.properties").read_text(encoding="utf-8")
        version = re.search(r"^neoforge_version\s*=\s*([^\s#]+)", properties, re.MULTILINE)
        if version:
            candidate = ROOT / "build/moddev/artifacts" / (
                f"neoforge-{version.group(1)}-client-extra-aka-minecraft-resources.jar")
            if candidate.is_file():
                path = candidate
    if path is None:
        print("Vanilla model/texture validation skipped: client resource JAR is not built; "
              "run the Gradle build or pass --minecraft-resources PATH.")
        return None
    try:
        return VanillaAssets(path)
    except (OSError, ValueError, zipfile.BadZipFile) as exc:
        check(False, f"cannot validate Minecraft client resources {path}: {exc}")
        return None


def model_asset_refs(path, data, vanilla):
    """Check parents, overrides, blockstate models, texture bindings and literal faces."""
    for node in walk(data):
        for key in ("parent", "model"):
            local_ref(node.get(key), "models", ".json")
            if vanilla:
                vanilla.ref(node.get(key), "models", ".json", path)
        textures = node.get("textures", {})
        values = list(textures.values()) if isinstance(textures, dict) else []
        values.append(node.get("texture"))
        for value in values:
            local_ref(value, "textures", ".png")
            if vanilla:
                vanilla.ref(value, "textures", ".png", path)


def model_geometry_parents(path, data, json_data, vanilla):
    """Reject authored elements that ModelBakery would replace with a flat item."""
    if not data.get("elements"):
        return
    model_id = "hoghunter:" + path.relative_to(ASSETS / "models").with_suffix("").as_posix()
    chain, seen = [model_id], {model_id}
    parent = data.get("parent")
    # These standard aliases also catch the observed handheld regression before
    # Gradle has downloaded native assets. With a JAR, read the actual JSON chain.
    prebuild_parents = {
        "minecraft:item/handheld": "minecraft:item/generated",
        "minecraft:item/generated": "minecraft:builtin/generated",
    }
    while isinstance(parent, str) and parent:
        parent_id = parent if ":" in parent else "minecraft:" + parent
        chain.append(parent_id)
        if parent_id in seen:
            check(False, f"{label(path)}: cyclic model parent chain: {' -> '.join(chain)}")
            return
        seen.add(parent_id)
        if parent_id == "minecraft:builtin/generated":
            check(False, f"{label(path)}: explicit elements inherit builtin/generated via "
                  f"{' -> '.join(chain)}; Minecraft replaces this geometry with a generated "
                  "item model. Use a geometry-preserving parent such as minecraft:block/block.")
            return
        namespace, resource = parent_id.split(":", 1)
        local_path = RES / "assets" / namespace / "models" / (resource + ".json")
        if local_path in json_data:
            parent = json_data[local_path].get("parent")
        elif namespace == "minecraft" and vanilla:
            parent = vanilla.model(resource, path).get("parent")
        else:
            parent = prebuild_parents.get(parent_id)
    check(True, f"{label(path)}: explicit geometry has no generated-item parent")


def java_vanilla_texture_refs(vanilla):
    # Resolve literal client ResourceLocations and the simple named-material
    # factory used by HogDetailsLayer, deriving its prefix/suffix from Java.
    if vanilla is None:
        return
    for path in sorted((JAVA / "client").rglob("*.java")):
        source = path.read_text(encoding="utf-8")
        for resource in re.findall(r'ResourceLocation\.withDefaultNamespace\("(textures/[^"+]+\.png)"\)', source):
            vanilla.ref("minecraft:" + resource.removeprefix("textures/").removesuffix(".png"), "textures", ".png", path)
        factories = re.findall(
            r'private\s+static\s+ResourceLocation\s+(\w+)\(String\s+(\w+)\)\s*\{\s*'
            r'return\s+ResourceLocation\.withDefaultNamespace\("(textures/[^"+]*)"\s*\+\s*\2\s*\+\s*"(\.png)"\);', source)
        for method, _parameter, prefix, suffix in factories:
            for value in re.findall(r'\b' + re.escape(method) + r'\("([^"+]+)"\)', source):
                vanilla.ref("minecraft:" + prefix.removeprefix("textures/") + value, "textures", suffix, path)


def walk(value):
    if isinstance(value, dict):
        yield value
        for nested in value.values():
            yield from walk(nested)
    elif isinstance(value, list):
        for nested in value:
            yield from walk(nested)


def item_ref(value, items, context):
    if isinstance(value, str) and value.startswith("hoghunter:"):
        check(value.split(":", 1)[1] in items, f"{context}: unregistered item {value}")


def check_png(path):
    raw = path.read_bytes()
    check(raw[:8] == b"\x89PNG\r\n\x1a\n", f"{label(path)}: invalid PNG signature")
    if raw[:8] != b"\x89PNG\r\n\x1a\n":
        return
    pos, dimensions, ended = 8, None, False
    try:
        while pos < len(raw):
            size = struct.unpack_from(">I", raw, pos)[0]
            kind = raw[pos + 4:pos + 8]
            body = raw[pos + 8:pos + 8 + size]
            crc = struct.unpack_from(">I", raw, pos + 8 + size)[0]
            check(zlib.crc32(kind + body) & 0xFFFFFFFF == crc, f"{label(path)}: bad {kind!r} CRC")
            if kind == b"IHDR":
                dimensions = struct.unpack_from(">II", body)
                check(all(value > 0 for value in dimensions), f"{label(path)}: empty PNG")
            pos += size + 12
            if kind == b"IEND":
                ended = True
                break
        check(ended and dimensions is not None and pos == len(raw), f"{label(path)}: incomplete PNG")
    except (struct.error, ValueError) as exc:
        check(False, f"{label(path)}: truncated PNG ({exc})")
    metadata = path.with_name(path.name + ".mcmeta")
    if metadata.exists() and dimensions:
        animation = read_json(metadata).get("animation", {})
        frame_width = animation.get("width", min(dimensions))
        frame_height = animation.get("height", min(dimensions))
        check(frame_width > 0 and frame_height > 0, f"{label(metadata)}: invalid frame size")
        if frame_width > 0 and frame_height > 0:
            check(dimensions[0] % frame_width == 0 and dimensions[1] % frame_height == 0,
                  f"{label(metadata)}: frames do not divide texture")
            frame_count = (dimensions[0] // frame_width) * (dimensions[1] // frame_height)
            for frame in animation.get("frames", []):
                index = frame.get("index", -1) if isinstance(frame, dict) else frame
                check(isinstance(index, int) and 0 <= index < frame_count,
                      f"{label(metadata)}: frame {index} outside {frame_count} frames")


class NbtReader:
    """Independent decoder; does not import the GameTest structure encoder."""

    def __init__(self, data):
        self.stream = io.BytesIO(data)

    def read(self, length):
        result = self.stream.read(length)
        if len(result) != length:
            raise ValueError("truncated NBT")
        return result

    def number(self, fmt):
        return struct.unpack(">" + fmt, self.read(struct.calcsize(">" + fmt)))[0]

    def string(self):
        return self.read(self.number("H")).decode("utf-8")

    def payload(self, kind):
        if kind in {1, 2, 3, 4, 5, 6}:
            return self.number({1: "b", 2: "h", 3: "i", 4: "q", 5: "f", 6: "d"}[kind])
        if kind == 8:
            return self.string()
        if kind == 10:
            out = {}
            while True:
                child = self.number("B")
                if child == 0:
                    return out
                name = self.string()
                if name in out:
                    raise ValueError(f"duplicate NBT field {name!r}")
                out[name] = self.payload(child)
        if kind == 9:
            child, length = self.number("B"), self.number("i")
            if length < 0 or length > 1_000_000:
                raise ValueError("invalid NBT list size")
            return [self.payload(child) for _ in range(length)]
        if kind in {7, 11, 12}:
            length = self.number("i")
            if length < 0 or length > 1_000_000:
                raise ValueError("invalid NBT array size")
            return [self.number({7: "b", 11: "i", 12: "q"}[kind]) for _ in range(length)]
        raise ValueError(f"unsupported NBT tag {kind}")


def check_template(path):
    try:
        reader = NbtReader(gzip.decompress(path.read_bytes()))
        check(reader.number("B") == 10, f"{label(path)}: root is not a compound")
        reader.string()
        root = reader.payload(10)
        check(not reader.stream.read(), f"{label(path)}: trailing NBT data")
        size = root.get("size", [])
        check(len(size) == 3 and all(isinstance(n, int) and n > 0 for n in size),
              f"{label(path)}: invalid dimensions")
        palette = root.get("palette", [])
        check(bool(palette) and all("Name" in entry for entry in palette),
              f"{label(path)}: malformed palette compounds")
        if not palette or any("Name" not in entry for entry in palette) or len(size) != 3:
            return
        positions = set()
        floor = 0
        for block in root.get("blocks", []):
            pos, state = block.get("pos", []), block.get("state", -1)
            valid = len(pos) == 3 and all(0 <= coordinate < bound for coordinate, bound in zip(pos, size))
            check(valid, f"{label(path)}: malformed/outside block position {pos}")
            check(isinstance(state, int) and 0 <= state < len(palette), f"{label(path)}: invalid palette index {state}")
            if valid and isinstance(state, int) and 0 <= state < len(palette):
                position = tuple(pos)
                check(position not in positions, f"{label(path)}: duplicate position {pos}")
                positions.add(position)
                if pos[1] == 0 and palette[state]["Name"] == "minecraft:stone":
                    floor += 1
        check(floor == size[0] * size[2], f"{label(path)}: incomplete stone floor")
        check(len(positions) == size[0] * size[1] * size[2], f"{label(path)}: missing explicit air/floor blocks")
    except (OSError, ValueError, KeyError, TypeError, struct.error) as exc:
        check(False, f"{label(path)}: {exc}")


def check_jar(path):
    try:
        with zipfile.ZipFile(path) as archive:
            names = set(archive.namelist())
            for source in RES.rglob("*"):
                if not source.is_file():
                    continue
                name = source.relative_to(RES).as_posix()
                check(name in names, f"release JAR is missing {name}")
                if name in names and name != "META-INF/neoforge.mods.toml":
                    check(archive.read(name) == source.read_bytes(), f"release JAR has stale resource {name}")
            for source in JAVA.rglob("*.java"):
                name = source.relative_to(JAVA.parent.parent).with_suffix(".class").as_posix()
                check(name in names, f"release JAR is missing compiled class {name}")
            metadata = archive.read("META-INF/neoforge.mods.toml").decode("utf-8")
            check("${" not in metadata, "release JAR mod metadata contains unresolved placeholders")
            check('modId="hoghunter"' in metadata.replace(" ", ""), "release JAR has wrong mod id")
            for name in names:
                check(not re.search(r"data/hoghunter/(recipes|loot_tables|tags/items|tags/blocks|tags/entity_types)/", name),
                      f"release JAR contains silently ignored legacy data path {name}")
        print(f"Release JAR inspected: {path.name}")
    except (OSError, KeyError, ValueError, zipfile.BadZipFile) as exc:
        check(False, f"cannot validate release JAR {path}: {exc}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jar", type=Path, help="also check the exact release JAR against current source resources")
    parser.add_argument("--minecraft-resources", "--mcjar", type=Path,
                        help="Minecraft client resource JAR; auto-detected from ModDev after build")
    args = parser.parse_args()
    vanilla = minecraft_assets(args.minecraft_resources)
    items = registrations("HogItems.java", ["registerItem", "armor", "item", "materialItem"])
    blocks = registrations("HogBlocks.java", ["register", "registerBlock"])
    entities = registrations("HogEntities.java", ["register"])
    sounds = registrations("HogSounds.java", ["sound"])
    check(len(items) >= 21 and len(blocks) >= 4 and len(entities) == 7,
          "Registry extraction failed; review source registration patterns in verify_resources.py")
    json_files = sorted(RES.rglob("*.json"))
    json_data = {path: read_json(path) for path in json_files}
    lang = json_data.get(ASSETS / "lang/en_us.json", {})

    for item in sorted(items):
        check((ASSETS / "models/item" / f"{item}.json").is_file(), f"item {item}: missing inventory model")
        check(f"item.hoghunter.{item}" in lang or f"block.hoghunter.{item}" in lang,
              f"item {item}: missing English display name")
    for block in sorted(blocks):
        check((ASSETS / "blockstates" / f"{block}.json").is_file(), f"block {block}: missing blockstate")
        check((DATA / "loot_table/blocks" / f"{block}.json").is_file(), f"block {block}: missing loot table")
    for entity in sorted(entities):
        check((ASSETS / "textures/entity/hog" / f"{entity}.png").is_file(), f"entity {entity}: missing texture")
        check((DATA / "loot_table/entities" / f"{entity}.json").is_file(), f"entity {entity}: missing loot table")
        check(f"entity.hoghunter.{entity}" in lang, f"entity {entity}: missing English display name")

    for path, data in json_data.items():
        if path.is_relative_to(ASSETS / "models") or path.is_relative_to(ASSETS / "blockstates"):
            model_asset_refs(path, data, vanilla)
        if path.is_relative_to(ASSETS / "models"):
            model_geometry_parents(path, data, json_data, vanilla)
        if path.is_relative_to(DATA / "recipe"):
            kind = data.get("type")
            result = data.get("result", {})
            if isinstance(result, dict):
                item_ref(result.get("id"), items, label(path))
            elif isinstance(result, str):
                item_ref(result, items, label(path))
            if kind in {"minecraft:crafting_shaped", "minecraft:crafting_shapeless"}:
                result = data.get("result", {})
                check(isinstance(result, dict) and "id" in result and "item" not in result,
                      f"{label(path)}: 1.21.1 crafting result requires id, not item")
                item_ref(result.get("id"), items, label(path))
                if kind == "minecraft:crafting_shaped":
                    pattern, key = data.get("pattern", []), data.get("key", {})
                    check(1 <= len(pattern) <= 3 and all(1 <= len(row) <= 3 for row in pattern)
                          and len({len(row) for row in pattern}) == 1, f"{label(path)}: invalid crafting grid")
                    used = set("".join(pattern)) - {" "}
                    check(used == set(key), f"{label(path)}: unmatched recipe symbols")
                else:
                    check(1 <= len(data.get("ingredients", [])) <= 9, f"{label(path)}: invalid shapeless ingredient count")
            for value in walk(data):
                item_ref(value.get("item"), items, label(path))
                tag = value.get("tag", "")
                if isinstance(tag, str) and tag.startswith("hoghunter:"):
                    check((DATA / "tags/item" / (tag.split(":", 1)[1] + ".json")).is_file(),
                          f"{label(path)}: missing ingredient tag {tag}")
        if path.is_relative_to(DATA / "loot_table"):
            for value in walk(data):
                if value.get("type") == "minecraft:item":
                    item_ref(value.get("name"), items, label(path))
                check(value.get("function") != "minecraft:survives_explosion",
                      f"{label(path)}: survives_explosion is a condition, not a function")
        if path.is_relative_to(DATA / "tags/item"):
            for value in data.get("values", []):
                name = value.get("id") if isinstance(value, dict) else value
                item_ref(name, items, label(path))
                if isinstance(name, str) and name.startswith("#hoghunter:"):
                    check((DATA / "tags/item" / (name.split(":", 1)[1] + ".json")).is_file(),
                          f"{label(path)}: missing nested item tag {name}")
        if path.is_relative_to(DATA / "tags/block"):
            for value in data.get("values", []):
                name = value.get("id") if isinstance(value, dict) else value
                if isinstance(name, str) and name.startswith("#hoghunter:"):
                    check((DATA / "tags/block" / (name.split(":", 1)[1] + ".json")).is_file(),
                          f"{label(path)}: missing nested block tag {name}")
                elif isinstance(name, str) and name.startswith("hoghunter:"):
                    check(name.split(":", 1)[1] in blocks, f"{label(path)}: unregistered block {name}")
        if path.is_relative_to(DATA / "advancement"):
            for recipe in data.get("rewards", {}).get("recipes", []):
                if recipe.startswith("hoghunter:"):
                    check((DATA / "recipe" / (recipe.split(":", 1)[1] + ".json")).is_file(),
                          f"{label(path)}: rewards a missing recipe {recipe}")

    for legacy in ["recipes", "loot_tables", "tags/items", "tags/blocks", "tags/entity_types", "structures"]:
        path = DATA / legacy
        check(not path.exists() or not any(path.rglob("*.*")), f"{label(path)}: obsolete 1.21.1 data directory")
    sound_data = json_data.get(ASSETS / "sounds.json", {})
    for sound in sorted(sounds):
        check(sound in sound_data, f"registered sound {sound}: no sounds.json definition")
    for key, value in sound_data.items():
        check(bool(value.get("sounds")), f"sound {key}: empty sound list")
        if "subtitle" in value:
            check(value["subtitle"] in lang, f"sound {key}: missing subtitle translation")
        for entry in value.get("sounds", []):
            name = entry.get("name") if isinstance(entry, dict) else entry
            if isinstance(entry, dict) and entry.get("type") == "event":
                if name.startswith("hoghunter:"):
                    check(name.split(":", 1)[1] in sound_data, f"sound {key}: missing event {name}")
            else:
                local_ref(name, "sounds", ".ogg")
    for path in sorted(ASSETS.rglob("*.png")):
        check_png(path)
    for path in sorted(ASSETS.rglob("*.ogg")):
        raw = path.read_bytes()
        check(len(raw) > 64 and raw[:4] == b"OggS", f"{label(path)}: invalid/truncated OGG file")
    check_template(DATA / "structure/empty.nbt")
    java_vanilla_texture_refs(vanilla)
    if vanilla:
        print(f"Vanilla model/texture references checked: {len(vanilla.references)} distinct assets from {vanilla.path.name}")
    if args.jar:
        check_jar(args.jar)

    if errors:
        print(f"Resource integrity FAILED: {len(errors)} errors across {checks} checks")
        for error in errors:
            print(" - " + error)
        return 1
    print(f"Resource integrity passed: {checks} checks; {len(items)} items, {len(blocks)} blocks, "
          f"{len(entities)} entities, {len(sounds)} registered sounds, {len(json_files)} JSON files")
    return 0


if __name__ == "__main__":
    sys.exit(main())
