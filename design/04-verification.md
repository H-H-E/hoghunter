# Hog Hunter verification contract

Target: Minecraft Java **1.21.1**, NeoForge **21.1.253**, **JDK 21**, Gradle **8.12**.
The current evidence and environmental limits belong in [`docs/HANDOFF.md`](../docs/HANDOFF.md).
Commands in this document are procedures, not claims that they have already passed.

## What each gate proves

| Gate | Positive evidence | Does not prove |
|---|---|---|
| Resource verifier | Committed data/asset references and checked formats are internally consistent | Vanilla codec acceptance, rendering, gameplay, sound quality |
| Template check | The committed GameTest NBT matches its deterministic generator | Tests executed |
| Compile | Final Java source compiles against the target APIs | Registry loading, worldgen, networking, visual correctness |
| Build | Final artifact was produced and configured checks passed | A client or server can load the artifact |
| Datagen | The mod's data-run bootstrap completes | Hand-authored recipes/assets were loaded or validated |
| GameTests | The named required server assertions passed in Minecraft | Human controls, screen presentation, real audio, balance |
| Development server | A dedicated server loads the development classpath and responds | The packaged JAR was the artifact loaded |
| Packaged server | A separate server loads the exact packaged JAR and responds | Client resource loading |
| Client | A real Minecraft client loads and the observed scenarios work | Unobserved scenarios or all hardware configurations |

Do not carry the old handoff's five passing tests forward as evidence for changed source. The
original tests also checked only entity creation, twenty ticks of survival, a direct damage call,
block placement/removal, and one block entity. They did not establish the survival loop.

## 1. Reproducible local checks

Use a clean checkout, JDK 21 on `PATH`, and the committed wrapper. On Linux/macOS:

```bash
java -version
./gradlew --version
python3 tools/verify_resources.py
python3 tools/make_gametest_structure.py --check
./gradlew compileJava --no-daemon --console=plain
./gradlew build --no-daemon --console=plain
./gradlew runData --no-daemon --console=plain
./gradlew runGameTestServer --no-daemon --console=plain
```

On Windows PowerShell:

```powershell
java -version
.\gradlew.bat --version
python tools/verify_resources.py
python tools/make_gametest_structure.py --check
.\gradlew.bat compileJava --no-daemon --console=plain
.\gradlew.bat build --no-daemon --console=plain
.\gradlew.bat runData --no-daemon --console=plain
.\gradlew.bat runGameTestServer --no-daemon --console=plain
```

Stop and investigate a nonzero result before describing a later gate as passing. Record the exact
command, UTC time, exit code, and complete output. A previous JAR in `build/libs` is not a successful
new build. First-time builds require the Gradle, NeoForge, Maven, and Mojang dependencies.

The resource verifier is Python standard-library code. Asset regeneration has additional authoring
dependencies; see [`03-assets.md`](03-assets.md). Those dependencies are not needed to load the
committed PNG/OGG assets in Minecraft.

This repository currently registers **zero datagen providers**. `runData` proves the data-run
bootstrap only; the resource verifier and loaded-recipe/tag/worldgen GameTests check the
hand-authored data. A zero-millisecond provider run is not a recipe validation result.

## 2. Static resources and GameTest structure

`tools/verify_resources.py` checks strict JSON parsing, duplicate keys, discovered registry ids,
local asset/data references, English names, sound definitions/files/subtitles, legacy data
directories, crafting shapes/results, PNG chunk integrity/frame ranges, OGG signatures, and the
GameTest NBT. It is a structural check with an explicit scope; it is not a Minecraft codec runner
or an image-quality test.

The template path is exactly:

```text
src/main/resources/data/hoghunter/structure/empty.nbt
```

`tools/make_gametest_structure.py` produces a **33 × 8 × 33** template with a complete stone floor
and explicit air. The wide template separates tests with abilities reaching up to sixteen blocks.
The independent decoder in the resource verifier checks the palette, bounds, unique positions,
floor, and complete volume. `--check` compares bytes without rewriting the committed file.

