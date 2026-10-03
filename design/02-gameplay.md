# Hog Hunter — Gameplay & Horror Design

**Target:** Minecraft Java 1.21.1, NeoForge 21.1.253, Java 21  
**Mod id:** `hoghunter`  
**Base package:** `com.hoghunter`

This document defines the playable single-player loop and the data that the implementation must expose. It assumes vanilla survival movement, mining, crafting, containers, and world saving remain available. Hog Hunter adds a pressure layer around descending, hunting, extracting evidence, and escaping.

## 1. Core loop

The player begins at a surface hunter camp at Y=64. A mine entrance leads into a generated vertical mine below Y=48. Each expedition is a run of five depth bands:

| Band | Y range | Purpose | Required progression |
|---|---:|---|---|
| Surface camp | 64 and above | Craft, heal, store trophies | None |
| Shallow workings | 48 to 16 | Learn tracking and conserve oil | Bolt gun, lantern |
| Flooded galleries | 15 to -16 | Water, ambushes, harpoon traversal | Harpoon blueprint |
| Blackstone seam | -17 to -48 | Fractures and pack hunts | Reinforced armor, snare |
| The Root | -49 to -64 | Boss arena and extraction | Three marked tusks |

The session loop is: prepare at camp → enter with a finite loadout → read spoor and sound cues → locate and kill hogs → collect `hoghunter:corrupted_tissue`, tusks, and depth evidence → satisfy the current depth objective → return to the entrance or descend → spend trophies on permanent unlocks. Death drops the expedition inventory at the death position; permanent blueprint unlocks and discovered depth records remain.

Every mine level has one objective from `data/hoghunter/hog_objectives/*.json`, one safe-room candidate, and one extraction route. The objective must be completed before the descent gate opens. The gate is a locked `hoghunter:depth_gate` block whose server-side interaction checks the player's persistent progression flag; the client never decides whether a gate is open.

The default run target is 25–35 minutes. A successful kill is not enough: the player must choose when to stop hunting because carrying more trophies increases risk and makes the return trip harder.

## 2. Authoritative player survival state

Store the following server-authoritative values in a `HogHunterData` attachment on `Player`, registered through `RegisterAttachmentsEvent` as `HoghunterAttachments.PLAYER_DATA`. Synchronize changed values with a play-to-client payload from `RegisterPayloadHandlersEvent` on `PlayPayloadHandler` channel `hoghunter:state_sync`; do not use client-only fields as gameplay authority.

| Field | Range / default | Meaning |
|---|---:|---|
| `heartRate` | 45–180 BPM, default 60 | Current fear/physical load. Recomputed every 10 ticks. |
| `oil` | 0–100, default 100 | Lantern fuel percentage. One point is 1/100 of a full tank. |
| `reserveOil` | integer, default 0 | Whole oil-can charges. One oil can adds 50 points, capped at 100. |
| `ammo` | integer, default 12 | Loaded bolt cartridges, not inventory bolts. |
| `wounds` | 0–3, default 0 | Bleeding injuries. Each wound adds +8 BPM and reduces max health by 1 heart until treated. |
| `fracture` | 0–2, default 0 | Broken limb severity. Each level reduces movement speed by 10%; level 2 also disables sprinting. |
| `sanity` | 0–100, default 100 | Hidden server value used to authorize hallucinations and audio events. |
| `noise` | 0–100, default 0 | Decays by 4 per second; mining, sprinting, and weapons add noise. |
| `evidence` | integer | Expedition-only proof carried in the inventory; extraction banks it at camp. |

### Heartbeat model

Every 10 ticks, the server calculates a target BPM:

`target = clamp(60 + lightStress + proximityStress + woundStress + sprintStress + sanityStress, 45, 180)`

The current value moves 4 BPM toward `target` per second. Components are:

| Source | Contribution |
|---|---:|
| Lantern brightness below 8 light | +10 |
| Lantern oil at 0 | +20 |
| Any hostile hog within 12 blocks and line of sight | +15 |
| Any hostile hog within 6 blocks, line of sight or not | +25 |
| `It Hunts You` stalk active | +20 |
| Sprinting | +8 |
| Each wound | +8 |
| Fracture level 2 | +6 |
| Sanity below 35 | +10 |
| Sanity below 15 | +15 |
| Standing in a safe room | -20, minimum 45 |

