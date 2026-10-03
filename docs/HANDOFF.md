# Hog Hunter — engineering handoff

**Date:** 2026-10-03
**Target:** Minecraft Java 1.21.1 / NeoForge 21.1.253 / Java 21
**Repo state:** first commit, self-contained, nothing carried over from the parent checkout.

This document is written for an engineer or agent picking the mod up cold. It states what is
actually working, how it was proven, and what is still open. Trust the "Verified" section — every
claim there came from a real command output, not from reading the code.

---

## 1. TL;DR

The mod is feature-complete against its own design document and **compiles, packages, generates
data, and passes 5/5 automated GameTests on a real NeoForge server**. It has custom models,
textures, and sounds for all seven entities.

The one significant gap is **client-side verification**: nothing has yet confirmed the renderer and
HUD actually draw in a real Minecraft client window. Everything below that line is unproven.

---

## 2. Verified — with the exact evidence

Run from the repo root with JDK 21 on the `PATH`.

### 2.1 Compile and package

```powershell
.\gradlew.bat build --no-daemon --console=plain
```

```text
> Task :jar
> Task :assemble
> Task :build
BUILD SUCCESSFUL
```

Produces `build/libs/hoghunter-0.1.0.jar`. The only warnings are two NeoForge deprecation notices
about `EventBusSubscriber(bus = ...)` in `client/HogClient.java` — cosmetic, not errors.

### 2.2 Data generation

```powershell
.\gradlew.bat runData --no-daemon --console=plain
```

```text
[minecraft/DataGenerator]: All providers took: 0 ms
BUILD SUCCESSFUL
```

### 2.3 Automated runtime proof — 5/5 GameTests pass

```powershell
.\gradlew.bat runGameTestServer --no-daemon --console=plain
```

```text
[minecraft/GameTestServer]: 5 tests are now running at position -12030903, -59, 7737995!
[minecraft/GameTestServer]: All 5 required tests passed :)
BUILD SUCCESSFUL in 34s
```

The five tests, defined in `src/main/java/com/hoghunter/test/HogHunterGameTests.java`:

| Test | Asserts |
|---|---|
| `hogRegistryAndAttributes` | All 7 entity types resolve, have attribute suppliers, and report the expected max health |
| `hogAiTicks` | A spawned `boar_hog` survives 20 ticks of live AI ticking |
| `hogWeaponDamageHurtsBoar` | `HogWeaponDamage.apply` reduces entity health |
| `hogBlocksPlaceAndRemove` | All 4 blocks place and break correctly |
| `hogBlockEntitiesInstantiate` | `hog_nest` creates the correct block entity type |

This gate is the one that matters. It runs a real dedicated server, loads the mod, and exercises
registries, attributes, AI, combat, and blocks.

### 2.4 Dedicated server smoke test

`runServer` has been run and reaches a clean world load:

```text
Done (17.095s)!
[co.ho.HogHunterMod/]: Hog Hunter server hook OK.
```

with no registry, attribute, recipe, loot-table, or worldgen errors. Earlier builds did log seven
`Entity hoghunter:... has no attributes` errors and invalid-loot-table crashes; both classes of
fault are fixed (see section 5).

---

## 3. Not yet verified — the open work

### 3.1 Client rendering and audio (highest priority)

`runClient` has **never been run to completion**. Consequently the following are unproven:

- `client/HogRenderer.java` and `client/model/HogModel.java` actually draw the hogs.
- `client/HogClientState.java` and the HUD overlay render.
- The 38 PNG textures bind correctly and are not missing or mis-mapped.
- The 4 OGG sounds are registered under the right keys and play.
- `HogClient`'s client setup is genuinely invoked at startup.

`HogClient.java` uses `@EventBusSubscriber(modid = "hoghunter", value = Dist.CLIENT, bus = Bus.MOD)`,
which currently draws two "marked for removal" deprecation warnings. While wiring the mod, confirm
the client setup actually fires — a client class that never registers its renderers fails silently
and looks identical to a working mod until you go looking for a hog in-game.

**How to close it:** run `runClient`, create a world, summon each entity with
`/summon hoghunter:boar_hog ~ ~ ~`, and screenshot each one. Verify the HUD appears when the
heart-rate or noise systems are active.

### 3.2 Gameplay tuning

Ability cooldowns, damage values, and spawn weights are implemented and plausible but have not been
playtested for balance. Treat the numbers in the entity classes as first-draft.

### 3.3 Known design substitutions

Two recipes deviate from the original design because the design was not runtime-valid:

- **Chestplate** — the design called for 10 ingredients, which cannot fit a 3x3 crafting grid. Now
  8 leather + 1 iron.
- **`salt` and `purified_tusk` tags** — no `salt` or `purified_tusk` item is registered, so these tags
  currently point at `minecraft:sugar` and `minecraft:bone`. Either register the real items or retarget
  the tags. **This is a loose end worth closing.**

---

## 4. Architecture notes for a new maintainer

### 4.1 Entry points

