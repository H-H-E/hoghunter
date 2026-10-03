# Hog Hunter verification, testing, and runtime proof

This plan is for Minecraft Java **1.21.1**, NeoForge **21.1.253**, Java **21**, mod id `hoghunter`, and package root `com.hoghunter`. It is a verification plan only. It does not create the Gradle project, Java source, JSON, NBT, models, sounds, or other assets.

The proof standard is deliberately strict: a successful compilation proves that Java and resources are structurally consumable; a successful GameTest proves selected server-side behavior inside Minecraft; a dedicated-server smoke test proves the packaged mod loads and responds to commands; a client run is required for rendering and audio. None of the earlier gates is a substitute for the later one.

## 1. Required Gradle gates

The project root for every command below is:

```text
C:\Users\Windows\Downloads\Hcubed_The_Mercer_Contract\hoghunter
```

The supplied Gradle executable is `C:\Users\Windows\Downloads\Hcubed_The_Mercer_Contract\work\gradle-8.12\bin\gradle.bat`. Once the project exists, the preferred invocation is the project wrapper (`.\gradlew.bat`) so the project pins its Gradle distribution. Until a wrapper exists, use the supplied executable exactly:

```powershell
Set-Location 'C:\Users\Windows\Downloads\Hcubed_The_Mercer_Contract\hoghunter'
$gradle = 'C:\Users\Windows\Downloads\Hcubed_The_Mercer_Contract\work\gradle-8.12\bin\gradle.bat'
& $gradle --version
& $gradle compileJava --stacktrace
& $gradle build --stacktrace
& $gradle runData --stacktrace
```

The same gates with a wrapper are:

```powershell
.\gradlew.bat compileJava --stacktrace
.\gradlew.bat build --stacktrace
.\gradlew.bat runData --stacktrace
```

