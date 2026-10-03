# Hog Hunter technical architecture

Target: Minecraft Java 1.21.1, NeoForge 21.1.253, Java 21, mod id `hoghunter`, base package `com.hoghunter`.

This document is the implementation contract for the first single-player release. The server owns game state. The client owns rendering, sounds that are purely presentational, screens, and input. A client must never be allowed to decide whether a hog was hit, whether sanity changed, or whether a drop was awarded.

## Project shape

The project is a single Gradle project. The source layout will be:

```text
hoghunter/
  settings.gradle
  build.gradle
  gradle.properties
  gradle/wrapper/gradle-wrapper.jar
  gradle/wrapper/gradle-wrapper.properties
  gradlew
  gradlew.bat
  src/main/java/com/hoghunter/
    HogHunter.java
    core/
      ModConstants.java
      ModRegistries.java
      ModEvents.java
      ModConfig.java
    content/
      HogItems.java
      HogBlocks.java
      HogEntities.java
      HogBlockEntities.java
      HogSounds.java
      HogCreativeTabs.java
      HogAttachments.java
    worldgen/
      HogBiomeModifiers.java
      HogPlacedFeatures.java
      HogConfiguredFeatures.java
      HogStructureTypes.java
    entity/
      HogEntity.java
      BoarHogEntity.java
      BruteHogEntity.java
      HogAttackGoal.java
      HogTargeting.java
      HogAttributes.java
      HogSpawnRules.java
    block/
      CorruptedOreBlock.java
      HogNestBlock.java
      HogNestBlockEntity.java
    item/
      HuntingKnifeItem.java
      HogHeartItem.java
      SanityLanternItem.java
    client/
      HogClient.java
      HogEntityRenderers.java
      HogBlockEntityRenderers.java
      HogScreens.java
      HogKeyMappings.java
      HogClientAudio.java
    net/
      HogPayloads.java
      SyncPlayerHogStatePayload.java
      HogServerPayloadHandlers.java
      HogClientPayloadHandlers.java
    data/
      HogDataGenerators.java
      HogBlockStateProvider.java
      HogItemModelProvider.java
      HogLanguageProvider.java
      HogRecipeProvider.java
      HogLootTableProvider.java
      HogEntityTagProvider.java
      HogBiomeModifierProvider.java
      HogDataMapProvider.java
```

`core` contains wiring and shared rules; it must not contain client-only imports. `content` contains registry holders. `entity`, `block`, and `item` contain game behavior. `worldgen` contains server/data registration. `client` is loaded only on the physical client. `net` contains payload definitions that are safe to load on both sides plus separate side handlers. `data` is datagen-only and must not be referenced by runtime classes.

## Gradle files

The four required project files are `settings.gradle`, `build.gradle`, `gradle.properties`, and the wrapper files under `gradle/wrapper`. The wrapper must use Gradle 8.12 and its `distributionUrl` must point at the Gradle 8.12 binary distribution. Use the already downloaded executable at `C:\Users\Windows\Downloads\Hcubed_The_Mercer_Contract\work\gradle-8.12\bin\gradle.bat` while generating the wrapper; do not change the Minecraft or NeoForge target.

`settings.gradle`:

```groovy
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { url = 'https://maven.neoforged.net/releases' }
    }
}

plugins {
    id 'org.gradle.toolchains.foojay-resolver-convention' version '1.0.0'
}

rootProject.name = 'hoghunter'
```

`build.gradle` must use the NeoGradle userdev plugin. The exact target plugin line for the 1.21.1 NeoForge MDK is:

