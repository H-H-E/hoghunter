# Hog Hunter asset, model, sound, and data pipeline

Target: Minecraft Java 1.21.1, NeoForge 21.1.253, Java 21, mod id `hoghunter`, base package `com.hoghunter`.

This document defines authored source assets and the generated resource tree. It is a design specification only. The first implementation must keep source generators outside `src/main/resources`; only their deterministic PNG, OGG, JSON, and language outputs belong in the mod jar.

## 1. Resource tree

The complete planned tree under `src/main/resources` is:

```text
src/main/resources/
├── META-INF/
│   └── neoforge.mods.toml                         # loader metadata; owned by project setup
├── assets/hoghunter/
│   ├── blockstates/
│   │   ├── bloodstone_ore.json
│   │   ├── cursed_bone_block.json
│   │   ├── hog_flesh_block.json
│   │   └── ritual_altar.json
│   ├── lang/
│   │   └── en_us.json
│   ├── models/
│   │   ├── block/
│   │   │   ├── bloodstone_ore.json
│   │   │   ├── cursed_bone_block.json
│   │   │   ├── hog_flesh_block.json
│   │   │   ├── ritual_altar.json
│   │   │   └── ritual_altar_inventory.json
│   │   ├── item/
│   │   │   ├── bloodstone_chunk.json
│   │   │   ├── cursed_bone.json
│   │   │   ├── hog_cleaver.json
│   │   │   ├── hog_flesh.json
│   │   │   ├── hoghide_boots.json
│   │   │   ├── hoghide_chestplate.json
│   │   │   ├── hoghide_helmet.json
│   │   │   ├── hoghide_leggings.json
│   │   │   ├── ritual_altar.json
│   │   │   └── salt_cartridge.json
│   │   └── armor/
│   │       └── hoghide_armor.json                 # item display model only; armor layer is texture data
│   ├── particles/
│   │   ├── blood_mist.json
│   │   ├── cursed_ash.json
│   │   └── hog_spark.json
│   ├── sounds.json
│   └── textures/
│       ├── models/armor/
│       │   ├── hoghide_layer_1.png
│       │   └── hoghide_layer_2.png
│       ├── block/
│       │   ├── bloodstone_ore.png
│       │   ├── cursed_bone_block.png
│       │   ├── hog_flesh_block.png
│       │   ├── ritual_altar_side.png
│       │   ├── ritual_altar_top.png
│       │   └── ritual_altar_bottom.png
│       ├── entity/hog/
│       │   ├── boar_hog.png
│       │   ├── tusked_hog.png
│       │   ├── mine_hog.png
│       │   └── hog_blood_overlay.png
│       ├── gui/
│       │   ├── hog_hunter_vignette.png
│       │   ├── hog_hunter_blood_edges.png
│       │   ├── ritual_altar_panel.png
│       │   └── fear_meter.png
│       ├── item/
│       │   ├── bloodstone_chunk.png
│       │   ├── cursed_bone.png
│       │   ├── hog_cleaver.png
│       │   ├── hog_flesh.png
│       │   └── salt_cartridge.png
│       └── particle/
│           ├── blood_mist.png
│           ├── cursed_ash.png
│           └── hog_spark.png
└── data/hoghunter/
    ├── loot_table/
    │   ├── blocks/bloodstone_ore.json
    │   ├── blocks/cursed_bone_block.json
    │   ├── blocks/hog_flesh_block.json
    │   ├── blocks/ritual_altar.json
    │   └── entities/boar_hog.json
    ├── recipe/
    │   ├── bloodstone_chunk_from_smelting.json
    │   ├── bloodstone_chunk_from_blasting.json
    │   ├── hog_cleaver.json
    │   ├── ritual_altar.json
    │   ├── salt_cartridge.json
    │   └── hoghide_armor_upgrade.json
    ├── tags/
    │   ├── blocks/mineable/pickaxe.json
    │   ├── blocks/needs_iron_tool.json
    │   ├── items/hog_food.json
    │   ├── items/hoghide_armor.json
    │   └── items/ritual_components.json
    ├── dimension/hog_depths.json                 # only if a separate dimension is implemented
    └── structure/
        └── collapsed_hog_shrine.nbt              # binary structure template, authored/generated separately
```

