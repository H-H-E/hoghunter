# Hog Hunter

A survival-horror mod for **Minecraft Java Edition 1.21.1**, **NeoForge 21.1.253**, and **Java 21**.
Hunt seven corrupted hog species in Overworld caves, manage heartbeat, noise, injuries, and
lantern fuel, extract evidence, then offer three marked tusks to face Rootmother.

**Development version:** implemented source and verified behavior are different things. Read
[`docs/HANDOFF.md`](docs/HANDOFF.md) for the actual build/test results and remaining verification
limits. The earlier repository's claim of a feature-complete release and five passing tests is not
used as evidence for this completion pass.

## Build and run

Use a 64-bit **JDK 21**. The committed wrapper pins Gradle 8.12. First-time builds download the
Minecraft/NeoForge toolchain and require access to their dependency hosts.

Linux/macOS:

```bash
git clone https://github.com/H-H-E/hoghunter.git
cd hoghunter
java -version
./gradlew --version
python3 tools/verify_resources.py
python3 tools/make_gametest_structure.py --check
./gradlew build --no-daemon --console=plain
./gradlew runGameTestServer --no-daemon --console=plain
./gradlew runClient
```

Windows PowerShell:

```powershell
git clone https://github.com/H-H-E/hoghunter.git
cd hoghunter
java -version
.\gradlew.bat --version
python tools/verify_resources.py
python tools/make_gametest_structure.py --check
.\gradlew.bat build --no-daemon --console=plain
.\gradlew.bat runGameTestServer --no-daemon --console=plain
.\gradlew.bat runClient
```

A successful final build produces `build/libs/hoghunter-0.1.0.jar`. Install that mod JAR in the
`mods/` directory of a compatible NeoForge 1.21.1 installation. For multiplayer, install the same
version on clients and the dedicated server. Do not use the `-sources.jar` as the mod.

`runServer` starts the dedicated development server and `runData` starts the data-generation
bootstrap. A graphical client requires a usable LWJGL/OpenGL context. Full procedures, including
packaged-JAR verification, are in [`design/04-verification.md`](design/04-verification.md).

## Start a survival hunt

1. **Prepare at your base.** Craft a bolt gun, iron bolts, field lantern, oil cans, and medicine.
   Put the lantern in your offhand and right-click it to turn it on. Weapons consume inventory
   bolts; there is no separate magazine or reload key.
2. **Gather tissue.** Corrupted ore generates below Y=48, and Burrowers also drop tissue.
   Craft a root altar using five cobbled deepslate, one copper ingot, and one corrupted tissue.
   Place it outdoors at Y=48 or higher with open sky above.
3. **Extract evidence.** Interact with the surface altar carrying three tissue for tier 1, then a
   hook tooth plus a spore sac for tier 2, then an iron plate after successfully treating a fracture
   for tier 3. Each interaction advances one completed step and consumes its required samples.
4. **Prepare the ritual.** Gather three marked tusks from deep hogs. Place another altar at
   Y=-49 or lower and clear a spacious dry room around it. The boss needs supported, unobstructed
   space four blocks from the altar in at least one horizontal direction.
5. **Return with the heart.** Activate the deep altar, defeat Rootmother, collect her root heart,
   and use the surface altar to finish tier 5. The heart remains a trophy.

This loop uses normal caves and player-built rooms. The mod does not generate a campaign mine,
a surface camp, safe-room quests, or a separate dimension. Recipes and material tooltips provide
survival acquisition paths; the complete behavior and current scope are in
[`design/02-gameplay.md`](design/02-gameplay.md).

## Controls and useful equipment

| Equipment | Action |
|---|---|
| Bolt gun | Right-click for a shot; sneak and hold use for ten ticks to steady a stronger shot |
| Silver bolt gun | Hold use for thirty ticks for a two-bolt pinning shot; early release fires one bolt |
| Mine harpoon | Aim at a hog to damage/pull it, or at a tagged chain/iron-block anchor to pull yourself |
| Field lantern | Right-click to toggle; works while held in either hand; one oil per twelve active seconds |
| Oil can | Hold use to refill fifty oil, capped at one hundred |
| Baited snare | Place on supported ground; it arms, attracts nearby hogs, then damages/roots a victim |
| Salt shaker | Use on supported ground to lay a temporary three-block defensive line |
| Bandage / splint / medkit | Hold use to treat injuries; stay still for a splint and avoid damage/sprinting |
| Root altar | Use at the surface to extract or deep underground to start a valid ritual |
| Depth gate | Use with the required extracted tier to open a passable gate |

The lantern currently uses personal night vision and reveals nearby Mire Hogs. It does not emit
world block light, change spawn lighting, or illuminate the world for other players, and it cannot
be placed or thrown. Rootmother blackout temporarily disables its effect without destroying fuel.

Salt is a real material: smelt a dried kelp block into four salt. A hook tooth, salt, and an amethyst
shard produce a purified tusk. The four armor registry ids are `hoghide_*`; the harness upgrades
the chestplate with actual iron plates.

## The hogs

| Entity | Identity |
|---|---|
| `boar_hog` — Burrower | A telegraphed, committed charge |
| `spore_hog` — Lanternback | An anchored cloud that pulses blindness and stress |
| `hook_hog` — Chainjaw | A line-of-sight grapple that pulls and exposes the hunter |
| `screecher_hog` — Squealer | A pressure pulse, sprint lock, and bounded reinforcement |
| `ironback_hog` — Ironback | Front armor and a vulnerable rear during its slow turn |
| `mire_hog` — Mire Hog | A water-triggered vanish and safe rear repositioning |
| `rootmother` — Rootmother | Reinforcement calls, a frontal sweep, a boss bar, and a fuel-preserving blackout |

Five species spawn naturally. Screechers join encounters; Rootmother requires the ritual. Hogs
can acquire targets through sight, scent, or noise. Their ability tells, treatment feedback, and
HUD are subject to the real-client acceptance checks below; static assets alone do not prove the
presentation works.

## Verification and project layout

- [`docs/HANDOFF.md`](docs/HANDOFF.md): exact current results, limitations, and next verification step.
- [`docs/RELEASE_CHECKS.md`](docs/RELEASE_CHECKS.md): focused real-client and survival acceptance scenarios.
- [`design/01-architecture.md`](design/01-architecture.md): actual toolchain, code ownership, authority, and paths.
- [`design/02-gameplay.md`](design/02-gameplay.md): mechanics, survival progression, and deliberate scope limits.
- [`design/03-assets.md`](design/03-assets.md): reproducible asset authoring and runtime resource contract.
- [`design/04-verification.md`](design/04-verification.md): what each automated/runtime gate proves.
- [`cmds.txt`](cmds.txt): visual setup commands for a joined player in a **disposable test world**.

Java source is under `src/main/java/com/hoghunter`; resources are under `src/main/resources`.
Minecraft 1.21.1 server data uses singular `recipe`, `loot_table`, `tags/item`, `tags/block`, and
`structure` directories. `tools/` contains the asset/template generators and resource verifier.

Enemy/fuel/pressure settings and visual/audio intensity are bounded in `HogHunterConfig` and saved
as NeoForge world server configuration. A full release still requires an actual client playthrough,
audio listening, and a successful build/runtime record for the final source.

## License

[MIT](LICENSE). The repository includes the source generators for its custom procedural assets.