```groovy
plugins {
    id 'java-library'
    id 'maven-publish'
    id 'net.neoforged.gradle.userdev' version '7.1.38'
}

version = mod_version
group = mod_group_id

base {
    archivesName = mod_id
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

sourceSets.main.resources {
    srcDir 'src/generated/resources'
}

repositories {
    // NeoForge is resolved from the plugin's configured NeoForge Maven.
    // Add this only if a later dependency requires it:
    // maven { url = 'https://maven.neoforged.net/releases' }
    // maven { url = 'https://maven.minecraftforge.net' }
}

runs {
    configureEach {
        workingDirectory project.layout.projectDirectory.dir('run').dir(name)
        systemProperty 'forge.logging.markers', 'REGISTRIES'
        systemProperty 'forge.logging.console.level', 'debug'
        modSource project.sourceSets.main
    }

    client {
        systemProperty 'neoforge.enabledGameTestNamespaces', project.mod_id
    }

    server {
        systemProperty 'neoforge.enabledGameTestNamespaces', project.mod_id
        argument '--nogui'
    }

    data {
        arguments.addAll '--mod', project.mod_id,
                '--all',
                '--output', file('src/generated/resources/').getAbsolutePath(),
                '--existing', file('src/main/resources/').getAbsolutePath()
    }
}

configurations {
    runtimeClasspath.extendsFrom localRuntime
}

dependencies {
    implementation "net.neoforged:neoforge:${neo_version}"
}

tasks.withType(ProcessResources).configureEach {
    def replaceProperties = [
            minecraft_version      : minecraft_version,
            minecraft_version_range: minecraft_version_range,
            neo_version            : neo_version,
            loader_version_range   : loader_version_range,
            mod_id                 : mod_id,
            mod_name               : mod_name,
            mod_license            : mod_license,
            mod_version            : mod_version,
    ]
    inputs.properties replaceProperties
    filesMatching(['META-INF/neoforge.mods.toml']) {
        expand replaceProperties
    }
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}
```

Do not use the old ForgeGradle plugin, `net.minecraftforge.gradle`, or a Forge `mods.toml` contract. This target uses NeoForge's `META-INF/neoforge.mods.toml`. `maven.minecraftforge.net` is not needed for the base project and must remain absent unless a deliberately added dependency requires it.

`gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx1G
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true

minecraft_version=1.21.1
minecraft_version_range=[1.21.1]
neo_version=21.1.253
loader_version_range=[1,)

mod_id=hoghunter
mod_name=Hog Hunter
mod_license=All Rights Reserved
mod_version=0.1.0
mod_group_id=com.hoghunter
```

The wrapper files are generated with Gradle 8.12. `gradle/wrapper/gradle-wrapper.properties` must contain `distributionUrl=https\\://services.gradle.org/distributions/gradle-8.12-bin.zip`. The wrapper jar, `gradlew`, and `gradlew.bat` are generated artifacts; they are not hand-authored source.

## Runtime resources and exact paths

Runtime resources use `src/main/resources` and generated data uses `src/generated/resources`. The mod metadata path is `src/main/resources/META-INF/neoforge.mods.toml`. Examples of required namespaced paths are:

```text
assets/hoghunter/lang/en_us.json
assets/hoghunter/models/item/hunting_knife.json
assets/hoghunter/models/block/corrupted_ore.json
assets/hoghunter/blockstates/corrupted_ore.json
assets/hoghunter/textures/item/hunting_knife.png
assets/hoghunter/textures/entity/boar_hog.png
assets/hoghunter/sounds.json
data/hoghunter/loot_table/blocks/corrupted_ore.json
data/hoghunter/loot_table/entities/boar_hog.json
data/hoghunter/tags/entity_types/hog_hunters_prey.json
data/hoghunter/tags/blocks/hog_nest_replaceable.json
data/hoghunter/neoforge/biome_modifier/spawn_boar_hog.json
data/hoghunter/recipes/hog_heart_salve.json
```

In 1.21.1, `data/<namespace>/loot_table/...` is singular `loot_table`, while tags use `tags`. A resource path is lowercase and must match the registry path exactly.

## Mod entrypoint and lifecycle

`com.hoghunter.HogHunter` is the sole common entrypoint:

```java
@Mod(HogHunter.MOD_ID)
public final class HogHunter {
    public static final String MOD_ID = "hoghunter";

    public HogHunter(IEventBus modBus, ModContainer modContainer) {
        ModRegistries.register(modBus);
        HogAttributes.register(modBus);
        HogPayloads.register(modBus);
        ModConfig.register(modContainer);

        modBus.addListener(HogHunter::commonSetup);
        NeoForge.EVENT_BUS.register(new ModEvents());
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(HogSpawnRules::registerSpawnPlacement);
    }
}
```

The constructor receives the mod event bus and `ModContainer`; use `modContainer.registerConfig(ModConfig.Type.COMMON, ModConfig.SPEC)`. Do not use the removed 1.20-era `ModLoadingContext.get().registerConfig(...)` pattern for this design.

