# Hog Hunter assets and resource pipeline

This document describes the current runtime resource contract. Static resource checks, generated
asset reproducibility, and actual client rendering/audio are separate gates; their results are
recorded in [`docs/HANDOFF.md`](../docs/HANDOFF.md).

## 1. Runtime resources

Client resources are under `src/main/resources/assets/hoghunter`:

| Directory / file | Used for |
|---|---|
| `blockstates/` | Registered block states, including open/closed depth gate |
| `models/block/` | Cube, gate frame, nest, altar, snare, and salt geometry |
| `models/item/` | Inventory/held models for registered items and block items |
| `textures/entity/hog/` | Seven hog skin strips and retained procedural source textures |
| `textures/models/armor/` | Hoghide material's humanoid armor layers |
| `textures/block/`, `textures/item/` | Procedural block and inventory artwork |
| `textures/gui/` | Blood-edge and vignette overlays |
| `textures/particle/`, `particles/` | Authored particle resources; see active-particle scope below |
| `sounds/` | Small shared library of original synthesized OGG samples |
| `sounds.json` | Registered sound events, samples, pitch/volume, and subtitles |
| `lang/en_us.json` | Display names, HUD labels, feedback, tooltips, and subtitles |

Server data lives separately under `src/main/resources/data/`. Minecraft 1.21.1 uses singular
`recipe`, `loot_table`, `structure`, `tags/item`, and `tags/block` directories. Actual tool tags
are in the `minecraft` namespace. The resource verifier rejects the silently ignored legacy
plural forms rather than treating a successful build as evidence that recipes loaded.

Item models may reference existing vanilla textures. A valid visible item model is required for
every registered item; a newly drawn PNG is not required when an existing texture is appropriate.
Java armor aliases do not create alternate registry ids. `hoghide_*` remains the four-piece set.

## 2. Entity geometry and skin sampling

The runtime geometry is Java source:

- `client/model/HogModel.java`: body/head/legs and species-specific geometry and animation.
- `client/model/SkinPatchConsumer.java`: sampling of the authored skin strip.
- `client/HogRenderer.java`: vanilla living-mob rendering, interpolation, orientation, scale,
  visibility, hurt/death transforms, and texture binding.
- `client/HogDetailsLayer.java`: material details such as tusks, plates, and fungal growths.
- `client/HogClient.java`: all seven renderer and model-layer registrations.

Entity textures are at `textures/entity/hog/<entity_id>.png`. The seven exact ids are `boar_hog`,
`spore_hog`, `hook_hog`, `screecher_hog`, `ironback_hog`, `mire_hog`, and `rootmother`.

The committed skins are **64 × 512 RGBA strips**, containing eight 64 × 64 illustration frames.
They are not conventional unfolded Minecraft cuboid UV atlases. The renderer explicitly selects
a frame and samples bounded opaque skin patches; tusks/plates/spores use separate detail layers.
This avoids sampling transparent areas or stretching all eight frames across one mob. Species
identity also comes from geometry, size, color, and synchronized ability poses.

Frame selection advances every two game ticks. The `.png.mcmeta` alongside an entity strip does
not animate a normal entity texture by itself. Maintain the explicit sampling logic when changing
texture dimensions or frame count. Every logical model UV rectangle must stay inside the
64 × 64 frame even though the physical PNG is taller.

`tools/assets/entity_models/*.json` contains earlier authoring/review sketches. These files are
not loaded by Minecraft, do not define the complete final roster, and do not belong under a
runtime `models/entity` directory. The Java model is authoritative.

## 3. Blocks, inventory, and armor

Simple blocks use valid classic 1.21.1 blockstate/model JSON. Noncube geometry supplies explicit
elements. Depth-gate variants must select different closed/open geometry, and the Java collision
shape must agree with the usable opening. Nests and snares are block entities with visible model
render shape; merely registering a block entity does not make its block render.

Ordinary inventory icons use `minecraft:item/generated` or `minecraft:item/handheld`. Block items
refer to the matching block/inventory model. Do not use the newer 1.21.4 item-definition format
for this target.

The field lantern has a small three-dimensional frame and a dark/lit core model override. Its
client item predicate follows synchronized toggle, fuel, and blackout state. This changes the
item's appearance; it is not a world-light emitter.

Hoghide and ironback armor materials are registered by `HogItems`. They reference the custom
`hoghunter:hoghide` armor layer instead of silently rendering as vanilla leather. The committed
textures use the 64 × 32 humanoid layout: layer 1 serves helmet/chest/boots, and layer 2 serves
leggings. Inventory armor icons are separate models/textures.

## 4. Sound and horror presentation