Vanilla and NeoForge resolve these namespaces differently: client resources are `assets/hoghunter/...`; server data are `data/hoghunter/...`. A path such as `hoghunter:item/hog_cleaver` resolves to `assets/hoghunter/models/item/hog_cleaver.json`, while `hoghunter:entities/boar_hog` resolves to `data/hoghunter/loot_table/entities/boar_hog.json`.

Do not create empty placeholder files. Every referenced model, texture, particle, sound, loot table, recipe, tag, and translation must be either present or the corresponding registry/reference must be removed before packaging.

## 2. Procedural, license-safe authoring

No copyrighted art is shipped. All custom raster art is generated from deterministic code and a small palette/shape specification owned by Hog Hunter. The source generator is planned at `tools/assets/generate_assets.py` and must have this exact interface:

```text
python tools/assets/generate_assets.py \
  --manifest tools/assets/manifest.json \
  --out src/main/resources \
  --seed 20261003 \
  --check
```

Required behavior:

* `--manifest` names every output, dimensions, palette, seed namespace, and generator function. The generator rejects duplicate paths and unexpected output paths.
* `--out` is the only output root. It creates/updates only declared files; it must not touch Java, Gradle, or source files.
* `--seed` is an integer mixed with each asset id, so one asset can be regenerated identically without depending on execution order.
* `--check` regenerates into a temporary memory/directory comparison and exits nonzero if any committed PNG differs, has a wrong size/mode, or contains an undeclared output.
* PNG output is lossless RGBA (`PIL.Image` mode `RGBA`), with no embedded authoring metadata. Pixel art uses nearest-neighbor scaling only.

The generator must expose callable functions with stable names: `generate_entity_skin(asset_id, size, palette, seed)`, `generate_block_texture(asset_id, size, palette, seed)`, `generate_item_icon(asset_id, size, palette, seed)`, `generate_gui_overlay(asset_id, size, palette, seed)`, `generate_armor_layer(asset_id, size, palette, seed)`, and `generate_particle_sheet(asset_id, size, palette, seed)`. It must use `random.Random(sha256(f"{seed}:{asset_id}".encode()).digest())`, never global random state. Geometry may be described in Python data, but the emitted PNG must be reproducible.

Sound sources must also be original or CC0/public-domain with a recorded source URL/license in a non-runtime manifest. Synthesis is preferred: generate one-shot waveforms/noise in a script, normalize them, and encode OGG Vorbis. Never copy Minecraft, Mojang, or another game's sounds.

## 3. Texture inventory and dimensions

The required custom textures are:

| Output group | Files | Required size and rules |
|---|---|---|
| Entity skins | `textures/entity/hog/boar_hog.png`, `tusked_hog.png`, `mine_hog.png` | 64x64 RGBA, matching the `HogModel` UV layout; transparent unused pixels are allowed. |
| Entity overlay | `textures/entity/hog/hog_blood_overlay.png` | 64x64 RGBA; transparent except for animated blood marks on the same UV layout. |
| Block faces | six files under `textures/block/` | 16x16 RGBA; opaque unless a deliberately translucent block is registered and rendered accordingly. |
| Item icons | five files under `textures/item/` | 16x16 RGBA; transparent background and centered silhouette. |
| Armor layers | `textures/models/armor/hoghide_layer_1.png`, `hoghide_layer_2.png` | 64x32 RGBA, vanilla humanoid armor atlas layout; layer 1 is helmet/chest/legs, layer 2 is boots. |
| GUI | four files under `textures/gui/` | `hog_hunter_vignette.png` 256x256, `hog_hunter_blood_edges.png` 256x256, `ritual_altar_panel.png` 176x166, `fear_meter.png` 16x64; RGBA with transparent unused areas. |
| Particles | three files under `textures/particle/` | 16x16 RGBA. If a particle is animated, use a vertical strip and declare frames in its `.mcmeta`. |

