# Hog Hunter engineering handoff

**Completion pass:** 2026-10-03 UTC.

**Target:** Minecraft Java 1.21.1 / NeoForge 21.1.253 / JDK 21 / Gradle 8.12.

**Baseline inspected:** Git commit `3b538c53d138d5e27d199485213a761b9d9090c0`.

## Status

The completion pass repairs the mod's survival acquisition, progression, item interactions,
entity abilities and persistence, state synchronization, client presentation, resources, and
verification. It does not turn the larger original mine-campaign plan into a shipped campaign.
The actual playable scope is ordinary caves plus a craftable extraction/ritual altar.

The repaired version builds, passes **all 39 required GameTests**, and passes fifteen unattended
dedicated-server assertions. An actual graphical Minecraft client loaded the packaged JAR and
displayed the corrected models and synchronized survival HUD. A hosted GitHub Actions run also
passed on a stock Ubuntu/JDK 21 environment. The remaining limits are full survival/combat
acceptance, audible listening, and separate authenticated multiplayer, as detailed below.

The results in this handoff come from the new completion pass. Historical build/server/five-test
claims from the earlier handoff are not substituted for verification of changed code.

## 1. Verification record

| Command / gate | Recorded result | Evidence / limit |
|---|---|---|
| `java -version`; `./gradlew --version` | Passed: JDK 21 / Gradle 8.12 | Local environment adaptation is documented below; hosted CI used the stock upstream toolchain |
| `python3 tools/verify_resources.py` | Passed | 30 registered items, six blocks, seven species, 26 sounds, and 144 JSON resources; structural checks are not gameplay tests |
| `python3 tools/make_gametest_structure.py --check` | Passed | Deterministic 33 × 8 × 33 template, independently decoded; exact generated NBT matches the committed file |
| `python3 tools/gen_assets.py --manifest tools/assets/manifest.json --out src/main/resources --seed 20261003 --check` | Passed for 52 declared outputs | Regenerated in a temporary directory; original asset-tree hashes unchanged by the check |
| `./gradlew compileJava --no-daemon --console=plain` | Passed; final `build` also executed `compileJava` | `integrated/compile-1`, `integrated/build-release`, and the hosted CI run |
| `./gradlew build --no-daemon --console=plain` | Passed, exit 0 | `integrated/build-release`; rebuilt after the final client Java/model/armor repairs |
| `./gradlew runData --no-daemon --console=plain` | Passed, exit 0 | `integrated/datagen-2` and hosted CI; **zero datagen providers**, so this is bootstrap evidence |
| `./gradlew runGameTestServer --no-daemon --console=plain` | All **39/39 required tests passed**, exit 0 | `integrated/gametest-4` and hosted CI; controlled server behavior, not a full survival playthrough |
| `python3 tools/smoke_server.py --timeout 240` | All **15 assertions passed**, clean exit 0 | `integrated/server-2` and hosted CI: readiness, seven species, incoming damage, and six blocks on a dedicated development server |
| `python3 tools/verify_resources.py --jar build/libs/hoghunter-0.1.0.jar` | Passed on the final local JAR | `integrated/jar-resources-release.log`; current resource bytes/class presence and 24 native model/texture assets checked against the real Minecraft resource archive |
| Actual client, including packaged-JAR launches | Observed scenarios passed; packaged save/stop verified | `integrated/client-2`, `integrated/client-final`, and `integrated/client-release`; exact scope below, with OpenAL null output |
| Hosted clean-checkout CI | All steps passed on commit `4dd3fc8c4aab31a55c0b4e0f5dcce453940228ff` | [Run 37161739823](https://github.com/H-H-E/hoghunter/actions/runs/37161739823), using ordinary hosted Ubuntu/JDK 21 and unmodified upstream tools |

The stock hosted run establishes clean-checkout toolchain support independently of the local
workaround. Subsequent source revisions are checked by the same
[verification workflow](https://github.com/H-H-E/hoghunter/actions/workflows/verify.yml). The final
delivery's evidence JSON records its exact concluding commit/run rather than treating an earlier
candidate as proof of later changes.

### Local toolchain adaptation

The build environment used official Eclipse Temurin **21.0.12.1+1-LTS** and the repository's
Gradle **8.12** wrapper. Normal NeoFormRuntime 1.0.40 bootstrap failed before mod compilation
because `ProcessHandle.current().info().command()` is empty in this container. Two external
NeoFormRuntime helper call sites were given a fallback to `java.home/bin/java` and loaded through
a scratch-only Gradle init script. The original dependency artifact was retained, and no mod
source, target API, or repository dependency was replaced to accommodate the environment.

The successful local Gradle invocations used this form, with the exact absolute invocation in
each evidence directory's `result.json`:

```text
bash gradlew -I <external-nfrt-portability.init.gradle> <task> --stacktrace --no-daemon --console=plain --max-workers=4
```

The portability patch and before/after hashes are recorded as
`nfrt-java-home-fallback.patch` and `nfrt-portability-record.json` in the accompanying verification
evidence. The proxy truststore/launcher were also external environment setup. These adapted local
runs and the passed stock hosted run are separate, explicitly identified evidence.

The evidence directories contain full logs, exact launcher arguments, and exit codes. Earlier
integration attempts remain in the evidence history; the successful labels above supersede them.
The dedicated helper writes an isolated world plus `verification/server/server.log`, `commands.txt`,
and `assertions.json`; it never resets an existing development world.

### Packaged artifact and client observations

The final local build produced `hoghunter-0.1.0.jar`, **337,906 bytes**, with SHA-256:

```text
659b11159e28f966b6bd0066a5821b7931da886c8a4071e2440b3af3c32db788
```

`integrated/release-artifacts.json` records the binary and source-JAR hashes. The release client copied
this exact binary into an isolated `mods` directory, cleared the exploded ModDev mod source
mapping, and entered a saved world. The launch configuration and hash are recorded in
`integrated/client-release/session.json`. The preceding packaged run's
`integrated/client-final/launch-evidence/packaged-load-proof.json` also records captured launch
files without exploded mod paths. These clients used a real 1280 × 720 Xvfb/Mesa llvmpipe OpenGL
context. The packaged save/stop run saved all dimensions and exited cleanly with code 0; session
and status records identify each individual invocation.

The inspected client captures establish:

- All seven entities render with grounded geometry and visible species details; all thirty item
  icons and all six block models are visible after the model fixes.
- The final worn hoghide set has complete brown leather coverage, and the harness has aligned iron
  chest/shoulder coverage. They retain custom stats and use native layers; old generated armor
  PNGs remain provenance resources rather than worn textures.
- The held silver gun and dark/lit lantern models render. A real gun-use click changed the HUD
  ammunition from 32 to 31, and the lantern click changed state, model core, feedback, and subtitle.
- The survival HUD shows live BPM, oil, ammunition, wounds, and fractures. It hides with F1;
  the final GUI-scale-3 capture keeps it below the Rootmother bar without overlap.
- Ordinary damage commands produce visible injuries and reduced maximum health, and saving and
  reopening the world preserves the recorded state. A fracture-two movement check showed no
  idle drift and no material speed increase from holding the sprint key while walking.

`integrated/client-2/movement.json` records three-second distances of 0 blocks while idle,
10.148 while walking, and 10.356 while holding walk plus sprint. This is a bounded integrated-client
check, not a latency or every-input-mode guarantee. `audio-dispatch.json` records all 26 sound
events dispatched without matching unknown/unavailable sound warnings.

The gallery uses `NoAI` entities. It proves model visibility, not every walk/attack/death animation.
The client used **OpenAL null output**: subtitles and playback dispatch were observed, but no
human listening pass is claimed. The packaged run used an integrated server; it is not a separate
authenticated multiplayer acceptance run. Full progression without commands, live ability
counterplay, balance, and audio listening remain the scenarios in [`RELEASE_CHECKS.md`](RELEASE_CHECKS.md).

Reviewed runtime warnings include failed offline authentication/profile DNS lookups at
`api.minecraftservices.com` and `sessionserver.mojang.com`, vanilla command-teleport ambiguity,
the development pack's `union:` resource schema, and vanilla shader/goat-horn warnings. An initial
client test-options error set simulation distance to three; the final launcher uses five. A real
missing silver-gun texture and blank voxel item models were repaired and checked again rather
than classified as environmental warnings. No authenticated multiplayer claim follows from the
offline runs, and a warning review is not permission to ignore mod registry/data/network errors.

## 2. Important defects found and repaired

| Area | Confirmed baseline problem | Completion work |
|---|---|---|
| Survival recipes | Obsolete plural directories hid recipes; result keys and armor ids were also stale | Singular 1.21.1 paths, correct result schema/ids, real material sources and recipe discovery |
| Natural spawning | Placement predicates existed without biome spawn entries; short proximity conditions conflicted with vanilla spawn distance | Biome modifiers, viable predicates, local bounds, and reachable species materials |
| Player HUD | State payload was never sent and attachment syncing had no configured client mirror | Explicit authoritative snapshots and client lifecycle reset; actual inventory ammo count |
| Physiology | Injuries, treatment, noise sources, fuel drain, and several exposed settings were inert or incomplete | Server injury/treatment/fuel/noise/panic behavior; removed unused stalk toggles |
| Persistence | Hog save overrides skipped superclass state; temporary boss fuel storage could lose or restore stale oil | Vanilla save preservation, cooldown/phase state, fuel-independent blackout timers |
| Combat | Generic back-facing armor applied to all species; sweep arc was twice the intended width; abilities lacked robust tells/limits | Ironback-only source-relative armor, 120-degree sweep, clear ability phases/cooldowns/caps |
| Tools | Medical items consumed without treatment; charged gun completion, armor piercing, pulls, and snares were incomplete | Actual timed treatments, single-fire charge paths, inventory/durability accounting, bounded movement, persistent placed snare |
| Blocks/progression | Gate had no award path and remained solid when open; boss arena helper was unreachable | Extraction tiers, functional collision, a survival ritual, and persisted active-boss reservation |
| Renderer/assets | Generic renderer missed standard transforms; 64×512 entity strips were sampled as ordinary skins; models/HUD/sounds had gaps | Living-mob renderer, explicit frame/patch sampling, seven variants, HUD/audio, complete referenced resources |
| Verification | Five broad tests missed major mechanics; NBT writer nested list compounds incorrectly; asset `--check` overwrote outputs | Correct template/independent decode, behavior tests, strict resource/JAR checks, nonmutating asset verification |

The malformed baseline template is an observation about the inspected files. It does not by
itself establish what a historical test invocation did in another checkout or runtime directory.
The newly executed gates are the basis for this handoff.

The real client also caught two issues that structural reference checks initially missed: a
nonexistent vanilla `quartz_block` texture name and explicit voxel elements inheriting a generated
item parent. They now use a real texture and a geometry-preserving parent. The verifier checks
actual Minecraft asset names and generated-marker ancestry, with failing reproductions for both
regressions. A GUI-scale-3 boss-bar overlap was likewise corrected and checked on the packaged run.
Worn armor inspection found unusable coverage in the original generated armor artwork; explicit
native leather/iron layers now provide aligned coverage, and hoghide has the vanilla dyeable tag.

## 3. Survival route and controls

The README contains the shortest walkthrough. The important content connections are:

- Corrupted ore and early hogs supply tissue. A craftable surface root altar banks sequential
  evidence tiers: three tissue, then tooth plus sac, then an iron plate after treating a fracture.
- Salt comes from dried kelp smelting; purified tusks use hook tooth, salt, and amethyst. There
  are no sugar/bone placeholders for these two materials.
- A deep altar at Y<=-49, tier 3, three marked tusks, and a prepared dry room start Rootmother.
  Offerings are consumed only after a valid spawn. The world tracks one active ritual boss,
  including unloaded encounters. A surface heart extraction finishes tier 5 and keeps the trophy.
- Guns consume actual inventory bolts. Early silver release fires normally; a completed charge
  costs two bolts and pins an eligible hog. Medical and oil use are timed, validated transactions.
- A held/offhand toggled lantern consumes oil and supplies personal night vision/reveal. It does
  not emit world block light. Blackout suspends availability without modifying stored fuel.
- Vanilla death inventory rules remain in effect. Player tier and fracture-treatment history
  survive; temporary expedition pressure/injuries/locks reset.

Exact mechanics, default numbers, and the limits compared with the original plan are in
[`design/02-gameplay.md`](../design/02-gameplay.md).

## 4. What remains outside this version

The implemented route uses ordinary caves and player-built rooms. It does not include a generated
five-floor mine/camp, authored quest/safe-room/scare templates, a separate stalk director, a
magazine/reload or blueprint interface, placed/thrown radius-light lanterns, root vents, or camp
fast travel. Those broader design ambitions are outside the implemented version.

The remaining acceptance work is practical: play the live combat and deep ritual scenarios,
listen to the audio, connect a separate authenticated multiplayer client, and complete a normal
survival playthrough to assess difficulty/drop/fuel pacing. The observed client checks are useful
evidence but do not replace that playthrough. See [`RELEASE_CHECKS.md`](RELEASE_CHECKS.md).

## 5. Maintenance map and invariants

- `HogHunterMod` wires common registration. Attribute and spawn listeners must each be installed
  once. NeoForge 21.1.253's inferred subscriber bus was not itself the missing-state-sync defect.
- `HogAttachments`/`HogHunterPlayerData` own persistent state; `HogHunterPlayerEvents` applies server
  rules and lifecycle resets; `HogNetworking` sends display snapshots. Do not introduce a second
  authoritative ammo, oil, or progression store on the client.
- `HogEntity` and species subclasses own target/ability state. Call superclass save methods;
  honor interruption/cooldown rules; keep Rootmother immune to ordinary rooting/pulls.
- `RootAltarBlock` performs evidence/ritual transactions. `RootmotherArenaGenerator` finds safe
  prepared space rather than overwriting builds. `RootmotherRitualData` protects encounter identity
  across unloads and dimensions.
- `HogNestBlockEntity`/`BaitedSnareBlockEntity` persist actual cooldown/armed state; test placement,
  recovery, reload, invalid support, and population bounds.
- `HogRenderer`, `HogModel`, `SkinPatchConsumer`, `HogDetailsLayer`, `HogHud`, and `HogClientAudio`
  remain client-only. Existing illustration strips require explicit sampling.
- Runtime resources use singular `recipe`, `loot_table`, `structure`, `tags/item`, and `tags/block`.
  Vanilla mining tags live in `data/minecraft/tags/block`, not only the mod namespace.

`tools/verify_resources.py` and the GameTests are complementary. The former catches broken
references/formats before launch; the latter checks actual server behavior. The asset manifest
covers the declared procedural PNG/metadata/OGG subset, not all runtime JSON. Its recorded authoring
toolchain is the reproducibility reference.

## 6. Release maintenance

After source changes, use [`design/04-verification.md`](../design/04-verification.md), rebuild the
binary, retain the matching evidence, and update this record. Pair distributed artifacts with
their actual commit, SHA-256, and workflow result. A source ZIP remains a useful fallback when a
remote write is unavailable, provided its verification state is clear. A configured workflow,
an old JAR, or an unexecuted test is never a replacement for an executed gate.