Run them separately and stop on the first non-zero exit code. Record the command, UTC timestamp, exit code, and the complete console output under a proof directory such as `verification\2026-10-03T...\`. Do not call a build “passing” because the JAR exists: the exit code must be zero and the expected warnings must be reviewed.

`compileJava` catches Java syntax, imports, generics, access-level errors, wrong NeoForge/Minecraft method signatures, and accidental client-only references from common/server code. It does not prove resources, data loading, registry contents, AI behavior, combat, or rendering.

`build` catches the complete project packaging path: resource processing, compilation, the mod JAR task, and any configured verification tasks. It must produce `build\libs\hoghunter-<version>.jar` (the exact version comes from `gradle.properties`). Inspect the JAR from this gate; do not inspect an old manually copied JAR.

`runData` catches datagen bootstrap failures, invalid codec/registry data generation, and generator crashes. If the project uses hand-authored assets or data, `runData` cannot validate their contents by itself; it still must pass because the mod's data-generation entry point must load cleanly. Generated output must be compared with the intended `src\main\resources` layout and must not silently overwrite hand-authored files.

Before any gate, verify that the build is using the intended toolchain:

```powershell
java -version
& $gradle --version
```

The Java output must identify a 64-bit Java 21 VM. The Gradle output must show Gradle 8.12 or the wrapper-pinned equivalent, and the NeoForge dependency must resolve to `21.1.253`.

## 2. NeoForge GameTest setup

Use the 1.21.1 `net.minecraft.gametest.framework` API. The test source belongs in the normal main source set so the `gameTestServer` run can load it:

```text
src\main\java\com\hoghunter\gametest\HogHunterGameTests.java
src\main\resources\data\hoghunter\structure\hog_combat_test.nbt
```

The structure is an actual Minecraft structure template, not a JSON description. It should contain a small sealed stone test room, a solid floor, a safe player/test origin, and any required interaction block. It must be saved as `data/hoghunter/structure/hog_combat_test.nbt`; the namespace and path are case-sensitive. The test class is in `com.hoghunter.gametest` but the template is addressed through the `hoghunter` namespace.

Register the test class with the mod id:

```java
@GameTestHolder("hoghunter")
public final class HogHunterGameTests {
    @GameTest(template = "hog_combat_test", timeoutTicks = 200, setupTicks = 10, required = true)
    public static void hogCombat(GameTestHelper helper) {
        // test body; every success path must call helper.succeed()
    }
}
```

The important exact behavior is the annotation, not the class name. With `@GameTestHolder("hoghunter")` and `template = "hog_combat_test"`, the template is resolved from `data/hoghunter/structure/hog_combat_test.nbt`; if `template` is omitted, the lower-case method/class naming convention can produce a different path. Keep the explicit template to make resource review deterministic.

The test method must be `public static void` and accept `GameTestHelper`. Use `helper.absolutePos(relativePos)` when a test needs a world position, `helper.runAfterDelay(ticks, runnable)` for delayed assertions, and `helper.succeed()` only after all assertions have passed. A condition that is expected to become true later belongs in `helper.succeedWhen(runnable)` or `helper.succeedOnTickWhen(tick, runnable)`. Failures should throw through the framework's assertion helpers rather than being caught and converted to a success.

If the project chooses event registration instead of annotation discovery, register the class on the mod event bus with `RegisterGameTestsEvent` in a listener such as `com.hoghunter.HogHunterGameTestRegistration`:

```java
@SubscribeEvent
public static void registerTests(RegisterGameTestsEvent event) {
    event.register(HogHunterGameTests.class);
}
```

Do not use both approaches for the same class. The simpler required route is `@GameTestHolder("hoghunter")`, which is automatically enabled for the mod namespace by the standard run configuration. In `build.gradle`, the `gameTestServer` run must have `setForceExit false`; otherwise NeoGradle's forced process exit can make a successful test server appear as a Gradle failure. The run must be a `gameTestServer` type, and the project should set `neoforge.enabledGameTestNamespaces` to `hoghunter` when isolating this mod's tests.

Run the full in-game test suite with:

```powershell
& $gradle runGameTestServer --stacktrace
```

The test server's exit code is the number of required failed tests. A zero exit code is required. The log must contain the named test completion and no `FAILED`/`GameTestAssertException` lines. Save the full output, including the final exit code, as the GameTest proof artifact. A successful `runGameTestServer` is server-side proof only; it does not prove a renderer, model, texture, animation, sound event, or client input path.

### Required GameTests

Freeze the entity registry names before implementation. This plan uses the required test roster `hoghunter:boar_hog`, `hoghunter:brute_hog`, and `hoghunter:alpha_hog`; if the design roster changes, update every registry, summon command, GameTest, and acceptance record together. Do not leave a test claiming to cover an entity that is absent from the final registry.

Use separate required tests, or one required test with clearly labelled phases, for the following:

1. **Registry and spawn.** Resolve each `HogEntities` holder and assert it is non-null; create each entity through its registered `EntityType` on the `ServerLevel`, place it at a known relative position, and assert `level.getEntitiesOfClass(HogEntity.class, ...)` contains the expected registered type. Also assert each entity is alive and has the expected initial max health. This catches a registry holder that exists but creates the wrong class or wrong type.
2. **AI.** Spawn a hog and a stationary target mob in the structure room. Give the hog enough room and at least 80 ticks. Assert its position changes toward the target or that it enters the intended attack range, according to the actual AI contract. Do not assert a particular path node or exact tick; assert observable behavior. If the AI is intentionally dormant in daylight or outside a trigger, construct the trigger in the template and assert the dormant-to-active transition explicitly.
3. **Combat.** Record `float before = hog.getHealth()`, make the target close enough for the hog's attack goal, wait for the attack window, and assert the target's health decreased or its hurt timestamp changed. Then damage the hog with `hog.hurt(level.damageSources().generic(), 1.0F)` and assert its health decreased. A test that only calls `hurt` and never checks a changed value is not a combat proof.
4. **Block interaction.** Put the exact Hog Hunter block used by the mechanic in the NBT template, obtain its `BlockState` at a relative position, and invoke the supported interaction path with a `FakePlayer` only if the block's contract is server-side. Otherwise use the registered block's `useItemOn`/`useWithoutItem` path with a real test player supplied by the GameTest environment. Assert the block state, block entity state, or inventory result changed as designed. Also assert that an invalid tool/item leaves the state unchanged. Do not test by directly mutating the block state; that bypasses the interaction code.

Keep each test deterministic: no random seed assertions, no wall-clock sleeps, no dependence on a pre-existing world, and no `/summon` command inside a GameTest when direct registry construction can test the same behavior. Use a fixed structure and explicit positions. If a test depends on a sound or visual effect, record that dependency as a client test rather than pretending GameTest can hear or see it.

## 3. Headless dedicated-server smoke proof

The dedicated-server test uses the generated development run, not a single-player integrated server. First accept the development EULA in the run directory and disable authentication for a local test account:

```powershell
Set-Location 'C:\Users\Windows\Downloads\Hcubed_The_Mercer_Contract\hoghunter'
New-Item -ItemType Directory -Force -Path '.\run\server' | Out-Null
Set-Content -Path '.\run\server\eula.txt' -Value 'eula=true'
```

The future `server` run must use `gameDirectory = project.file('run/server')`. After the first launch, edit `run\server\server.properties` so it contains `online-mode=false`, `enable-rcon=true`, `rcon.port=25575`, and a test-only `rcon.password` that is never committed. Keep `server-port` at a free local port, preferably `25565` for the standard proof.

The exact development launch command is:

```powershell
& $gradle runServer --args='--nogui' --stacktrace
```

`--nogui` is a Minecraft server program argument. `runServer` is the NeoGradle/ModDevGradle run task and must be executed from the project root so the development classpath loads the source mod. For a packaged proof after `build`, run a separately prepared NeoForge server whose `mods` directory contains the exact JAR under test; do not call the development classpath proof a packaged-server proof.

### Automated PowerShell smoke script behavior

Create the smoke script later at `verification\run-dedicated-smoke.ps1` (the script is implementation work, not part of this planning deliverable). It must:

1. Start `gradle.bat runServer --args=--nogui` with `System.Diagnostics.ProcessStartInfo`, `WorkingDirectory` set to the project root, and stdout/stderr redirected. Do not use a fixed `Start-Sleep` as the readiness condition.
2. Append every output line with an ISO-8601 timestamp to `verification\<run-id>\server.log` and mirror it to the PowerShell host.
3. Wait until the output contains the exact readiness marker `Done (` from the server log. Fail after a bounded timeout such as 180 seconds and include the last 100 log lines in the failure report.
4. Send commands through redirected standard input, one command per line, and wait for evidence in the captured output. The minimum command sequence is:

```text
gamerule commandBlockOutput true
gamerule keepInventory true
time set day
tp SmokeTester 0 80 0
summon hoghunter:boar_hog 0 80 0 {PersistenceRequired:1b}
summon hoghunter:brute_hog 3 80 0 {PersistenceRequired:1b}
summon hoghunter:alpha_hog 6 80 0 {PersistenceRequired:1b}
data get entity @e[type=hoghunter:boar_hog,limit=1,sort=nearest]
data get entity @e[type=hoghunter:brute_hog,limit=1,sort=nearest]
data get entity @e[type=hoghunter:alpha_hog,limit=1,sort=nearest]
```

The exact NBT is optional only if the entity does not use `PersistenceRequired`; if it is sent, the server must accept it. The assertions must be based on command output, not on the absence of a crash. Each `data get entity` response must identify the requested type and an entity UUID. If no console player named `SmokeTester` exists, the script must create one through the approved test setup or use a local RCON client; a teleport command targeting a nonexistent player is a failed step, not a pass.

5. Prove damage through a controlled server command and query. The script may use a temporary invulnerable target or a test player, but the operation must be observable. A robust command sequence is:

```text
summon minecraft:armor_stand 0 80 2 {Invulnerable:0b,NoGravity:1b,Tags:["hoghunter_smoke_target"]}
data get entity @e[type=minecraft:armor_stand,tag=hoghunter_smoke_target,limit=1]
```

Then place the target within the hog's actual aggro range, wait at least the AI acquisition/attack budget (for example 200 ticks), and query the target with `data get entity ... Health`. The pre-attack health and post-wait health must be parsed as numbers; post-wait must be lower. If the designed attack only damages players, teleport `SmokeTester` into range and use `data get entity SmokeTester Health`, with `gamemode survival SmokeTester` set first. If the designed attack requires a special trigger, the smoke script must issue that trigger and record it.

6. Assert the reverse combat path by issuing a damage command that targets the hog, for example:

```text
damage @e[type=hoghunter:boar_hog,limit=1,sort=nearest] 1 minecraft:generic
data get entity @e[type=hoghunter:boar_hog,limit=1,sort=nearest] Health
```

The script must compare the recorded initial hog health with the post-damage value. If the hog is immune to `generic`, use the documented damage source that should hurt it and record that choice in the smoke output.

7. Stop cleanly by sending `stop`, wait for process exit, and fail if the process does not exit within a bounded timeout. Save `server.log`, `commands.log`, `assertions.json` or `assertions.txt`, the Gradle exit code, and the final process exit code under the same run directory. The script's own exit code must be zero only if every assertion passed and the server stopped cleanly.

Console stdin is the preferred no-extra-dependency route. RCON is an acceptable alternative if stdin is unreliable, using an explicitly pinned RCON client and the password from the uncommitted test properties. Never print the RCON password. The server log must show `hoghunter` loading without a registry exception, and the command responses must show all three entity identifiers and numeric health changes.

## 4. Client rendering proof and its limit

The actual client proof is a separate run:

```powershell
& $gradle runClient --stacktrace
```

On Windows, an offscreen proof is feasible only if the machine has a working graphics driver and the Java client can create an OpenGL context under the selected display/session. The practical route is an interactive or virtual-display session with the client launched from `runClient`, then an automation driver such as Chrome DevTools is not applicable because this is a native LWJGL window. Capture the client log, the window/display mode, the exact world/commands used, and screenshots or video of every hog model, animation state, particle, block model, item model, and sound-triggering interaction.

Do not claim headless rendering proof when the client cannot create a real graphics context. Windows service sessions, RDP sessions with no usable GPU context, or a CI worker that only runs the dedicated server prove nothing about model baking, texture binding, animation, particles, post-processing, or audio. In that case, state exactly: “Dedicated-server and GameTest proof passed; client rendering/audio proof was not run because the Windows environment could not create a usable LWJGL/OpenGL context.” A human launching the client on a supported desktop then remains a required acceptance step.

For an actual client pass, verify at minimum: the title screen reaches the world without a mod-loading error; `/summon hoghunter:boar_hog`, `/summon hoghunter:brute_hog`, and `/summon hoghunter:alpha_hog` each render with the intended model and texture; idle, walk, attack, hurt, and death states do not show missing-model geometry; the interaction block has its model, particles, and sound; and `latest.log` contains no `Missing model`, `Unable to load texture`, `Unable to play unknown soundEvent`, or renderer exception lines.

## 5. JAR and log inspection

After `build`, inspect the exact JAR path reported by Gradle:

```powershell
$jar = Get-ChildItem '.\build\libs\hoghunter-*.jar' | Sort-Object LastWriteTime -Descending | Select-Object -First 1
jar tf $jar.FullName | Set-Content '.\verification\jar-contents.txt'
Select-String -Path '.\verification\jar-contents.txt' -Pattern 'META-INF/neoforge.mods.toml','assets/hoghunter/','data/hoghunter/','com/hoghunter/'
```

The inspection must confirm, at the exact paths expected by the implementation:

```text
META-INF/neoforge.mods.toml
com/hoghunter/HogHunter.class
com/hoghunter/entity/HogEntity.class
com/hoghunter/entity/HogEntities.class
assets/hoghunter/models/entity/boar_hog.json
assets/hoghunter/models/entity/brute_hog.json
assets/hoghunter/models/entity/alpha_hog.json
assets/hoghunter/textures/entity/boar_hog.png
assets/hoghunter/textures/entity/brute_hog.png
assets/hoghunter/textures/entity/alpha_hog.png
assets/hoghunter/lang/en_us.json
assets/hoghunter/sounds.json
data/hoghunter/structure/hog_combat_test.nbt
```

The entity model paths above are a contract for this plan. If the renderer uses code-defined models or a different resource convention, update the contract before implementation and inspect the actual required paths. A JAR entry proves presence only; it does not prove the JSON parses or the model bakes.

Inspect `META-INF/neoforge.mods.toml` by extracting it to a temporary verification directory and checking that `modId="hoghunter"`, the declared version is the project version, the loader/version range matches NeoForge 21.1.253's 1.21.1 line, and the dependency block declares the required `minecraft` version range. The file must not contain a placeholder mod id or an old example mod name.

Search logs for both positive and negative signals:

```powershell
rg -n -i 'hoghunter|Done \(|GameTest|FAILED|Exception|Missing model|Unable to load texture|unknown sound|missing registry|Failed to load' '.\verification' '.\run' '.\build\logs'
```

The pass record must list every expected warning that was reviewed. A successful server start with a `Missing registry key` or `Failed to load model` line is a failure even if the process remains alive.

## 6. Final acceptance checklist

The mod is accepted only when every applicable item below has a recorded artifact and a zero/positive result as specified:

- [ ] `java -version` is Java 21, 64-bit; the build resolves NeoForge 21.1.253 and targets Minecraft 1.21.1.
- [ ] `compileJava` exits 0 with no unresolved API or client-on-server errors.
- [ ] `build` exits 0 and produces the intended `hoghunter-<version>.jar`.
- [ ] `runData` exits 0; generated output is reviewed and no required hand-authored resource is silently missing.
- [ ] `@GameTestHolder("hoghunter")` is present on the test class, the `.nbt` template is at `data/hoghunter/structure/hog_combat_test.nbt`, and `runGameTestServer` exits 0.
- [ ] GameTests prove registry spawn for every final hog entity, AI activation/targeting, outgoing combat damage, incoming damage, and the real block interaction path.
- [ ] The dedicated `runServer --args='--nogui'` smoke script waits for `Done (` rather than sleeping blindly, teleports a real test player, summons every final hog id, verifies entity UUID/type output, verifies a health decrease from hog combat, and exits 0 after a clean `stop`.
- [ ] The dedicated smoke artifacts include timestamped server output, sent commands, parsed assertions, and both process/Gradle exit codes.
- [ ] The JAR contains the verified `neoforge.mods.toml`, classes, entity models/textures, sounds, language data, and GameTest structure at their exact final paths.
- [ ] Server and GameTest logs contain no missing registry, missing model, missing texture, unknown sound, or uncaught exception errors.
- [ ] Client rendering/audio proof is completed on a real usable LWJGL/OpenGL context, or the acceptance record explicitly marks it unproven with the Windows headless limitation stated above and schedules a human desktop launch.
- [ ] A human opens the client and confirms the final horror presentation, because no server-side test can prove the player's visual or audio experience.

The strongest honest completion statement is therefore one of: **server-complete and client-complete**, when every box passes; or **server-complete, client-unverified**, when all automated gates pass but the client could not obtain a usable graphics context. Never label the latter as a fully working release.

## Sources

- [NeoForged 1.21.1 Game Tests](https://docs.neoforged.net/docs/1.21.1/misc/gametest/)
- [NeoForged 1.21.1 Getting Started: building and dedicated-server testing](https://docs.neoforged.net/docs/1.21.1/gettingstarted/)
- [NeoGradle/ModDevGradle run configuration reference](https://docs.neoforged.net/toolchain/docs/plugins/mdg/)
