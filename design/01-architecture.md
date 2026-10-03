# Hog Hunter architecture

This document describes the current source layout and integration contracts. It does not certify
that a build, server, or client has passed. The evidence record is
[`docs/HANDOFF.md`](../docs/HANDOFF.md), and the verification procedure is
[`04-verification.md`](04-verification.md).

The earlier planning documents mixed several mod prototypes, API versions, and entity rosters.
Their original text remains available in Git history at `3b538c5`. Do not copy API examples from
that revision into this Minecraft 1.21.1 implementation.

## Toolchain and packaging

| Component | Repository contract |
|---|---|
| Minecraft | Java Edition 1.21.1 |
| NeoForge | 21.1.253 |
| Java | JDK 21, 64-bit |
| Gradle | 8.12, through the committed wrapper |
| Gradle plugin | `net.neoforged.moddev` 2.0.107 |
| Mod id / Java package | `hoghunter` / `com.hoghunter` |
| Metadata | `src/main/resources/META-INF/neoforge.mods.toml` |
| Artifact name | `build/libs/hoghunter-<mod_version>.jar` |

`gradle.properties` owns the Minecraft, NeoForge, and mod versions. `build.gradle` owns the Java
toolchain, run configurations, source sets, resource expansion, and packaging. `settings.gradle`
owns plugin repositories. This project uses ModDevGradle; the old NeoGradle configuration is not
the build contract.

The runtime resource root is `src/main/resources`. The data run has an output directory at
`src/generated/resources`. There are currently **zero registered datagen providers**: recipes,
loot, tags, and worldgen JSON are committed resources. A successful data bootstrap is distinct
from validating those files. Do not infer that a missing recipe was checked merely because
`runData` exited zero.

Development runs have separate working directories:

| Run | Directory | Purpose |
|---|---|---|
| `runClient` | `run/client` | Client resource loading and interactive presentation checks |
| `runServer` | `run/server` | Dedicated development server |
| `runGameTestServer` | `run/gametest` | Required automated server tests |
| `runData` | `run/data` | Datagen bootstrap |

## Source map

| Location | Responsibility |
|---|---|
| `HogHunterMod.java` | Common entrypoint, deferred register wiring, config, lifecycle listeners |
| `HogHunterBusEvents.java` | Common server lifecycle events |
| `content/HogItems.java` | Items, armor materials, block-item handles |
| `content/HogBlocks.java`, `HogBlockEntities.java` | Block and block-entity registries |
| `content/HogEntities.java` | The seven entity types and dimensions |
| `content/HogSounds.java`, `HogCreativeTabs.java` | Sound events and creative inventory |
| `core/` | Persistent player state and authoritative survival rules |
| `entity/` | Target selection, movement, abilities, cooldowns, entity persistence |
| `item/`, `block/` | Actual use, placement, treatment, combat, traps, progression interactions |
| `worldgen/` | Generation predicates and the Rootmother arena path |
| `net/` | Wire payloads and server-to-client state transport |
| `client/`, `client/model/` | Client mirror, sprint prediction, HUD, audio, renderer and model layers |
| `test/` | GameTests loaded by the dedicated test run |
| `tools/` | Asset/template authoring and static resource verification |

This map lists the ownership boundaries, not a requirement to create a new class for every
feature. The existing small codebase intentionally keeps related behavior together.

## Registration and lifecycle

Registry holders use NeoForge `DeferredRegister` and `DeferredHolder`/`DeferredItem`/
`DeferredBlock`. Attach every register once during the `HogHunterMod` constructor. Dereference
holders in the factory or runtime callback that needs the object, after registration.

Entity attribute suppliers are installed by `HogEntityAttributes` through
`EntityAttributeCreationEvent`. Spawn-placement predicates are installed by `HogSpawnPlacements`
through `RegisterSpawnPlacementsEvent`. An entity also needs a biome spawn entry before the
natural spawning system can select it; placement predicates alone do not add a species to a biome.

Payload registration belongs to `RegisterPayloadHandlersEvent`. Client renderers and layer
definitions belong to client-only subscribers. Ordinary server/player/world events belong to the
game event bus. NeoForge 21.1.253 can infer the appropriate bus for annotated event subscribers;
removing a deprecated explicit bus parameter is not, by itself, a gameplay repair.

The current entity roster is fixed:

`boar_hog`, `spore_hog`, `hook_hog`, `screecher_hog`, `ironback_hog`, `mire_hog`, `rootmother`.

## Authority and persistence