The initial template writer incorrectly nested anonymous compounds inside list elements. Both
the writer and its output must be checked; accepting a parseable NBT root alone misses this defect.

The structure block also applies a vertical offset. Template-local floor Y=0 appears at
GameTest-helper-relative Y=1; standing fixtures use helper-relative **Y=2**. Do not put short
entities at helper Y=1: their eyes/cloud origin may be inside the floor while taller entities
appear to work. `HogTestSupport` checks collision clearance to catch this fixture error early.

## 3. Runtime GameTests

Test sources live in `src/main/java/com/hoghunter/test/`. They are discovered through
`@GameTestHolder("hoghunter")`, use `@PrefixGameTestTemplate(false)`, and explicitly name
`templateNamespace = "hoghunter", template = "empty"`. Tests must not depend on a nonexistent
vanilla empty template.

The entity registry roster is:

| Registry id | Entity class |
|---|---|
| `hoghunter:boar_hog` | `BoarHogEntity` |
| `hoghunter:spore_hog` | `SporeHogEntity` |
| `hoghunter:hook_hog` | `HookHogEntity` |
| `hoghunter:screecher_hog` | `ScreecherHogEntity` |
| `hoghunter:ironback_hog` | `IronbackHogEntity` |
| `hoghunter:mire_hog` | `MireHogEntity` |
| `hoghunter:rootmother` | `RootmotherEntity` |

The current source contains **39 tests**:

| Source under `src/main/java/com/hoghunter/test/` | Tests | Scope |
|---|---:|---|
| `HogHunterGameTests.java` | 5 | Seven entity registries/attributes, live AI tick, incoming damage, block placement, block entity |
| `HogDataGameTests.java` | 8 | Missing/malformed NBT, copy/serialization, attachment save, death policy, overlapping effects, payload codec, partial oil drain |
| `HogAbilityGameTests.java` | 12 | Charge, cloud, grapple/occlusion, screech/cap, directional armor, Mire duration/collision, boss arc/phase/call, vanilla save state |
| `HogItemGameTests.java` | 11 | Gun ammo/charge, invalid ammo, timed treatment/cancel, medkit cap, oil/lantern, harpoon wall, salt expiry, snare save/trigger, gate collision, surface extraction |
| `HogResourceGameTests.java` | 3 | All 21 recipe registrations and sample crafting, actual tags/worldgen registries, ore drops and Silk Touch |

The exact test methods and runtime summary remain authoritative as the suite changes. An
additional test method is not a passing test until the server executes it. Deep ritual
interaction, client delivery, authenticated multiplayer, and a full survival run retain the
manual checks described below.

Use controlled positions, explicit preconditions, and mock players where the framework supports
them. Check observable effects such as health, movement, state, inventory, or collision. Do not
substitute direct state assignment for exercising the interaction under test. Test randomness
through deterministic prerequisites and bounds; avoid requiring a specific random spawn roll or
an exact pathfinding tick. Delayed checks use game ticks, not wall-clock sleeps.

A GameTest server must exit zero and report that all required tests passed. Preserve any failure
report and include the failing method; do not suppress errors or turn failed assertions into
success. Mock-player tests establish server logic, not mouse/key handling or a remote client's
rendering.

## 4. Dedicated-server check

The unattended helper runs an isolated loopback server, waits for readiness, verifies command
results, and stops cleanly:

```bash
python3 tools/smoke_server.py --timeout 240
```

It creates a new disposable `run/verification-server-<timestamp>-<pid>` world, writes the test EULA
acknowledgement, uses console input rather than RCON, and stores `server.log`, `commands.txt`, and
`assertions.json` under `verification/server` by default. It verifies all seven summons, a numeric
health decrease, block placement, and save/stop. It does not claim an outgoing-combat or player
interaction test; those are GameTest/client scenarios. `--output` selects an evidence directory.