Lifecycle order is: mod constructors; static `@EventBusSubscriber` registration; registry events; common setup; physical-side setup; inter-mod communication; load complete. Deferred registers must be attached during the constructor before registry events fire. Registry objects must be referenced through `DeferredHolder`/`DeferredBlock` and only dereferenced after registration. Server gameplay listeners belong on `NeoForge.EVENT_BUS`; registry and payload registration belong on the mod bus. Parallel lifecycle events may require `event.enqueueWork` before touching game-thread state.

## Registry holders

`ModRegistries.register(modBus)` calls `.register(modBus)` once for every register below. Each register is a `static final` field initialized before the mod constructor runs. Use `BuiltInRegistries` for vanilla registries and `NeoForgeRegistries.Keys` for NeoForge registries. Do not use `ForgeRegistries`; that class is the old Forge namespace. The method is deliberately explicit:

```java
public static void register(IEventBus modBus) {
    HogItems.ITEMS.register(modBus);
    HogBlocks.BLOCKS.register(modBus);
    HogBlockEntities.BLOCK_ENTITY_TYPES.register(modBus);
    HogEntities.ENTITY_TYPES.register(modBus);
    HogSounds.SOUND_EVENTS.register(modBus);
    HogCreativeTabs.CREATIVE_MODE_TABS.register(modBus);
    HogAttachments.ATTACHMENT_TYPES.register(modBus);
}
```

### Items, blocks, block entities, entities, sounds, and tabs

```java
public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(HogHunter.MOD_ID);

public static final DeferredItem<Item> HOG_HEART = ITEMS.registerItem(
        "hog_heart", Item::new, new Item.Properties());

public static final DeferredRegister.Blocks BLOCKS =
        DeferredRegister.createBlocks(HogHunter.MOD_ID);

public static final DeferredBlock<Block> CORRUPTED_ORE = BLOCKS.register(
        "corrupted_ore", () -> new CorruptedOreBlock(
                BlockBehaviour.Properties.of().strength(3.0f, 3.0f)));

public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
        DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, HogHunter.MOD_ID);

public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HogNestBlockEntity>> HOG_NEST =
        BLOCK_ENTITY_TYPES.register("hog_nest", () ->
                BlockEntityType.Builder.of(HogNestBlockEntity::new, HogBlocks.HOG_NEST.get())
                        .build(null));

public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, HogHunter.MOD_ID);

public static final DeferredHolder<EntityType<?>, EntityType<BoarHogEntity>> BOAR_HOG =
        ENTITY_TYPES.register("boar_hog", () ->
                EntityType.Builder.of(BoarHogEntity::new, MobCategory.MONSTER)
                        .sized(1.1f, 1.0f)
                        .clientTrackingRange(8)
                        .build(ResourceLocation.fromNamespaceAndPath(HogHunter.MOD_ID, "boar_hog")));

public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
        DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, HogHunter.MOD_ID);

public static final DeferredHolder<SoundEvent, SoundEvent> HOG_SQUEAL = SOUND_EVENTS.register(
        "hog_squeal", () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(HogHunter.MOD_ID, "hog_squeal")));

public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
        DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB, HogHunter.MOD_ID);

public static final DeferredHolder<CreativeModeTab, CreativeModeTab> HOG_HUNTER_TAB =
        CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.hoghunter.main"))
                .icon(() -> new ItemStack(HogItems.HUNTING_KNIFE.get()))
                .displayItems((parameters, output) -> {
                    output.accept(HogItems.HUNTING_KNIFE.get());
                    output.accept(HogItems.HOG_HEART.get());
                })
                .build());
```

The real code should keep these fields in `HogItems`, `HogBlocks`, `HogBlockEntities`, `HogEntities`, `HogSounds`, and `HogCreativeTabs`, respectively. `ModRegistries.register` attaches all six registers to the same mod bus. `DeferredItem`, `DeferredBlock`, and `DeferredHolder` are the registered handles; call `.get()` only in runtime callbacks after registration.

1.21.1 changes to preserve: `Item.Properties` is the item construction type; block construction uses `BlockBehaviour.Properties.of()`; creative tabs are registered with `DeferredRegister<CreativeModeTab>` rather than an old `CreativeModeTabEvent.BuildContents`-only approach; entity construction uses `EntityType.Builder.of(factory, MobCategory)` and the `build(ResourceLocation)` call; `BlockEntityType.Builder.of(factory, blocks...)` returns a builder whose `build(null)` is used here because this mod does not use a data fixer schema.