The armor trim is not a second custom armor atlas. If the armor supports vanilla trims, use the vanilla trim system with a trim material and supply `assets/hoghunter/textures/trims/models/armor/hoghide.png` only if a custom trim material is actually registered. The regular armor layers are exactly `assets/hoghunter/textures/models/armor/hoghide_layer_1.png` and `hoghide_layer_2.png`. The four armor item icons are exactly `assets/hoghunter/textures/item/hoghide_helmet.png`, `hoghide_chestplate.png`, `hoghide_leggings.png`, and `hoghide_boots.png`.

## 4. Animated textures and `.mcmeta`

Minecraft reads metadata beside a texture: `boar_hog.png.mcmeta` is the exact companion filename. Entity texture animation is a vertical frame strip. For the 64x64 logical hog skin, an 8-frame strip is 64x512; the entity model continues to use `textureWidth = 64` and `textureHeight = 64` for UVs while the renderer samples the animation texture as a sprite. The generator must emit:

```json
{
  "animation": {
    "frametime": 2,
    "interpolate": true,
    "frames": [0, 1, 2, 3, 4, 5, 6, 7]
  }
}
```

This means 2 ticks per frame, 16 ticks per loop, with interpolation between frames. Use `frametime: 3` and `interpolate: false` for a deliberately choppy corruption pulse. Do not put a frame list on a static 64x64 skin. The texture atlas must be a valid integer number of square 64x64 frames; the generator must verify that.

Block animations use the same adjacent `.mcmeta` convention. For a 4-frame 16x16 `hog_flesh_block.png` strip (16x64), use:

```json
{
  "animation": {
    "frametime": 4,
    "interpolate": false,
    "frames": [0, 1, 2, 3]
  }
}
```

Use block animation sparingly because animated opaque textures are sampled constantly. Static block textures have no `.mcmeta`. GUI overlays should be static unless the screen deliberately advances frames in code; a texture `.mcmeta` alone does not make an arbitrary GUI widget animate.

## 5. Entity model contract

NeoForge 21.1 does not make an arbitrary `models/entity/*.json` file into a living entity renderer. Vanilla entity rendering uses a registered `EntityModelLayer`, a Java `LayerDefinition`, a `ModelPart`, and an `EntityRenderer`. Therefore `assets/hoghunter/models/entity/*.json` is not part of the runtime tree. The authoritative runtime model must be `com.hoghunter.client.model.HogModel`, with a layer registered on the mod event bus and a renderer that binds `textures/entity/hog/boar_hog.png`.

The model authoring format should still be JSON-like and generated for review at `tools/assets/entity_models/boar_hog.json`; it is an input to a generator or a hand translation into `HogModel.createBodyLayer()`, not a vanilla resource consumed directly. The complete worked quadruped specification is:

```json
{
  "id": "boar_hog",
  "texture": "hoghunter:textures/entity/hog/boar_hog.png",
  "texture_size": [64, 64],
  "bones": [
    {"name":"body", "pivot":[0,12,0], "box":[-6,-5,-10,12,10,20], "uv":[0,0]},
    {"name":"neck", "parent":"body", "pivot":[0,10,-9], "box":[-5,-5,-5,10,10,8], "uv":[0,30]},
    {"name":"head", "parent":"neck", "pivot":[0,7,-13], "box":[-5,-5,-6,10,10,10], "uv":[0,48]},
    {"name":"snout", "parent":"head", "pivot":[0,8,-19], "box":[-4,-2,-3,8,5,5], "uv":[40,48]},
    {"name":"leg_front_left", "parent":"body", "pivot":[4,14,-6], "box":[-2,0,-2,4,10,4], "uv":[36,0]},
    {"name":"leg_front_right", "parent":"body", "pivot":[-4,14,-6], "box":[-2,0,-2,4,10,4], "uv":[36,14]},
    {"name":"leg_back_left", "parent":"body", "pivot":[4,14,6], "box":[-2,0,-2,4,10,4], "uv":[52,0]},
    {"name":"leg_back_right", "parent":"body", "pivot":[-4,14,6], "box":[-2,0,-2,4,10,4], "uv":[52,14]},
    {"name":"tusk_left", "parent":"head", "pivot":[4,10,-17], "rotation":[0,0,-20], "box":[0,0,0,2,5,2], "uv":[40,58]},
    {"name":"tusk_right", "parent":"head", "pivot":[-4,10,-17], "rotation":[0,0,20], "box":[-2,0,0,2,5,2], "uv":[48,58]}
  ]
}
```

