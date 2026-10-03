package com.hoghunter.content;

import com.hoghunter.HogHunterMod;
import com.hoghunter.entity.BoarHogEntity;
import com.hoghunter.entity.HookHogEntity;
import com.hoghunter.entity.IronbackHogEntity;
import com.hoghunter.entity.MireHogEntity;
import com.hoghunter.entity.RootmotherEntity;
import com.hoghunter.entity.ScreecherHogEntity;
import com.hoghunter.entity.SporeHogEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registry holder for every corrupted hog. */
public final class HogEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, HogHunterMod.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<BoarHogEntity>> BOAR_HOG =
            ENTITIES.register("boar_hog", () -> EntityType.Builder
                    .of(BoarHogEntity::new, MobCategory.MONSTER)
                    .sized(1.1F, 1.0F)
                    .clientTrackingRange(8)
                    .fireImmune()
                    .build("hoghunter:boar_hog"));

    public static final DeferredHolder<EntityType<?>, EntityType<SporeHogEntity>> SPORE_HOG =
            ENTITIES.register("spore_hog", () -> EntityType.Builder
                    .of(SporeHogEntity::new, MobCategory.MONSTER)
                    .sized(1.2F, 1.3F)
                    .clientTrackingRange(8)
                    .build("hoghunter:spore_hog"));

    public static final DeferredHolder<EntityType<?>, EntityType<HookHogEntity>> HOOK_HOG =
            ENTITIES.register("hook_hog", () -> EntityType.Builder
                    .of(HookHogEntity::new, MobCategory.MONSTER)
                    .sized(1.0F, 1.6F)
                    .clientTrackingRange(10)
                    .build("hoghunter:hook_hog"));

    public static final DeferredHolder<EntityType<?>, EntityType<ScreecherHogEntity>> SCREECHER_HOG =
            ENTITIES.register("screecher_hog", () -> EntityType.Builder
                    .of(ScreecherHogEntity::new, MobCategory.MONSTER)
                    .sized(0.9F, 1.5F)
                    .clientTrackingRange(10)
                    .build("hoghunter:screecher_hog"));

    public static final DeferredHolder<EntityType<?>, EntityType<IronbackHogEntity>> IRONBACK_HOG =
            ENTITIES.register("ironback_hog", () -> EntityType.Builder
                    .of(IronbackHogEntity::new, MobCategory.MONSTER)
                    .sized(1.4F, 1.2F)
                    .clientTrackingRange(8)
                    .build("hoghunter:ironback_hog"));

    public static final DeferredHolder<EntityType<?>, EntityType<MireHogEntity>> MIRE_HOG =
            ENTITIES.register("mire_hog", () -> EntityType.Builder
                    .of(MireHogEntity::new, MobCategory.MONSTER)
                    .sized(1.3F, 1.0F)
                    .clientTrackingRange(8)
                    .build("hoghunter:mire_hog"));

    public static final DeferredHolder<EntityType<?>, EntityType<RootmotherEntity>> ROOTMOTHER =
            ENTITIES.register("rootmother", () -> EntityType.Builder
                    .of(RootmotherEntity::new, MobCategory.MONSTER)
                    .sized(2.8F, 2.6F)
                    .clientTrackingRange(12)
                    .fireImmune()
                    .build("hoghunter:rootmother"));

    private HogEntities() {
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        modBus.addListener(com.hoghunter.entity.HogSpawnPlacements::register);
    }
}