Threshold effects are evaluated on the server and sent to the client as state flags:

| BPM | Effect |
|---:|---|
| 90–119 | Audible heartbeat every 1.5 seconds; no mechanical penalty. |
| 120–139 | FOV pulses ±2 degrees; mining speed -10%; breathing sound. |
| 140–159 | Sprint drains 1 extra hunger point per 2 seconds; accuracy cone +15%; heartbeat every 0.8 seconds. |
| 160–179 | 5% chance per second of a 0.4-second stagger; screen vignette; no new stalk can begin. |
| 180 | Panic lock for 3 seconds: cannot sprint, accuracy cone +35%, emits 30 noise; heart rate then falls toward target. |

Light is functional survival equipment. `HogHunterLightSource` is the lantern's server-owned light emission, and the held lantern must provide a radius of 8 light at oil > 0, radius 4 at oil 1–5, and no light at oil 0. Drain is 1 oil point per 12 seconds while held or placed and lit. A player may extinguish it with the use action; extinguishing stops drain and removes the light-stress contribution after 20 ticks.

### Damage, wounds, fractures, and treatment

Incoming damage uses vanilla armor first, then the mod adds a state injury only when the final damage event is large enough. Handle `LivingIncomingDamageEvent` on the server and never apply injury from client animation.

| Trigger | Result |
|---|---|
| Final hit damage ≥ 4 hearts | Add 1 wound if wounds < 3. |
| Hog charge or boss tusk hit ≥ 6 hearts | Add 1 fracture if fracture < 2; knockback 0.8. |
| Fall distance ≥ 6 blocks while fracture < 2 | Add 1 fracture. |
| Wound count > 0 | Lose 0.5 heart every 12 seconds while not bandaged; cap at 1 heart remaining. |
| Death | Drop expedition inventory and reset temporary state after respawn. |

`hoghunter:bandage` removes one wound after a 3-second use action; `hoghunter:splint` removes one fracture after a 5-second use action. Damage, sprinting, or starting another use cancels treatment. A wound can be treated only when the player has at least 4 hearts, and a fracture can be treated only while standing still. A medkit removes one wound and one fracture but applies Weakness for 10 seconds.

## 3. Corrupted hog roster

All entities are server-side `Monster` subclasses under `com.hoghunter.entity`. Register each with `HogEntities.register(modBus, "<id>")` in a `DeferredRegister<EntityType<?>>` using `EntityType.Builder.of(<EntityClass>::new, MobCategory.MONSTER).sized(width, height).clientTrackingRange(range).build(new ResourceLocation("hoghunter", "<id>"))`. Exact entity IDs, physical sizes, and combat values are below.