### Attributes and worldgen

`HogAttributes` uses `DeferredRegister<Attribute>` created with `BuiltInRegistries.ATTRIBUTE` and registers attributes with `new RangedAttribute(...).setSyncable(true)`. Attach the attribute set on `EntityAttributeCreationEvent` for `BOAR_HOG` and `BRUTE_HOG`; this event is a mod-bus event.

For worldgen, keep configured and placed feature holders in `HogConfiguredFeatures` and `HogPlacedFeatures`, using `DeferredRegister<ConfiguredFeature<?, ?>>` and `DeferredRegister<PlacedFeature>` against `Registries.CONFIGURED_FEATURE` and `Registries.PLACED_FEATURE`. Biome spawning is a data-driven NeoForge biome modifier under `data/hoghunter/neoforge/biome_modifier/`, generated by `HogBiomeModifierProvider`; it must not mutate biome generation in a common setup callback. If a custom biome modifier type is needed, register its codec in a `DeferredRegister<MapCodec<? extends BiomeModifier>>` against `NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS`.

## Player heartbeat and sanity data

The old Forge `CapabilityManager`/`CapabilityProvider` pattern is not the correct primary storage API in NeoForge 21.1. Persistent arbitrary player state belongs in an entity data attachment. The design exposes it through `PlayerHogState`, a capability-style service, but stores it with the 1.21.1 attachment system so it copies and persists correctly.

`PlayerHogState` contains `int heartbeat`, `float sanity`, and a short cooldown/timestamp set. Register an attachment in `HogAttachments`:

```java
public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, HogHunter.MOD_ID);

public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerHogState>> PLAYER_HOG_STATE =
        ATTACHMENT_TYPES.register("player_hog_state", () -> AttachmentType.builder(PlayerHogState::new)
                .serialize(PlayerHogState.CODEC)
                .copyOnDeath()
                .build());
```

Gameplay reads `player.getData(HogAttachments.PLAYER_HOG_STATE)` and mutates a controlled state object, or replaces it with `player.setData(...)` when using immutable records. The attachment is registered on the mod bus with every other registry. A server-side tick obtains the attachment only for `ServerPlayer` and updates it at a throttled interval; a client payload mirrors the values for HUD display.

If an integration API genuinely needs a queryable capability, define an `EntityCapability<PlayerHogState, Void>` in `HogPlayerCapabilities` and register its provider with `RegisterCapabilitiesEvent.registerEntity(...)` for `HogEntities` or player entity types. That provider should delegate to the attachment. Do not create a second source of truth and do not use block capability invalidation methods for player data.

## Server tick and event policy

`ModEvents` is registered to `NeoForge.EVENT_BUS` from the constructor. Its per-player loop listens to `PlayerTickEvent.Post`, returns unless `event.getEntity()` is a `ServerPlayer`, returns unless `event.getEntity().level().isClientSide()` is false, and performs heartbeat/sanity work only when `serverPlayer.tickCount % 5 == 0`. Use the player's `ServerLevel`, nearby hog queries, light/position checks, and the attachment. Never send a payload every tick; send only on state change or at a small HUD refresh interval.

Use event priorities deliberately:

```java
@SubscribeEvent(priority = EventPriority.HIGHEST)
public static void blockFatalHogDamage(LivingIncomingDamageEvent event) { ... }

@SubscribeEvent(priority = EventPriority.HIGH)
public static void alterHogDrops(LivingDropsEvent event) { ... }
```

`EventPriority.HIGHEST` is for a gameplay-blocking rule such as an invulnerability window or a contract rule that must cancel incoming damage before normal handlers. `HIGH` is for authoritative drop filtering/replacement. Handlers must check the entity type and source before cancelling. Do not cancel every `LivingIncomingDamageEvent`, and do not assume `LivingDropsEvent` is fired only for hogs. NeoForge event priorities are `HIGHEST`, `HIGH`, `NORMAL`, `LOW`, and `LOWEST`; priority matters on the game bus, while parallel lifecycle events must not depend on handler order.

## Networking

`HogPayloads` owns payload registration on the mod event bus through `RegisterPayloadHandlersEvent`. The protocol version is a string, currently `"1"`:

```java
public record SyncPlayerHogStatePayload(int heartbeat, int sanityFixed) implements CustomPacketPayload {
    public static final Type<SyncPlayerHogStatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(HogHunter.MOD_ID, "sync_player_hog_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPlayerHogStatePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SyncPlayerHogStatePayload::heartbeat,
                    ByteBufCodecs.VAR_INT, SyncPlayerHogStatePayload::sanityFixed,
                    SyncPlayerHogStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
```

Register it with:

```java
@SubscribeEvent
public static void registerPayloads(RegisterPayloadHandlersEvent event) {
    PayloadRegistrar registrar = event.registrar("1");
    registrar.playToClient(SyncPlayerHogStatePayload.TYPE,
            SyncPlayerHogStatePayload.STREAM_CODEC,
            HogClientPayloadHandlers::handleSync);
}
```

If a future input payload is needed, use `registrar.playToServer(...)`; its handler validates that the sender is a `ServerPlayer`, clamps all values, and schedules world mutation on the server thread. The handler must not trust client heartbeat/sanity values. `RegistryFriendlyByteBuf` is the correct 1.21.1 buffer type for registry-aware payload codecs. A client handler uses `payloadContext.enqueueWork(...)` before changing client HUD state and `payloadContext.reply(...)` only when a response is required.

## Physical sides and client safety

`HogClient` is annotated `@EventBusSubscriber(modid = HogHunter.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)`. It owns `EntityRenderersEvent.RegisterRenderers`, `RegisterKeyMappingsEvent`, `RegisterGuiLayersEvent`, client payload handlers, and renderer/model classes. Common classes must not import `net.minecraft.client.*`, renderer classes, `Minecraft`, `KeyMapping`, or `Dist.CLIENT` implementation types.

For a listener installed from the constructor, use `DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> HogClient::registerClient)` or register a common listener that only references a client-safe method reference inside the physical-side lambda. Prefer the annotation for client-only event classes because NeoForge will not load that subscriber on a dedicated server. `@EventBusSubscriber` without `value = Dist.CLIENT` is common-side and must be safe on a dedicated server.

## Data generation and validation

`HogDataGenerators` subscribes to `GatherDataEvent` on the mod bus and adds providers with `event.getGenerator().addProvider(...)`, using `event.getLookupProvider()` where a provider requires registry lookups. Generated files go to `src/generated/resources` and are included by `sourceSets.main.resources`.

The first implementation gate is:

```text
gradlew.bat runData
gradlew.bat build
gradlew.bat runClient
```

`runData` must produce the exact namespace paths above; `build` must resolve `net.neoforged:neoforge:21.1.253` and process `META-INF/neoforge.mods.toml`; `runClient` must reach the title screen with registries, payload registration, and client subscribers loaded. A dedicated server run is a separate gate because classloading a client-only renderer from common code can pass a client launch and still crash a server.

## API migration guardrails for implementers

Use `net.neoforged.*` event and mod APIs. Use `net.minecraft.core.registries.BuiltInRegistries`/`Registries` for vanilla registries and `net.neoforged.neoforge.registries.NeoForgeRegistries` for NeoForge registries. `ForgeRegistries` is not the registration API for this target. Use `ResourceLocation.fromNamespaceAndPath("hoghunter", "path")` rather than assuming the old public two-string constructor. Use `DeferredHolder` handles instead of accessing registry objects during static initialization. Use `ModContainer.registerConfig(...)` with `ModConfigSpec`, `RegisterPayloadHandlersEvent` with `PayloadRegistrar`, and entity data attachments for player state. These choices prevent the most common 1.21.1 crashes: wrong bus, wrong side, premature registry dereference, legacy config registration, and client-only classloading on a dedicated server.

### Reference material

- NeoForge 1.21.1 registries: https://docs.neoforged.net/docs/1.21.1/concepts/registries/
- NeoForge 1.21.1 events and lifecycle: https://docs.neoforged.net/docs/1.21.1/concepts/events/
- NeoForge 1.21.1 payloads: https://docs.neoforged.net/docs/1.21.1/networking/payload/
- NeoForge 1.21.1 configuration: https://docs.neoforged.net/docs/1.21.1/misc/config/
- NeoForge 1.21.1 capabilities and attachments: https://docs.neoforged.net/docs/1.21.1/inventories/capabilities/
- NeoForge 1.21.1 NeoGradle MDK: https://github.com/NeoForgeMDKs/MDK-1.21.1-NeoGradle
