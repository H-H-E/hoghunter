# Hog Hunter engineering handoff

**Completion pass:** 2026-10-03 (started in the user's local timezone).

**Target:** Minecraft Java 1.21.1 / NeoForge 21.1.253 / JDK 21 / Gradle 8.12.

**Baseline inspected:** Git commit `3b538c53d138d5e27d199485213a761b9d9090c0`.

## Status

The completion pass repairs the mod's survival acquisition, progression, item interactions,
entity abilities and persistence, state synchronization, client presentation, resources, and
verification. It does not turn the larger original mine-campaign plan into a shipped campaign.
The actual playable scope is ordinary caves plus a craftable extraction/ritual altar.

**Final integration evidence is being collected.** The table below is the release record and must
be finalized against the last source revision before an artifact is described as release-ready.
Historical build/server/five-test claims from the earlier handoff are not substituted for new
verification of changed code.

## 1. Verification record

| Command / gate | Current result | Meaning / limit |
|---|---|---|
| `java -version`; `./gradlew --version` | JDK 21 and Gradle 8.12 available in the execution environment | Build environment required a scratch-only compatibility workaround; details below |
| `python3 tools/verify_resources.py` | Static resource pass reported during integration | Re-run after final source/resources; does not prove game rendering or codecs |
| `python3 tools/make_gametest_structure.py --check` | Corrected template generated and independently decoded | Exact checked-in NBT must continue to match generator |
| `python3 tools/gen_assets.py --manifest tools/assets/manifest.json --out src/main/resources --seed 20261003 --check` | Passed for 52 declared outputs | Compared in a temporary directory; asset-tree hashes unchanged by check |
| `./gradlew compileJava --no-daemon --console=plain` | Integrated compilation passed; final rerun pending | Against exact target dependencies through the execution environment adapter |
| `./gradlew build --no-daemon --console=plain` | Integrated build passed; final rerun pending | Final JAR must be rebuilt after all repairs |
| `./gradlew runData --no-daemon --console=plain` | Integrated datagen bootstrap passed; final rerun pending | Resources are hand-authored; no claim that this alone validates recipes |
| `./gradlew runGameTestServer --no-daemon --console=plain` | First integrated run: 23/25; Burrower and Lanternback cases under investigation | This is a failed gate until every required test passes |
| `python3 tools/smoke_server.py --timeout 240` | First run passed fourteen assertions and clean exit 0; final six-block rerun pending | Dedicated development server, not a separate packaged-JAR installation |
| `python3 tools/verify_resources.py --jar build/libs/hoghunter-0.1.0.jar` | Final result pending | Must inspect the JAR rebuilt from final source |
| `./gradlew runClient` and real-client scenarios | Unverified until a usable graphics/audio run is recorded | Server tests cannot establish visuals, controls, HUD delivery, or audibility |
| `.github/workflows/verify.yml` | Workflow configured | Remote execution is not implied by committing a workflow |

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
evidence. The proxy truststore/launcher were also external environment setup. A stock
clean-checkout run through the configured CI workflow remains distinct from these adapted runs.

Full logs, exact launcher/arguments, process exit codes, and JAR hash should accompany the final
record. Initial evidence labels are `integrated/compile-1`, `build-1`, `datagen-1`, `gametest-1`,
and `server-1`; later final runs supersede them. The dedicated helper writes an isolated world plus `verification/server/server.log`,
`commands.txt`, and `assertions.json`; it never resets an existing development world.

Reviewed runtime warnings include the offline test environment's failed authentication-key DNS
lookup at `api.minecraftservices.com` and vanilla command-teleport ambiguity warnings. These did
not prevent the isolated offline server from passing its command assertions. They are not
evidence of working authenticated multiplayer, and mod registry/data/network errors must not be
hidden in that warning allowance.

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
fast travel. These were broader design ambitions, not secretly working systems in the baseline.

The remaining acceptance work is practical: verify final automated gates on the final revision,
run the real-client scenarios, listen to the audio, and complete a normal survival playthrough to
assess difficulty/drop/fuel pacing. A client launch without visible errors is still weaker than
that playthrough. See [`RELEASE_CHECKS.md`](RELEASE_CHECKS.md).

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

## 6. Before publishing a binary

Use [`design/04-verification.md`](../design/04-verification.md), collect final evidence, and update
section 1. Publish a JAR only when its final build succeeds. A source ZIP is an honest fallback
when a target environment or remote write is unavailable; label its actual verification state.
Do not call a configured CI workflow, an old JAR, or an unexecuted test a passing release gate.
