# Hog Hunter

A Minecraft **Java Edition 1.21.1** survival-horror mod built on **NeoForge 21.1.253**. You are a
contract hunter working abandoned deep-slate mines. Seven hog species hunt you by sound and smell;
your own heart rate and noise level are the central survival stats.

> **Independent handoff repo.** This repository contains the mod and nothing else. It was extracted
> from a larger unrelated checkout and is self-contained. See [`docs/HANDOFF.md`](docs/HANDOFF.md)
> for the state of play, what is verified, and what is still open.

---

## Quick start

Requires **JDK 21** on the `PATH` (`java -version` must print 21).

```powershell
git clone https://github.com/H-H-E/hoghunter.git
cd hoghunter

.\gradlew.bat build               # compile + package  -> build/libs/hoghunter-0.1.0.jar
.\gradlew.bat runData             # data generation smoke check
.\gradlew.bat runGameTestServer   # automated runtime proof (5 tests)
.\gradlew.bat runServer           # dedicated server smoke test
.\gradlew.bat runClient           # real client, for rendering and audio
```

Drop `build/libs/hoghunter-0.1.0.jar` into a NeoForge 1.21.1 `mods/` folder to play.

---

## Toolchain

| Component | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.253 |
| Gradle | 8.12 (via wrapper) |
| Java | 21 |
| Mod id | `hoghunter` |
| Package root | `com.hoghunter` |
| Loader | `javafml` 4.0 |

All of this lives in [`gradle.properties`](gradle.properties). Change the version there, not in
`build.gradle`.

---

## Mod contents

### Entities (7)

| Entity | Ability |
|---|---|
| `boar_hog` | Line-of-sight charge, bonus damage + knockback (8s cooldown) |
| `spore_hog` | Spore cloud: +12 BPM and Blindness (14s cooldown) |
| `hook_hog` | Grapple pull up to 12 blocks, adds noise (6s cooldown) |
| `screecher_hog` | Pulse: +25 BPM, sprint lock, may summon boars (18s cooldown) |
| `ironback_hog` | Frontally armored, turns slowly, high knockback resistance |
| `mire_hog` | Invisibility + water speed, rear teleport (16s cooldown) |
| `rootmother` | Reinforcements, 120 degree sweep, blackout phase at 50% health |

Five of these spawn naturally (`screecher_hog` and `rootmother` do not). All seven are registered
with attributes via `entity/HogEntityAttributes.java` and have custom models, textures, and sounds.

### Items

Consumables, four armor pieces (hoghide), tools, and five interactive items:
`mine_harpoon`, `salt_shaker`, `oil_can`, `field_lantern`, `baited_snare`.

### Blocks

`corrupted_ore`, `depth_gate`, `hog_nest` (block entity), `salt_line`.

### Systems

- **Player state** — heart rate, noise, and lantern oil, persisted via a NeoForge data attachment
  (`core/HogAttachments.java`, `core/HogHunterPlayerData.java`).
- **Networking** — `net/HogNetworking.java`, `net/HogPayloads.java`.
- **Worldgen** — ore, biome modifiers, and a rootmother arena (`worldgen/`).
- **Config** — `core/HogHunterConfig.java`.
- **Client** — HUD overlays and the entity renderer (`client/`).

---

## Repository layout

```text
hoghunter/
  build.gradle              Gradle + NeoForge ModDevGradle + run configurations
  gradle.properties         version and toolchain single source of truth
  src/main/java/com/hoghunter/
    HogHunterMod.java       mod entrypoint
    HogHunterBusEvents.java common/server event wiring
    block/  client/  content/  core/  entity/  item/  net/  test/  worldgen/
  src/main/resources/
    assets/hoghunter/       models, textures, sounds, lang, blockstates, particles
    data/hoghunter/         recipes, tags, loot tables, worldgen, structure
    META-INF/               neoforge.mods.toml
  tools/                    helper scripts (incl. gametest structure generator)
  cmds.txt                  in-game commands for manual testing (type into the chat box)
  design/                   original design documents (architecture, gameplay, assets, verification)
  docs/HANDOFF.md           state of play for the next engineer
```

### Manual smoke-testing the entities

Start a world in creative mode, then paste the lines from [`cmds.txt`](cmds.txt) into the chat box
one at a time. It summons all seven hogs in a row so you can see every model in one place:

```text
/summon hoghunter:boar_hog 0 -60 0
/summon hoghunter:spore_hog 4 -60 0
...
/summon hoghunter:rootmother 26 -60 0
```

---

## Verification

The bar for "done" is deliberately high, and it is documented in
[`design/04-verification.md`](design/04-verification.md). In short:

| Gate | Command | Proves |
|---|---|---|
| Compile | `compileJava` | Java and imports are valid |
| Package | `build` | The JAR is produced |
| Datagen | `runData` | Data generation loads cleanly |
| GameTests | `runGameTestServer` | Registries, attributes, AI, damage, blocks work in a real server |
| Server | `runServer` | The packaged mod loads and responds without errors |
| Client | `runClient` | Rendering and audio work (**not yet verified — see handoff**) |

A green `build` alone is *not* evidence that the mod works. The GameTest and server gates are the
ones that catch real defects.

---

## License

MIT. All textures, models, and sounds in this repository were authored for this mod.