| ID / name | Role and silhouette | Health / armor | Damage | Speed | Spawn trigger | Special ability |
|---|---|---:|---:|---:|---|---|
| `boar_hog` / Burrower | Low 1.1×1.0 m body, oversized shovel snout; baseline hunter | 24 HP / 2 armor | 4 HP bite | 0.27 | Light level ≤7, Y≤48, player noise ≥25; group size 1–3 | **Headlong:** after 20 ticks of line-of-sight, charge 8 blocks; impact deals +2 HP and 0.6 knockback. Cooldown 8 s. |
| `spore_hog` / Lanternback | 1.2×1.3 m hump with glowing fungal plates; area denial | 30 HP / 3 armor | 3 HP gore | 0.22 | Y≤16, air block has `hoghunter:spore_air`, light ≤8 | **Spore cloud:** emits a 4-block cloud for 6 s; players inside gain +12 BPM and Blindness I for 1 s every 2 s. Cooldown 14 s. |
| `hook_hog` / Chainjaw | Tall 1.0×1.6 m head, four chain-like tusks; ranged puller | 20 HP / 1 armor | 2 HP hit + pull | 0.25 | Flooded galleries, player within 14 blocks, line of sight | **Grapple:** launches a 12-block hook; on hit pulls player 5 blocks and adds 20 noise. Cooldown 6 s. |
| `screecher_hog` / Squealer | Thin 0.9×1.5 m body and split jaw; alarm support | 18 HP / 0 armor | 2 HP bite | 0.31 | After any hog dies within 24 blocks, 30% chance; never natural-spawns alone | **Screech:** 10-block radius pulse adds 25 BPM, summons one `boar_hog` if local hostile count <5, and disables sprint for 2 s. Cooldown 18 s. |
| `ironback_hog` / Ironback | Broad 1.4×1.2 m plated back; slow armored bruiser | 48 HP / 8 armor | 6 HP tusk | 0.19 | Y≤-17, blackstone seam, light ≤5 | **Plated turn:** front damage is reduced by 60% while its back is toward the player; turning takes 12 ticks. Knockback resistance 0.8. |
| `mire_hog` / Mire Hog | 1.3×1.0 m half-submerged body with dripping legs; ambusher | 34 HP / 2 armor | 5 HP bite | 0.24 | Waterlogged block or mud, Y≤-17, player crouching or heartRate ≥120 | **Mire vanish:** becomes invisible for 4 s and moves through water at 0.38 speed; reappears behind the nearest player within 8 blocks. Cooldown 16 s. |
| `rootmother` / Rootmother | Boss: 2.8×2.6 m boar skull, root antlers, six legs | 260 HP / 10 armor | 8 HP tusk; 4 HP shockwave | 0.16 | One per world at `hoghunter:rootmother_arena`, after three tusks are offered | **Root call:** every 25 s summons two `boar_hog` and one `spore_hog`; **Tusk sweep:** 120° melee arc every 7 s; **Blackout:** at 50% HP, extinguishes lanterns in 16 blocks for 8 s and opens two root vents. |

For natural spawning, use `SpawnPlacementRegisterEvent` with `SpawnPlacements.Type.ON_GROUND`, `Heightmap.Types.MOTION_BLOCKING_NO_LEAVES`, and a shared `HogSpawnRules` predicate. The predicate checks difficulty, Y band, light, local hostile count, and the trigger column above; it must reject peaceful difficulty. Boss spawning is a scripted server event, never a natural spawn.

Each entity must expose a distinct `HogEntity` animation state (`idle`, `sniff`, `charge`, `hurt`, `death`) to the client. Their renderers and model layers are registered on `EntityRenderersEvent.RegisterRenderers` and `EntityRenderersEvent.RegisterLayerDefinitions`; no client class is referenced from common registration code.

## 4. Hunter toolkit

Items live under `com.hoghunter.item`; register them with `HogItems.ITEMS = DeferredRegister.create(Registries.ITEM, "hoghunter")`. Weapon use and durability are server-authoritative. The recipes below are shaped or shapeless JSON files in `data/hoghunter/recipes/` and use vanilla tags where possible.

| Item | Combat values | Cooldown / durability | Recipe ingredients | Special mechanic |
|---|---|---:|---|---|
| `bolt_gun` | 9 HP per bolt; ignores 2 armor; 24-block effective range | 20 ticks; 128 durability | 3 iron ingots, 2 sticks, 1 string, 1 copper ingot | Holding sneak for 10 ticks steadies aim: +25% damage, but movement speed -70% while held. Bolt impact adds 10 noise. |
| `silver_bolt_gun` | 14 HP per bolt; ignores 5 armor; 28-block range | 24 ticks; 192 durability | `bolt_gun`, 2 `hoghunter:purified_tusk`, 2 iron ingots, 1 amethyst shard | Charged shot (30 ticks) pins a non-boss hog for 2 s; charged shot consumes 2 ammo. |
| `baited_snare` | 12 HP when triggered; 4-block trigger radius | Place cooldown 10 ticks; 16 uses | 4 string, 2 iron nuggets, 1 leather, 1 rotten flesh | Place on a full block; hogs path toward bait within 10 blocks. Trigger roots the hog for 5 s and creates 40 noise. Cannot root Rootmother. |
| `field_lantern` | No damage; light radius 8 | Toggle 10 ticks; 256 durability; oil-powered | 4 iron nuggets, 1 glass pane, 1 copper ingot, 1 torch | Placeable or held. While held, reveals `hoghunter:spoor` particles within 8 blocks and makes Mire Hog visible. Can be thrown 8 blocks; thrown lantern emits radius 6 and breaks on impact. |
| `mine_harpoon` | 16 HP on direct hit; 10 HP on pull impact | 30 ticks; 96 durability | 2 iron ingots, 1 chain, 1 tripwire hook, 1 stick | Hit a hog to pull it 6 blocks toward the player; hit a tagged anchor to pull the player 10 blocks. Adds 20 noise and cannot fire through blocks. |
| `salt_shaker` | 3 HP to hogs; 0 HP to others | 15 ticks; 32 uses | 1 paper, 2 salt (`#hoghunter:salt`), 1 glass bottle | Creates a 3-block salt line for 12 s. Hogs crossing it are slowed 40% and cannot use special abilities for 3 s. |