`HogAttachments.PLAYER_DATA` stores `HogHunterPlayerData` on the player. Heart rate, fuel,
injuries, sanity, noise, and progression are decided by the server. The HUD reads a client mirror;
it never awards evidence, spends inventory, changes health, or authorizes an ability.

Serialization must retain defaults for missing fields and clamp loaded values. Permanent
progression and temporary expedition state have different death behavior; inspect the respawn
handler when changing that policy. Attachment `copyOnDeath` alone is not a complete respawn
policy. Inventory death drops continue to follow vanilla game rules, including `keepInventory`.

Entity save overrides must call their superclass. Dropping those calls loses vanilla health,
attributes, effects, equipment, and other mob state even if a custom boolean is saved correctly.
Ability cooldowns and one-time boss phase state must not reset into an immediate attack or a
fresh boss phase merely because a chunk was reloaded.

Block entities store their own persistent cooldowns and interaction state. A tick callback must
verify the expected block-entity type, run on the server, and avoid spawning into solid blocks or
creating an unbounded population. Boss availability is a world concern; player progression alone
cannot guarantee a single live boss.

## Networking and physical sides

`HogPayloads.SyncPlayerState` carries the authoritative state needed by the HUD. Send it on player
lifecycle changes and at a bounded refresh interval. A data attachment without a registered sync
handler does not become a client mirror simply by calling `player.syncData(...)`.

Use vanilla item/block interaction packets for item actions where possible. A custom packet must
not accept client-supplied damage, fuel, evidence, or progression totals. Server handlers must
validate the sender and schedule world mutations on the server thread.

Common classes must load on a dedicated server without resolving `net.minecraft.client.*`.
Renderer, `Minecraft`, input, and GUI dependencies stay in the `client` package behind a physical
client entrypoint. Wire records use common types only. A successful client startup does not prove
this boundary; a dedicated-server launch is a separate check.

`HogClientInput` mirrors server sprint locks before local movement prediction. It temporarily
suppresses the sprint key and double-tap transition for that tick, then restores the key state;
normal movement input still refreshes before travel. The server remains authoritative. Keep the
hold/toggle preference and ordinary forward/strafe speed intact when changing this path.

## Server data paths

Minecraft 1.21.1 uses these exact directories:

```text
data/hoghunter/recipe/
data/hoghunter/loot_table/blocks/
data/hoghunter/loot_table/entities/
data/hoghunter/tags/item/
data/hoghunter/tags/block/
data/hoghunter/tags/entity_type/
data/hoghunter/worldgen/configured_feature/
data/hoghunter/worldgen/placed_feature/
data/hoghunter/neoforge/biome_modifier/
data/hoghunter/structure/
```

Vanilla tool tags belong in `data/minecraft/tags/block/`, including `mineable/pickaxe`. A custom
namespace copy of a vanilla tool tag does not make a pickaxe recognize a block. Crafting results
use `{"id": "hoghunter:item_name", "count": 1}`. Java aliases such as `HUNTER_COAT_HELMET`
do not create additional registry names: the registered armor ids remain `hoghide_*`.

## Client assets

Runtime living-entity geometry is Java code in `client/model/HogModel.java`, baked through
registered model layers and rendered by `HogRenderer`. The review files under
`tools/assets/entity_models/` are not Minecraft model resources. Entity skins live at
`assets/hoghunter/textures/entity/hog/<entity_id>.png`.

An ordinary entity texture does not automatically animate from `.png.mcmeta`. If a vertical
frame strip is used, the renderer must explicitly sample one frame with UVs for the logical skin
size. GUI textures also require explicit animation code when animated. The asset contract and
generator commands are in [`03-assets.md`](03-assets.md).

## Configuration

`HogHunterConfig` registers a NeoForge server config. World-specific server configuration is
managed by NeoForge in the world's `serverconfig` directory. The validated settings cover enemy
health/damage/speed, spawn pressure, oil drain, heartbeat stress, and horror
presentation. A changed multiplier must not silently restore health, refill fuel, or reset
progression. Visual/audio intensity controls affect presentation rather than authorizing damage.

## Primary references

- [NeoForge 1.21.1 registries](https://docs.neoforged.net/docs/1.21.1/concepts/registries/)
- [NeoForge 1.21.1 event buses](https://docs.neoforged.net/docs/1.21.1/concepts/events/)
- [NeoForge 1.21.1 payloads](https://docs.neoforged.net/docs/1.21.1/networking/payload/)
- [NeoForge 1.21.1 data attachments](https://docs.neoforged.net/docs/1.21.1/datastorage/attachments/)
- [ModDevGradle run configuration](https://docs.neoforged.net/toolchain/docs/plugins/mdg/)