`content/HogSounds.java` owns sound registry ids. Each registered event resolves through
`sounds.json` to an existing extensionless `hoghunter:` sample path or a valid referenced event.
A sample named `hoghunter:entity/boar_hog/attack` resolves to
`assets/hoghunter/sounds/entity/boar_hog/attack.ogg`.

The current sound design names gun, lantern, trap, salt, altar, heartbeat, generic hog, species
windup/ability, and boss-phase events. Several events intentionally share the small synthesized
sample library with pitch and volume variation. Distinct event ids make tells independently
replaceable by a resource pack; they do not imply a unique recording for every species.

Samples are mono OGG Vorbis. Subtitles have English keys, and playback uses appropriate vanilla
sound categories. Horror intensity scales controlled ambient/ability/heartbeat effects. Ordinary
item-use feedback and vanilla damage sounds may use their own sound categories; do not describe
the horror intensity control as a global game-audio mute.

`HogHud` presents synchronized survival values and constrained vignette/blood/blackout overlays.
`HogClientAudio` schedules the heartbeat from the mirrored heart rate. Both are presentation-only.
Hiding the GUI or lowering visual intensity must not change server damage, fuel, or progression.
Client disconnect/world changes reset the mirror so a new world does not inherit stale state.

Current ability particles use registered vanilla particle types emitted by server-side behavior.
Retained custom particle JSON/PNGs are assets, not evidence of a custom particle provider or an
active custom particle system. A complete authored hallucination/death cinematic is outside the
implemented presentation scope.

## 5. Procedural authoring

`tools/gen_assets.py` contains the deterministic raster and audio authoring functions. Default
seed: **20261003**. Normal gameplay and a Java build use the committed files and do not require
Python imaging/audio tools.

Asset regeneration requires Python, Pillow, and an `ffmpeg` build with the `libvorbis` encoder.
`tools/assets/manifest.json` records the seed, provenance, toolchain, dimensions, and SHA-256
values for **52 owned outputs: 38 PNGs, ten metadata files, and four OGGs**. The recorded authoring
toolchain is Python 3.12.14, Pillow 12.3.0, and FFmpeg 6.1.1-3ubuntu5. Use the manifest as the exact
version record if the authoring environment changes.

The generator writes declared asset outputs under its selected resource root. Keep generation
scripts, temporary WAVs, manifests, review sketches, and authoring dependencies outside runtime
resources.

Common authoring commands:

```bash
python3 tools/gen_assets.py --manifest tools/assets/manifest.json --out src/main/resources --seed 20261003
python3 tools/gen_assets.py --manifest tools/assets/manifest.json --out src/main/resources --seed 20261003 --check
```

All shown flags have those defaults. Check mode compares temporary regenerated outputs with
committed outputs **without rewriting them**. It checks the declared hashes, PNG dimensions,
RGBA mode and pixels, metadata, OGG headers, and regenerated audio bytes. The audio encoder uses
bitexact flags and a path-independent seed. Different FFmpeg builds can nevertheless change the
encoded bytes; a mismatch requires review against the recorded toolchain rather than silently
updating the expected hashes. A check that overwrites its expected files cannot detect stale art.

Each raster asset has an independent seed namespace. Audio seeds must use a logical sound id,
not the absolute output path, so choosing a temporary output directory does not change the
waveform. JSON, block/item models, language entries, and sound-event definitions remain explicit
repository resources unless they are included in the generator's declared manifest.

## 6. Verification

```bash
python3 tools/verify_resources.py
python3 tools/make_gametest_structure.py --check
```

The standard-library verifier discovers registration ids and checks relevant models, blockstates,
loot, names, local references, recipe shapes/results, singular data paths, sound mappings and
subtitles, PNG integrity/frame ranges, OGG signatures, and independently decoded GameTest NBT.
After a successful final build, also check the exact JAR:

```bash
python3 tools/verify_resources.py --jar build/libs/hoghunter-0.1.0.jar
```

A static pass establishes structural consistency in the checked scope. It does not prove that
vanilla codecs accept every resource, that an image looks good on a model, that a texture animates,
that a HUD fits at all GUI scales, or that the sound is audible and useful. Those require the
real-client scenarios in [`docs/RELEASE_CHECKS.md`](../docs/RELEASE_CHECKS.md).

## 7. Asset provenance and scope

The repository includes the source of its procedural raster/sound generation and uses the MIT
license. Vanilla texture/sound references resolve from the user's Minecraft installation; the
repository does not need to redistribute Minecraft's files. No claim is made that retained
prototype artwork, a JSON review model, or an unused particle definition is an active feature.