Ammo is `hoghunter:iron_bolt`, stack size 32. Bolt gun shots consume one loaded `ammo` and require one bolt item only when reloading; reload takes 30 ticks and fills up to 6 shots. The player cannot reload while sprinting, falling, or in a panic lock. `silver_bolt_gun` uses the same ammo and its charged shot consumes two.

### Consumables and armor

| Item | Recipe / stack | Use effect |
|---|---|---|
| `oil_can` | 1 iron ingot + 2 coal + 1 glass bottle; stack 4 | Adds 50 oil to `reserveOil`; using it on a lit lantern refills 50 oil immediately. |
| `bandage` | 3 wool + 1 string; stack 8 | 3-second use; removes one wound. |
| `splint` | 2 sticks + 1 string; stack 4 | 5-second use; removes one fracture. |
| `field_medkit` | 1 bandage + 1 golden carrot + 1 honey bottle; stack 1 | 4-second use; restores 4 HP, removes one wound and one fracture, applies Weakness 10 s. |
| `salt_ration` | 1 bread + 1 salt; stack 16 | Restores 3 hunger and reduces heartRate by 5 over 5 s. |

`hunter_coat` is a four-slot armor set. Helmet: 2 leather + 1 iron ingot; chestplate: 8 leather + 2 iron ingots; leggings: 7 leather + 2 iron ingots; boots: 4 leather + 1 iron ingot. Armor values are 1/3/2/1, toughness 0, durability 165/240/225/195. The complete set grants 15% reduction to fall fracture chance, but no protection from direct damage. `ironback_harness` is a chestplate upgrade made from `hunter_coat` chestplate + 4 iron plates; it gives 5 armor and 0.1 knockback resistance but makes sprint noise +10.

## 5. Progression and depth gates

Progression is stored in `HogHunterData.unlockedTier` and awarded only when the player extracts the named evidence at camp. Evidence cannot be awarded by a client packet or by placing an item in a container.

| Tier / depth | Unlock condition | Unlocks |
|---|---|---|
| 0 / camp | Start | Field lantern, bolt gun, iron bolts, bandage, shallow gate. |
| 1 / Y≤16 | Extract 3 corrupted tissue from Shallow workings | Baited snare recipe, oil can recipe, Flooded galleries gate. |
| 2 / Y≤-16 | Extract one hook tooth and one spore sac | Mine harpoon recipe, salt shaker recipe, silver bolt gun blueprint. |
| 3 / Y≤-32 | Extract one iron plate and survive with at least one fracture treated | Hunter coat and ironback harness, The Root gate. |
| 4 / Y≤-49 | Offer three marked tusks at the Root altar | Rootmother arena opens; boss loot table becomes active. |
| 5 / boss cleared | Kill Rootmother and extract the black heart | Permanent camp fast travel to deepest cleared band; `hoghunter:root_heart` trophy. |

The gate checks are deliberately evidence-based. A player may reach a lower Y coordinate through an accidental cave, but the mine generator continues to produce higher-tier hazards only after the corresponding gate flag is set. This prevents a lucky early tunnel from skipping the intended tool economy.

## 6. Horror presentation and stalk system

### Ambience triggers

The client receives typed ambience events from the server, rather than polling proximity every frame. `HogAmbienceManager` selects a sound event from `assets/hoghunter/sounds.json` and plays it with `SoundSource.AMBIENT` at the supplied position. Events are rate-limited per player.