Coordinates are model-space units in the same convention as `PartPose.offset(...)`; a child rotation occurs around its declared pivot. In `HogModel.createBodyLayer()`, create `MeshDefinition root`, add `PartDefinition` children with `CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, dx, dy, dz)`, and return `LayerDefinition.create(mesh, 64, 64)`. The renderer must use `ModelLayerLocation(new ResourceLocation("hoghunter", "boar_hog"), "main")`. Verify that every UV rectangle stays within 64x64 and that the four leg parts are children of the body so walk animation changes their `xRot` around the correct pivots.

## 6. Blockstates and block models

Each simple cube block gets a blockstate variant pointing to a block model. Example for `bloodstone_ore`:

```json
{
  "variants": {
    "": {"model": "hoghunter:block/bloodstone_ore"}
  }
}
```

The model at `assets/hoghunter/models/block/bloodstone_ore.json` should use `"parent":"minecraft:block/cube_all"` and `"textures":{"all":"hoghunter:block/bloodstone_ore"}`. A six-sided altar uses `minecraft:block/cube_bottom_top` or explicit `elements` with `top`, `bottom`, and `side` texture keys. For a horizontal-facing altar, define variants such as `"facing=north"`, `"facing=east"`, `"facing=south"`, and `"facing=west"`, with `y` rotations 0, 90, 180, and 270. The block model is not the item model; provide `models/block/ritual_altar_inventory.json` and point `models/item/ritual_altar.json` to it with `"parent":"hoghunter:block/ritual_altar_inventory"`.

For the entity client setup, `HogModel.createBodyLayer()` returns the `LayerDefinition`; subscribe `EntityRenderersEvent.RegisterLayerDefinitions` and call `event.registerLayerDefinition(HogModel.LAYER, HogModel::createBodyLayer)`. In `EntityRenderersEvent.RegisterRenderers`, call `event.registerEntityRenderer(HogEntities.BOAR_HOG.get(), HogRenderer::new)`. `HogRenderer` binds `new ResourceLocation("hoghunter", "textures/entity/hog/boar_hog.png")`; the layer location is `new ModelLayerLocation(new ResourceLocation("hoghunter", "boar_hog"), "main")`. These calls are client-only event subscribers and must not be placed in common registration code.

## 7. Items, recipes, tags, and loot

In 1.21.1, ordinary item models use the classic model JSON format. A generated icon model is:

```json
{
  "parent": "minecraft:item/generated",
  "textures": {"layer0": "hoghunter:item/bloodstone_chunk"}
}
```

Tools use `minecraft:item/handheld`. Block items either use `"parent":"hoghunter:block/<name>"` or a dedicated inventory model. The four armor icons use `minecraft:item/generated` with layer0 set to the matching `hoghunter:item/hoghide_*` texture. These are small and deterministic enough to hand-write or generate; the manifest should generate them from item declarations so references cannot drift. Do not use the newer 1.21.4+ item-model component format for this 1.21.1 target.

Shaped recipe example at `data/hoghunter/recipe/ritual_altar.json`:

```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["CBC", "BDB", "EEE"],
  "key": {
    "B": {"item":"hoghunter:cursed_bone"},
    "C": {"item":"hoghunter:bloodstone_chunk"},
    "D": {"item":"minecraft:crying_obsidian"},
    "E": {"item":"minecraft:stone"}
  },
  "result": {"id":"hoghunter:ritual_altar", "count":1}
}
```

Smelting and blasting use the 1.21.1 result object with an item id:

```json
{
  "type":"minecraft:smelting",
  "category":"misc",
  "cookingtime":200,
  "experience":0.7,
  "ingredient":{"item":"hoghunter:bloodstone_ore"},
  "result":{"id":"hoghunter:bloodstone_chunk"}
}
```

Smithing uses the smithing transform shape, for example `data/hoghunter/recipe/hoghide_armor_upgrade.json`:

```json
{
  "type":"minecraft:smithing_transform",
  "template":{"item":"minecraft:netherite_upgrade_smithing_template"},
  "base":{"item":"minecraft:iron_chestplate"},
  "addition":{"item":"hoghunter:cursed_bone"},
  "result":{"id":"hoghunter:hoghide_chestplate"}
}
```

Use item tags when a recipe accepts a family: `{"values":["hoghunter:cursed_bone","minecraft:bone"]}` at `data/hoghunter/tags/items/ritual_components.json`. Loot tables use the 1.21.1 `type`, `pools`, `rolls`, and `entries` format. A block loot table must drop the registered block through `minecraft:item` and can add `minecraft:survives_explosion`; an entity table uses `type":"minecraft:entity"` and an entity entry with `name":"hoghunter:cursed_bone"`. Every registered block that should drop something needs a table at `data/hoghunter/loot_table/blocks/<registry_name>.json`; every custom hog entity needs `entities/<registry_name>.json`.

Dimension and structure data are server data, never assets: `data/hoghunter/dimension/hog_depths.json` describes the dimension type/generator only if the mod registers and uses it, and `data/hoghunter/structure/collapsed_hog_shrine.nbt` is a binary structure template referenced by a configured structure feature. Do not claim either file is active until the corresponding registry and world-generation code exist.

## 8. Particles

Each particle definition is under `assets/hoghunter/particles/<name>.json`. A simple particle definition is:

```json
{
  "textures": ["hoghunter:blood_mist"]
}
```

The texture reference omits `textures/particle/` and `.png`; the file is `assets/hoghunter/textures/particle/blood_mist.png`. For an animated sheet, list the frame texture indices in the particle JSON and place the timing in `blood_mist.png.mcmeta` only when the intended particle provider supports that animation. Keep particle PNGs small and alpha-tested; fully transparent pixels still occupy atlas space.

## 9. Sounds

`assets/hoghunter/sounds.json` maps event ids to OGG files. Example:

```json
{
  "entity.boar_hog.ambient": {
    "sounds": [
      {"name":"hoghunter:entity/boar_hog/ambient_01", "volume":0.8, "pitch":0.95},
      {"name":"hoghunter:entity/boar_hog/ambient_02", "volume":0.8, "pitch":1.05}
    ],
    "subtitle":"subtitles.hoghunter.entity.boar_hog.ambient"
  },
  "entity.boar_hog.attack": {
    "sounds": [{"name":"hoghunter:entity/boar_hog/attack", "volume":1.0}],
    "subtitle":"subtitles.hoghunter.entity.boar_hog.attack"
  },
  "block.ritual_altar.activate": {
    "sounds": [{"name":"hoghunter:block/ritual_altar/activate", "volume":0.7}],
    "subtitle":"subtitles.hoghunter.block.ritual_altar.activate"
  }
}
```

The corresponding files are `assets/hoghunter/sounds/entity/boar_hog/ambient_01.ogg`, `ambient_02.ogg`, `attack.ogg`, and `assets/hoghunter/sounds/block/ritual_altar/activate.ogg`. The name in `sounds.json` is extensionless and uses forward slashes; the actual file is lowercase `.ogg`. Encode original or CC0 material as Vorbis OGG, preferably mono 44.1 or 48 kHz for one-shots. Keep source WAVs and license/provenance records under `tools/assets/audio_sources/`, outside the runtime resource tree. Sound event ids must be registered/played with `SoundEvent.createVariableRangeEvent(new ResourceLocation("hoghunter", "entity.boar_hog.ambient"))` and the resulting holder; the JSON event key and registry path must match exactly.

## 10. Language keys

`assets/hoghunter/lang/en_us.json` must contain a key for every visible custom registry object and every subtitle:

```json
{
  "block.hoghunter.bloodstone_ore": "Bloodstone Ore",
  "block.hoghunter.cursed_bone_block": "Cursed Bone Block",
  "block.hoghunter.hog_flesh_block": "Hog Flesh Block",
  "block.hoghunter.ritual_altar": "Ritual Altar",
  "item.hoghunter.bloodstone_chunk": "Bloodstone Chunk",
  "item.hoghunter.cursed_bone": "Cursed Bone",
  "item.hoghunter.hog_cleaver": "Hog Cleaver",
  "item.hoghunter.hog_flesh": "Hog Flesh",
  "item.hoghunter.hoghide_helmet": "Hoghide Helmet",
  "item.hoghunter.hoghide_chestplate": "Hoghide Chestplate",
  "item.hoghunter.hoghide_leggings": "Hoghide Leggings",
  "item.hoghunter.hoghide_boots": "Hoghide Boots",
  "item.hoghunter.salt_cartridge": "Salt Cartridge",
  "entity.hoghunter.boar_hog": "Boar Hog",
  "subtitles.hoghunter.entity.boar_hog.ambient": "A corrupted hog grunts",
  "subtitles.hoghunter.entity.boar_hog.attack": "A corrupted hog attacks",
  "subtitles.hoghunter.block.ritual_altar.activate": "The ritual altar awakens",
  "gui.hoghunter.fear": "Fear"
}
```

Translation lookup does not normally crash the client when a key is missing; it displays the raw key. The release gate must still fail on missing keys so players never see `block.hoghunter...` or `subtitles...` in game. Generate this file from the same registry manifest or compare it against all registered blocks, items, entities, sounds, menus, and GUI keys. JSON keys must be unique, values must be nonempty, and the file must be valid UTF-8 JSON.

## 11. Verifier checklist

The asset verifier should run from the project root after generation:

```text
python tools/assets/generate_assets.py --manifest tools/assets/manifest.json --out src/main/resources --seed 20261003 --check
```

Then perform these checks:

1. Parse every `assets/hoghunter/**/*.json` and `data/hoghunter/**/*.json` with a strict JSON parser; reject duplicate keys, invalid UTF-8, and unexpected files.
2. Enumerate every `hoghunter:` reference in blockstates, models, particles, loot tables, recipes, tags, and `sounds.json`; resolve it to the exact expected path and fail on missing targets.
3. For every PNG, verify RGBA mode, the manifest dimensions, lowercase filename, and that any `.mcmeta` animation height is an integer multiple of the declared frame width/height.
4. For each item model, resolve its `parent` recursively through `minecraft:` and `hoghunter:` model namespaces; verify every `layer0`, `all`, `side`, `top`, `bottom`, and element-face texture exists.
5. For each blockstate, verify every variant model exists and every referenced block has a block loot table unless explicitly marked `no_drop`.
6. For each particle JSON, verify every listed texture exists under `textures/particle/`.
7. For `sounds.json`, verify every extensionless sound name maps to one or more `.ogg` files, each OGG has a readable Vorbis header, and every subtitle key exists in `en_us.json`.
8. Compare generated `en_us.json` keys against all custom block/item/entity/sound/menu/GUI keys and fail on missing, blank, or orphaned keys.
9. Verify the four hog leg bones, body, head, tusks, texture size, and UV bounds in the procedural entity-model input; reject pivots outside the documented model envelope.
10. Build the jar and inspect it as a zip: confirm no source WAVs, generator files, copyrighted inputs, or files outside the declared asset/data manifest are packaged.
11. Launch a NeoForge client with the mod and inspect `logs/latest.log` for `Unable to load model`, `Missing textures in model`, `File ... does not exist`, `Unable to play unknown soundEvent`, and missing-language warnings. A clean static check is necessary but not sufficient; the client log and a test world must prove the renderer, particles, sounds, drops, recipes, and GUI resolve at runtime.

The final evidence should record generator seed, manifest hash, resource-tree file list, verifier result, jar contents, and the client log scan. This keeps deterministic local asset proof separate from actual in-game rendering proof.