- `HogHunterMod.java` — mod entrypoint; wires registries, attachments, spawn placements, and the
  server hook that logs `Hog Hunter server hook OK.`
- `HogHunterBusEvents.java` — common/server event subscribers.
- `HogClient.java` — the client-side event subscriber.

### 4.2 Registration pattern

Everything is registered through `DeferredRegister` holders in `content/`:
`HogBlocks`, `HogItems`, `HogEntities`, `HogBlockEntities`, `HogSounds`, `HogCreativeTabs`.

Two registration details are easy to break and were both live bugs during development:

1. **Entity attributes must be explicitly registered.** `entity/HogEntityAttributes.java` is what
   makes `boar_hog` and friends loadable. Deleting it produces `Entity hoghunter:X has no attributes`
   at server start.
2. **Every block needs a `BlockItem`.** The four in `content/HogItems.java`
   (`CORRUPTED_ORE_BLOCK`, `DEPTH_GATE_BLOCK`, `HOG_NEST_BLOCK`, `SALT_LINE_BLOCK`) are what make the
   blocks placeable *and* what the loot tables resolve against. Note the constructor argument order
   is `BlockItem(Block, Properties)` — the reverse does not compile, which is exactly the mistake
   that was made once already.

### 4.3 Data layout

Recipes, tags, loot tables, worldgen, and the GameTest structure all live under
`src/main/resources/data/hoghunter/`.

**Loot tables:** `survives_explosion` is an *entry condition* in 1.21.1, not a pool function:

```json
{ "type": "minecraft:item", "name": "hoghunter:hog_nest",
  "conditions": [ { "condition": "minecraft:survives_explosion" } ] }
```

The `functions` form crashes the server. All four block tables are currently correct.

### 4.4 The GameTest structure template

`tools/make_gametest_structure.py` generates
`src/main/resources/data/hoghunter/structure/empty.nbt`, the template all five tests use.

Two facts that cost real time and will cost it again:

- **Minecraft release jars ship no gametest structure templates.** The `minecraft:empty` template
  that NeoForge examples reference does not exist in 1.21.1. The mod must supply one. This is why
  `build.gradle` enables `minecraft` in `neoforge.enabledGameTestNamespaces`, and why the tests are
  annotated `@PrefixGameTestTemplate(false)` with `templateNamespace = "hoghunter"`.
- **The template needs a solid floor.** Entities spawn inside the structure bounds; a truly empty
  template lets them fall out of the world before the assertions run. The generated template is a
  7x7x7 stone floor with air above.

Regenerate with:

```powershell
python tools/make_gametest_structure.py
```

If you resize the template, remember the tests place blocks at relative positions `(0..3, 0, 0)`.

---

## 5. Defects found and fixed

Kept as a record, because each one is a trap that is easy to fall back into.

| Defect | Symptom | Fix |
|---|---|---|
| Missing `BlockItem` import | `cannot find symbol: BlockItem` | Added the import |
| `BlockItem` args swapped | `Properties cannot be converted to Block` | `new BlockItem(block, props)` |
| No entity attribute registration | `Entity hoghunter:X has no attributes` (x7) | `entity/HogEntityAttributes.java` |
| Invalid loot-table functions | Server crash on load | `survives_explosion` moved into `conditions` |
| `template = "empty"` unprefixed | Server requested `minecraft:hoghuntergametests.empty` | `@PrefixGameTestTemplate(false)` |
| No vanilla gametest template | `IllegalStateException: Missing test structure: minecraft:empty` | Mod-supplied `structure/empty.nbt` |
| Empty template has no floor | Spawned entities fall out of the world | Stone floor in the generated NBT |
| `runData` misconfigured | `Trying to prepare unknown run: clientData` | `data` run needs `type = 'data'`, not `clientData()` |

The `runData` one is worth flagging: `clientData()` inside the `data` run block creates a *separate*
run type and silently breaks `prepareDataRun`. It must be `type = 'data'`.

---

## 6. Suggested next steps, in priority order

1. Run `runClient` and verify rendering, HUD, textures, and sound. Close section 3.1.
2. Fix the `salt` / `purified_tusk` tag targets — either register the items or repoint the tags (3.3).
3. Playtest and tune ability cooldowns and spawn weights (3.2).
4. Review `run/` crash-report history for anything not yet reproduced.
5. Only then consider a release: bump `mod_version`, confirm `neoforge.mods.toml`, and re-run the
   full verification chain from `design/04-verification.md`.

---

## 7. Reference material

- [`design/01-architecture.md`](../design/01-architecture.md) — intended architecture.
- [`design/02-gameplay.md`](../design/02-gameplay.md) — gameplay and ability specs.
- [`design/03-assets.md`](../design/03-assets.md) — asset requirements.
- [`design/04-verification.md`](../design/04-verification.md) — the full verification standard. The
  brief summary in `README.md` is derived from this; this is the authoritative version.

The design docs were written as plans before implementation. Where the implementation and a design
doc disagree, the design doc describes the original intent and section 3.3 records the deliberate
deviations.