| Trigger | Event | Limit |
|---|---|---:|
| No hostile visible for 45 s below Y=16 | Distant wet hoof scrape 18–32 blocks away | Once / 35 s |
| Player mines a block tagged `#hoghunter/vein_blocks` | Tunnel groan and dust burst | Once / 12 s |
| Oil drops below 10 | Breath behind player, no entity | Once / expedition |
| HeartRate ≥140 for 8 s | Layered heartbeat and low-frequency rumble | Once / 20 s |
| A hog dies | Distant answering squeal with 35% chance | Once / 10 s |
| Rootmother below 75% HP | Arena root creak | Once / phase |

Sound event IDs use the exact namespace `hoghunter:<event>`, with files under `assets/hoghunter/sounds/` and definitions in `assets/hoghunter/sounds.json`. The client must use a reduced-volume path when the player has the `options.dark` accessibility preference unavailable; the config `horrorAudioIntensity` still scales volume from 0.0 to 1.0.

### “It Hunts You” stalk behavior

The stalk is a timed director state, not a hidden permanently spawned monster. `HogStalkDirector` runs on the server every 20 ticks and has states `DORMANT`, `WATCHING`, `APPROACHING`, `REVEALED`, and `COOLDOWN`.

It may start when all conditions hold: player is below Y=16; no hostile hog is within 16 blocks; `heartRate < 160`; `sanity ≤ 70` or oil ≤ 25; and at least 90 seconds have elapsed since the previous stalk. The director selects a valid spawn node 24–40 blocks away with two solid blocks of headroom and no direct line of sight.

In `WATCHING`, the stalker is a non-colliding client presentation silhouette for 2–4 seconds, visible only at the edge of the player's camera. In `APPROACHING`, the server spawns one `boar_hog` with `stalkSpawn=true` at the selected node after a 1–3 second randomized delay. It receives speed 0.34, 40 HP, and a rule to break line of sight after 6 seconds if the player turns toward it. If it reaches 6 blocks, it becomes a normal Burrower and enters `REVEALED`; the player can then fight it. If it despawns after being seen, the cooldown is 75 seconds. The director never creates a second stalker while one is alive.

### Sanity and hallucinations

Sanity is not a death meter. It changes the reliability of what the player sees and hears:

| Sanity | Effect |
|---:|---|
| 70–100 | No hallucinations. |
| 40–69 | 1% per second chance of a false hoofstep; no gameplay collision. |
| 20–39 | 2% per second chance of a client-only `hallucination_hog`; distorted audio pitch ±8%; false spoor particles. |
| 1–19 | 4% per second chance of a client-only hallucination; 10% vignette opacity; inventory item names may flicker visually for ≤1 s. |
| 0 | 3-second blackout and forced heartRate +20, then sanity resets to 15. |

Hallucinations are explicitly client-only entities or particles and cannot damage, block, drop items, trigger advancement criteria, or alter pathfinding. Sanity decreases by 1 per 10 seconds below Y=16 while oil ≤10, by 5 when a screecher pulse hits, and by 8 when the player sees Rootmother. It recovers by 2 per 10 seconds in a lit safe room and by 10 on sleeping at camp.

Screen effects are implemented as client overlays registered through `RegisterGuiLayersEvent`, with alpha derived from the synchronized state. Distorted audio is implemented by selecting alternate sound events, not by changing global volume. The mod must respect the vanilla `SoundSource` volume slider and provide a `horrorVisualIntensity` config from 0.0 to 1.0.

### Death, scare, and authored failure

When a player reaches 0 HP, the server emits `hoghunter:death_sting`, records the killer entity ID, and lets normal Minecraft death handling run. The client plays a 1.2-second authored failure sequence: lantern flicker, 6-frame silhouette, low-frequency hit, then the vanilla death screen. No forced jump scare uses a full-screen flash above 0.2 seconds; the flash is capped at 0.15 alpha and is disabled when `horrorVisualIntensity=0`.

Three authored scare encounters are placed in mine templates:

1. A blocked tunnel has fresh spoor and a lantern that extinguishes when approached; the stalk director starts only after the player crosses the exit marker.
2. A safe room contains a harmless carcass. Interacting three times makes it emit a screech and removes the room's safe status for 30 seconds, teaching the player that safety is temporary.
3. The Root altar presents the three tusk sockets. The third socket opens the arena, seals the exit for 45 seconds, and gives the player a clear audio cue before Rootmother spawns.

