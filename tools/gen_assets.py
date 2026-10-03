"""Deterministic, license-free Hog Hunter asset generator.

Usage: python tools/gen_assets.py --out src/main/resources --seed 20261003 --check
The manifest declares every generated PNG, animation metadata file and OGG.
Check mode regenerates into a temporary directory and never changes the output tree.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
import random
import shutil
import subprocess
import tempfile
import wave
from pathlib import Path

from PIL import Image, ImageDraw

ENTITY = {
    "boar_hog": ((116, 57, 49, 255), (196, 131, 83, 255), "low"),
    "spore_hog": ((54, 91, 68, 255), (145, 202, 135, 255), "hump"),
    "hook_hog": ((61, 64, 78, 255), (190, 177, 142, 255), "tall"),
    "screecher_hog": ((104, 45, 61, 255), (232, 114, 130, 255), "thin"),
    "ironback_hog": ((48, 52, 58, 255), (160, 168, 176, 255), "broad"),
    "mire_hog": ((46, 72, 56, 255), (139, 172, 103, 255), "mire"),
    "rootmother": ((73, 39, 45, 255), (179, 117, 67, 255), "boss"),
}
BLOCKS = {"bloodstone_ore": (76, 29, 38), "cursed_bone_block": (187, 172, 137),
          "hog_flesh_block": (117, 44, 46), "ritual_altar": (46, 37, 47),
          "depth_gate": (29, 36, 48), "corrupted_ore": (62, 38, 76)}
ITEMS = {"hog_cleaver": (184, 192, 201), "hog_flesh": (161, 56, 61),
         "bloodstone_chunk": (145, 44, 58), "cursed_bone": (199, 183, 144),
         "salt_cartridge": (225, 225, 211), "hunting_knife": (185, 194, 202),
         "hog_heart": (183, 39, 55), "sanity_lantern": (214, 157, 53),
         "field_lantern": (214, 157, 53), "hoghide_helmet": (115, 67, 53),
         "hoghide_chestplate": (115, 67, 53), "hoghide_leggings": (115, 67, 53),
         "hoghide_boots": (115, 67, 53)}

def rng_for(seed: int, asset_id: str) -> random.Random:
    return random.Random(hashlib.sha256(f"{seed}:{asset_id}".encode()).digest())

def _rgba(value):
    return value if len(value) == 4 else (*value, 255)

def generate_entity_skin(asset_id, size, palette, seed):
    """Create an eight-frame 64px hog skin strip with shading and corruption."""
    w, h = size
    base, accent, shape = palette
    out = Image.new("RGBA", (w, h * 8), (0, 0, 0, 0))
    r = rng_for(seed, asset_id)
    for frame in range(8):
        im = Image.new("RGBA", (w, h), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
        shadow = tuple(max(0, c - 30) for c in _rgba(base)[:3]) + (255,)
        deep = tuple(max(0, c - 55) for c in _rgba(base)[:3]) + (255,)
        light = tuple(min(255, c + 35) for c in _rgba(base)[:3]) + (255,)
        hoof = (31, 22, 24, 255)
        # Canonical 64x64 quadruped atlas: head in the top band, body in the
        # middle, and four clearly separated leg columns on the right.
        dip = 1 if frame in (2, 3) else 0
        stride = 2 if frame in (1, 5) else (-2 if frame in (3, 7) else 0)
        # Head box, ears, brow and shovel snout/front face.
        head_y = 1 + dip
        d.rectangle((1, head_y, 28, head_y + 14), fill=base)
        d.rectangle((3, head_y + 2, 26, head_y + 4), fill=shadow)  # brow shadow
        d.polygon([(2, head_y + 1), (0, head_y - 4), (6, head_y + 1)], fill=accent)
        d.polygon([(23, head_y + 1), (29, head_y - 4), (26, head_y + 5)], fill=accent)
        snout = (184, 126, 116, 255) if asset_id != "ironback_hog" else (148, 151, 156, 255)
        if asset_id == "rootmother": snout = (205, 177, 130, 255)
        d.rectangle((7, head_y + 8, 23, head_y + 15), fill=snout)
        d.rectangle((10, head_y + 11, 12, head_y + 13), fill=(40, 25, 27, 255)); d.rectangle((18, head_y + 11, 20, head_y + 13), fill=(40, 25, 27, 255))
        iris = (104, 255, 220, 255) if asset_id == "spore_hog" else ((255, 207, 75, 255) if asset_id == "rootmother" else (235, 49, 47, 255))
        d.rectangle((5, head_y + 5, 7, head_y + 7), fill=(18, 12, 14, 255)); d.point((6, head_y + 6), fill=iris)
        d.rectangle((21, head_y + 5, 23, head_y + 7), fill=(18, 12, 14, 255)); d.point((22, head_y + 6), fill=iris)
        tusk = (240, 226, 181, 255)
        if asset_id in ("boar_hog", "hook_hog", "rootmother", "screecher_hog"):
            d.line((8, head_y + 14, 6, head_y + 18), fill=tusk, width=2); d.line((22, head_y + 14, 24, head_y + 18), fill=tusk, width=2)
        if asset_id == "hook_hog":
            for x in (4, 9, 20, 25): d.line((x, head_y + 14, x + (1 if x < 15 else -1), head_y + 19), fill=tusk, width=1)
        if asset_id == "screecher_hog":
            d.rectangle((11, head_y + 14, 13, head_y + 17), fill=(28, 12, 20, 255)); d.rectangle((16, head_y + 14, 18, head_y + 17), fill=(28, 12, 20, 255))
        # Barrel torso with dorsal ridge, ribs and a recognizable underside.
        body_x0, body_x1 = (17, 58) if shape != "thin" else (24, 51)
        body_y0, body_y1 = (18, 39) if shape != "tall" else (16, 42)
        d.rounded_rectangle((body_x0, body_y0, body_x1, body_y1), radius=4, fill=base)
        d.line((body_x0 + 2, body_y0 + 2, body_x1 - 2, body_y0 + 2), fill=deep, width=2)
        for x in range(body_x0 + 7, body_x1 - 3, 7): d.line((x, body_y0 + 7, x - 1, body_y1 - 3), fill=shadow, width=1)
        if shape in ("hump", "broad", "boss"):
            d.ellipse((body_x0 + 7, body_y0 - 6, body_x1 - 7, body_y0 + 9), fill=accent)
        if asset_id == "ironback_hog":
            for x in range(body_x0 + 3, body_x1 - 2, 7): d.rectangle((x, body_y0 + 3, x + 4, body_y0 + 9), fill=light)
        if asset_id == "spore_hog":
            for x, y in ((25, 18), (34, 15), (43, 19), (49, 16)):
                d.ellipse((x - 3, y - 3, x + 3, y + 3), fill=(74, 223, 170, 255)); d.rectangle((x - 1, y + 2, x + 1, y + 6), fill=(47, 133, 102, 255))
        if asset_id == "mire_hog":
            for x in (body_x0 + 5, body_x1 - 8): d.line((x, body_y1 - 2, x - 2, 48), fill=(31, 96, 61, 220), width=2)
        if asset_id == "rootmother":
            d.line((31, body_y0, 27, 8), fill=(91, 146, 69, 255), width=2); d.line((39, body_y0, 45, 7), fill=(91, 146, 69, 255), width=2)
        # Four legs: separate columns, animated stride, dark hooves.
        legs = [(body_x0 + 4, 37, stride), (body_x0 + 12, 37, -stride), (body_x1 - 14, 37, -stride), (body_x1 - 6, 37, stride)]
        for x, y, offset in legs:
            x += offset; leg_h = 19 if shape not in ("tall", "boss") else 23
            d.rectangle((x, y, x + 5, min(62, y + leg_h)), fill=shadow)
            d.rectangle((x, min(62, y + leg_h - 3), x + 5, min(63, y + leg_h)), fill=hoof)
        # Controlled corruption marks read as wounds/veins rather than noise.
        for _ in range(5):
            x = r.randrange(body_x0 + 3, body_x1 - 3); y = r.randrange(body_y0 + 5, body_y1 - 2)
            d.line((x, y, x + r.choice((-3, 2, 4)), y + r.choice((2, 3, -2))), fill=(151, 31, 42, 215), width=1)
        if frame % 2: d.line((body_x0 + 3, body_y0 + 4, body_x1 - 3, body_y0 + 4), fill=(210, 47, 57, 150), width=1)
        out.paste(im, (0, frame * h))
    return out

def generate_block_texture(asset_id, size, palette, seed):
    w, h = size; frames = 4 if asset_id == "hog_flesh_block" else 1
    out = Image.new("RGBA", (w, h * frames), (0, 0, 0, 255)); r = rng_for(seed, asset_id)
    for f in range(frames):
        im = Image.new("RGBA", (w, h), (*palette, 255)); d = ImageDraw.Draw(im)
        for _ in range(32):
            x, y = r.randrange(w), r.randrange(h)
            c = tuple(max(0, min(255, v + r.randrange(-22, 23))) for v in palette) + (255,)
            d.rectangle((x, y, min(w - 1, x + r.randrange(1, 4)), min(h - 1, y + r.randrange(1, 4))), fill=c)
        out.paste(im, (0, f * h))
    return out

def generate_item_icon(asset_id, size, palette, seed):
    im = Image.new("RGBA", size, (0, 0, 0, 0)); d = ImageDraw.Draw(im); c = (*palette, 255)
    d.rounded_rectangle((3, 3, size[0] - 4, size[1] - 4), radius=3, fill=c)
    d.line((4, size[1] - 5, size[0] - 4, 4), fill=(245, 226, 181, 255), width=2)
    return im

def generate_gui_overlay(asset_id, size, palette, seed):
    im = Image.new("RGBA", size, (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    w, h = size
    if asset_id == "fear_meter": d.rectangle((3, 0, w - 4, h), fill=(*palette, 185))
    elif asset_id == "ritual_altar_panel": d.rounded_rectangle((2, 2, w - 3, h - 3), 6, outline=(*palette, 220), width=3)
    else:
        for i in range(24):
            alpha = int(155 * (1 - i / 24))
            d.rectangle((i, i, w - i - 1, h - i - 1), outline=(*palette, alpha), width=2)
    return im

def generate_armor_layer(asset_id, size, palette, seed):
    im = Image.new("RGBA", size, (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    d.rectangle((8, 4, 23, 18), fill=(*palette, 255)); d.rectangle((28, 4, 47, 25), fill=(*palette, 255))
    d.rectangle((8, 20, 22, 31), fill=(*palette, 255)); d.rectangle((42, 20, 55, 31), fill=(*palette, 255))
    return im

def generate_particle_sheet(asset_id, size, palette, seed):
    im = Image.new("RGBA", size, (0, 0, 0, 0)); d = ImageDraw.Draw(im); w, h = size
    d.ellipse((2, 2, w - 3, h - 3), fill=(*palette, 205)); d.point((w // 2, h // 2), fill=(255, 240, 180, 255))
    return im

def _sound(path: Path, kind: str, seed: int, asset_id: str):
    rate, seconds = 44100, {"squeal": .7, "attack": .35, "ambient": 1.1, "activate": .6}.get(kind, .5)
    # Preserve the original seed namespace, independent of cwd or --out.
    n = int(rate * seconds)
    r = rng_for(seed, f"src/main/resources/assets/hoghunter/sounds/{asset_id}.ogg")
    samples = bytearray()
    for i in range(n):
        t = i / rate; env = min(1, t * 18) * min(1, (seconds - t) * 9)
        freq = (145 + 110 * math.sin(t * 7)) if kind == "squeal" else (75 + 25 * math.sin(t * 3))
        val = math.sin(2 * math.pi * freq * t) * .55 + (r.random() - .5) * .18
        samples += int(max(-1, min(1, val * env)) * 32767).to_bytes(2, "little", signed=True)
    with tempfile.TemporaryDirectory() as td:
        wav = Path(td) / "source.wav"
        with wave.open(str(wav), "wb") as f:
            f.setnchannels(1); f.setsampwidth(2); f.setframerate(rate); f.writeframes(samples)
        subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", str(wav),
                        "-fflags", "+bitexact", "-flags:a", "+bitexact", "-map_metadata", "-1",
                        "-c:a", "libvorbis", "-q:a", "4", str(path)], check=True)

def declared_specs():
    """The complete owned output set; models/lang are maintained separately."""
    specs = []
    def png(path, generator, asset_id, size, palette, output_size=None):
        specs.append({"path": "assets/hoghunter/" + path, "generator": generator,
                      "asset_id": asset_id, "size": list(size), "palette": palette,
                      "output_size": list(output_size or size)})
    def animation(path, frametime, frames, interpolate):
        specs.append({"path": "assets/hoghunter/" + path, "generator": "animation",
                      "data": {"animation": {"frametime": frametime,
                              "interpolate": interpolate, "frames": list(range(frames))}}})
    for name, source in [(name, name) for name in ENTITY] + [("tusked_hog", "boar_hog"), ("mine_hog", "ironback_hog")]:
        path = f"textures/entity/hog/{name}.png"
        png(path, "generate_entity_skin", name, (64, 64), ENTITY[source], (64, 512))
        animation(path + ".mcmeta", 2, 8, True)
    png("textures/entity/hog/hog_blood_overlay.png", "generate_blood_overlay", "hog_blood_overlay", (64, 64), [])
    for name, color in BLOCKS.items():
        png(f"textures/block/{name}.png", "generate_block_texture", name, (16, 16), color,
            (16, 64) if name == "hog_flesh_block" else (16, 16))
        if name == "hog_flesh_block": animation(f"textures/block/{name}.png.mcmeta", 4, 4, False)
    for name, color in ITEMS.items():
        png(f"textures/item/{name}.png", "generate_item_icon", name, (16, 16), color)
    for name, size, color in [("hog_hunter_vignette", (256, 256), (40, 5, 12)),
                              ("hog_hunter_blood_edges", (256, 256), (130, 12, 18)),
                              ("ritual_altar_panel", (176, 166), (164, 101, 56)),
                              ("fear_meter", (16, 64), (172, 37, 45))]:
        png(f"textures/gui/{name}.png", "generate_gui_overlay", name, size, color)
    for name, color in [("hoghide_layer_1", (115, 67, 53)), ("hoghide_layer_2", (91, 50, 42))]:
        png(f"textures/models/armor/{name}.png", "generate_armor_layer", name, (64, 32), color)
    for name, color in [("blood_mist", (145, 25, 38)), ("spore_cloud", (94, 177, 117)), ("hog_spark", (227, 161, 63))]:
        png(f"textures/particle/{name}.png", "generate_particle_sheet", name, (16, 16), color)
    for name, kind in [("entity/boar_hog/ambient_01", "ambient"), ("entity/boar_hog/attack", "attack"),
                       ("entity/boar_hog/squeal", "squeal"), ("block/ritual_altar/activate", "activate")]:
        specs.append({"path": f"assets/hoghunter/sounds/{name}.ogg", "generator": "sound",
                      "asset_id": name, "kind": kind, "sample_rate": 44100, "channels": 1})
    return specs


def generate_blood_overlay(asset_id, size, palette, seed):
    im = Image.new("RGBA", size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)
    draw.line((7, 6, 18, 23, 12, 39), fill=(176, 20, 29, 210), width=2)
    draw.line((44, 8, 37, 26, 48, 47), fill=(115, 10, 22, 190), width=2)
    return im


def strict_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result: raise ValueError(f"Duplicate JSON key: {key}")
        result[key] = value
    return result


def read_manifest(path):
    manifest = json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=strict_object)
    if manifest.get("version") != 1: raise ValueError("Unsupported asset manifest version")
    specs = manifest.get("outputs", [])
    seen = set()
    expected = {spec["path"] for spec in declared_specs()}
    generators = {spec["path"]: spec["generator"] for spec in declared_specs()}
    for spec in specs:
        rel = spec["path"]
        if rel in seen: raise ValueError(f"Duplicate output: {rel}")
        if rel not in expected or Path(rel).is_absolute() or ".." in Path(rel).parts:
            raise ValueError(f"Unexpected output: {rel}")
        if spec.get("generator") != generators[rel]: raise ValueError(f"Wrong generator for {rel}")
        if len(spec.get("sha256", "")) != 64: raise ValueError(f"Missing checksum for {rel}")
        seen.add(rel)
    if seen != expected: raise ValueError(f"Manifest omits: {sorted(expected - seen)}")
    return manifest


def render_assets(root, specs, seed):
    if shutil.which("ffmpeg") is None: raise RuntimeError("ffmpeg with libvorbis is required")
    generators = {name: globals()[name] for name in (
        "generate_entity_skin", "generate_block_texture", "generate_item_icon",
        "generate_gui_overlay", "generate_armor_layer", "generate_particle_sheet", "generate_blood_overlay")}
    for spec in specs:
        path = root / spec["path"]
        path.parent.mkdir(parents=True, exist_ok=True)
        kind = spec["generator"]
        if kind == "animation":
            path.write_text(json.dumps(spec["data"], indent=2) + "\n", encoding="utf-8")
        elif kind == "sound":
            _sound(path, spec["kind"], seed, spec["asset_id"])
        else:
            palette = tuple(tuple(value) if isinstance(value, list) else value for value in spec["palette"])
            image = generators[kind](spec["asset_id"], tuple(spec["size"]), palette, seed)
            if image.mode != "RGBA" or image.size != tuple(spec["output_size"]):
                raise ValueError(f"Wrong generated PNG dimensions/mode: {spec['path']}")
            image.save(path)


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def check_assets(root, specs, seed):
    problems = []
    owned = {spec["path"] for spec in specs}
    existing = {path.relative_to(root).as_posix() for path in (root / "assets/hoghunter").rglob("*")
                if path.is_file() and path.suffix in (".png", ".ogg", ".mcmeta")}
    for path in sorted(existing - owned): problems.append(f"Undeclared procedural output: {path}")
    with tempfile.TemporaryDirectory(prefix="hoghunter-asset-check-") as directory:
        regenerated = Path(directory)
        render_assets(regenerated, specs, seed)
        for spec in specs:
            original = root / spec["path"]
            fresh = regenerated / spec["path"]
            if not original.is_file():
                problems.append(f"Missing: {spec['path']}")
                continue
            if digest(original) != spec["sha256"]:
                problems.append(f"Checksum differs: {spec['path']}")
            if original.suffix == ".png":
                with Image.open(original) as image, Image.open(fresh) as reference:
                    if image.mode != "RGBA" or image.size != tuple(spec["output_size"]):
                        problems.append(f"Wrong PNG dimensions/mode: {spec['path']}")
                    elif image.tobytes() != reference.tobytes():
                        problems.append(f"Pixels differ from generator: {spec['path']}")
            elif original.suffix == ".mcmeta":
                if json.loads(original.read_text(), object_pairs_hook=strict_object) != spec["data"]:
                    problems.append(f"Animation metadata differs: {spec['path']}")
            elif original.suffix == ".ogg":
                blob = original.read_bytes()
                if not blob.startswith(b"OggS") or b"\x01vorbis" not in blob[:256]:
                    problems.append(f"Not Vorbis OGG: {spec['path']}")
                if digest(original) != digest(fresh):
                    problems.append(f"Audio differs (use the recorded FFmpeg version): {spec['path']}")
    if problems: raise ValueError("\n".join(problems))
    print(f"PASS: {len(specs)} declared assets reproduce; output tree was not modified")


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--out", default="src/main/resources", help="Only this resource tree is written")
    ap.add_argument("--seed", type=int, default=20261003)
    ap.add_argument("--manifest", type=Path, default=Path(__file__).parent / "assets/manifest.json")
    ap.add_argument("--check", action="store_true", help="Compare temporary regeneration without changing any committed files")
    args = ap.parse_args()
    try:
        manifest = read_manifest(args.manifest)
        specs = manifest["outputs"]
        root = Path(args.out)
        if args.check:
            check_assets(root, specs, args.seed)
        else:
            render_assets(root, specs, args.seed)
            print(f"Generated {len(specs)} declared assets; run --check to validate release checksums")
    except (ValueError, RuntimeError, OSError, subprocess.CalledProcessError) as error:
        raise SystemExit(str(error)) from error


if __name__ == "__main__": main()