For an interactive development server, prepare a disposable world and accept the Minecraft EULA
for that environment. The run configuration already supplies `--nogui`:

```bash
./gradlew runServer --no-daemon --console=plain
```

Wait for the `Done (` readiness marker with a bounded timeout. Send commands through console
stdin or an intentionally configured local test connection. Do not assume readiness after a fixed
sleep. Query each summoned entity's UUID/type, record initial health, apply controlled damage,
then query health again. Stop with `stop` and require a clean process exit.

`cmds.txt` is a **manual client visual setup**, intended for a disposable test world with a joined
player. It is not an unattended dedicated-server proof script. A command targeting a missing
player is a failed step.

For packaged proof, prepare a separate NeoForge 21.1.253 server and put the exact just-built mod
JAR in its `mods/` directory. The development run uses source/class directories and must be
reported as development-server proof. Keep credentials, EULA acknowledgements, world saves,
runtime properties, and caches out of the source archive.

## 5. JAR and log checks

Inspect the exact JAR produced by the final build:

```bash
jar tf build/libs/hoghunter-0.1.0.jar
```

Adjust the filename if `mod_version` changes. Required entries include:

```text
META-INF/neoforge.mods.toml
com/hoghunter/HogHunterMod.class
com/hoghunter/content/HogEntities.class
com/hoghunter/entity/HogEntity.class
com/hoghunter/client/model/HogModel.class
assets/hoghunter/lang/en_us.json
assets/hoghunter/sounds.json
assets/hoghunter/textures/entity/hog/boar_hog.png
data/hoghunter/recipe/bolt_gun.json
data/hoghunter/loot_table/entities/rootmother.json
data/hoghunter/structure/empty.nbt
```

Inspect every final entity's skin, every registered inventory/block model, and the generated
metadata values. Verify the Minecraft/NeoForge dependency ranges and the actual mod version.
The runtime JAR should not contain Python generators, source WAVs, runtime worlds, caches, or
verification logs. A JAR listing proves presence, not successful resource decoding.

Scan server, client, GameTest, and build output for registry failures, invalid recipes/loot,
worldgen codec errors, missing textures/models, unknown sound events, mixin/network exceptions,
and client-only classloading. Review expected warnings explicitly; a ready server with a recipe
load failure is not a clean result.

## 6. Client acceptance

```bash
./gradlew runClient --no-daemon --console=plain
```

The client requires a usable LWJGL/OpenGL graphics context. A dedicated server cannot prove that
models bake, HUD state arrives, texture frames sample correctly, or sounds play. When no usable
graphics context or interaction surface is available, record the actual limitation and mark these
checks unverified.

Follow [`docs/RELEASE_CHECKS.md`](../docs/RELEASE_CHECKS.md). Capture a client log and screenshots
covering the seven entities, movement/ability tells, block and item models, survival HUD, darkness,
config intensity, and disconnect/rejoin state. Audio playback needs an actual listening check;
an OGG file header or absence of a log error is insufficient.

## Acceptance language

The workflow at `.github/workflows/verify.yml` configures JDK 21 and the stock wrapper, then runs
resource checks, compilation, build, datagen, GameTests, the dedicated helper, and exact-JAR
verification. It retains evidence artifacts even when a step fails. A workflow file's existence
does not prove a remote run passed; record its actual run URL/result when available.

Use the narrowest accurate description: **source repaired**, **static checks passed**,
**compiled**, **server tested**, or **client verified**, with the relevant evidence. Do not label a
source-only archive as a tested binary release. A fully verified release requires the final source
to build, its server checks to pass, and a real client acceptance pass.

## Primary references

- [NeoForge 1.21.1 GameTests](https://docs.neoforged.net/docs/1.21.1/misc/gametest/)
- [NeoForge 1.21.1 build and server testing](https://docs.neoforged.net/docs/1.21.1/gettingstarted/)
- [ModDevGradle run configuration](https://docs.neoforged.net/toolchain/docs/plugins/mdg/)