These scares are authored in structure markers and server events. They never rely on an invisible damage event, a fake item loss, or a client packet that changes authoritative state.

## 7. Difficulty configuration

Expose a server-side TOML config at `config/hoghunter-server.toml` through `ModConfig.Type.SERVER` and `ModConfigEvent.Reloading`. Values are multipliers or bounded settings; all server gameplay reads a validated snapshot.

| Key | Default | Allowed range | Applied to |
|---|---:|---:|---|
| `enemyHealthMultiplier` | 1.0 | 0.5–3.0 | All non-boss hog max health; Rootmother uses the same multiplier. |
| `enemyDamageMultiplier` | 1.0 | 0.5–2.5 | Melee, ranged, and ability damage. |
| `enemySpeedMultiplier` | 1.0 | 0.75–1.35 | Movement speed after attribute setup. |
| `spawnDensityMultiplier` | 1.0 | 0.25–2.0 | Per-band hostile cap and spawn roll, never boss count. |
| `oilDrainMultiplier` | 1.0 | 0.25–3.0 | Lantern drain interval inverse. |
| `heartbeatStressMultiplier` | 1.0 | 0.0–2.0 | Positive BPM contributions only; safe-room reduction is unchanged. |
| `stalkCooldownSeconds` | 90 | 30–300 | Minimum time between stalk starts. |
| `stalkEnabled` | true | boolean | Enables the director. |
| `sanityEffectsEnabled` | true | boolean | Enables hallucination and distortion presentation; sanity still tracks if false. |
| `horrorVisualIntensity` | 1.0 | 0.0–1.0 | Client visual overlay alpha, synced from client config when available. |
| `horrorAudioIntensity` | 1.0 | 0.0–1.0 | Client horror event volume multiplier. |

The config must be clamped on load and logged with the final values. Changing difficulty does not retroactively heal, refill oil, despawn hogs, or reroll progression. A server operator can tune pressure without changing the authored progression or the boss's attack phases.

## 8. Required data and implementation map

The implementation team should create the following later; this document intentionally does not create them:

| Concern | Planned location / API |
|---|---|
| Common bootstrap | `com.hoghunter.HogHunter` with `IEventBus modBus`; register `HogItems.ITEMS`, `HogEntities.ENTITIES`, attachments, payloads, and config. |
| Registries | `com.hoghunter.registry.HogItems`, `HogEntities`, `HogBlocks`, `HogSounds`; `DeferredRegister.create(Registries.ITEM, "hoghunter")` and matching registry keys. |
| Entity attributes | `EntityAttributeCreationEvent`; `event.put(HogEntities.BOAR_HOG.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 24.0).add(Attributes.MOVEMENT_SPEED, 0.27).add(Attributes.ATTACK_DAMAGE, 4.0).build())`, with per-entity exact table values. |
| Spawn rules | `SpawnPlacementRegisterEvent` plus `RegisterSpawnPlacementsEvent` equivalent for the installed NeoForge mappings; verify the exact event name against the 21.1.253 API before coding. |
| Items and recipes | `src/main/java/com/hoghunter/item/`; `src/main/resources/data/hoghunter/recipes/*.json`; item models at `src/main/resources/assets/hoghunter/models/item/*.json`; language at `assets/hoghunter/lang/en_us.json`. |
| Entity data | Tags at `data/hoghunter/tags/entity_types/` and loot at `data/hoghunter/loot_tables/entities/`; model textures at `assets/hoghunter/textures/entity/`. |
| Sounds | `assets/hoghunter/sounds.json` and `assets/hoghunter/sounds/*.ogg`; register `SoundEvent` instances in `HogSounds`. |
| Mine content | Structures under `data/hoghunter/structure/`, templates under `data/hoghunter/structures/`, and configured/template placements registered through the 1.21.1 worldgen APIs. |
| Client hooks | `com.hoghunter.client.HogHunterClient`; register entity renderers, GUI layers, key mappings, and client payload handlers only from the client setup event. |

All gameplay numbers in this document are initial balance values. They belong in named constants or server config, with boss phase constants kept separate from global multipliers so that balance changes do not alter save compatibility.
