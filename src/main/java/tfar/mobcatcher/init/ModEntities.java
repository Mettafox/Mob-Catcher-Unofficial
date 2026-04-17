package tfar.mobcatcher.init;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import tfar.mobcatcher.realize.NetEntity;

import static tfar.mobcatcher.MobCatcher.MOD_ID;

public class ModEntities {

    public static EntityType<NetEntity> NET;

    public static void register() {
        NET = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "net"),
                FabricEntityTypeBuilder.<NetEntity>create(MobCategory.MISC, NetEntity::new)
                        .dimensions(EntityDimensions.fixed(0.6f, 0.6f))
                        .trackRangeBlocks(128)
                        .trackedUpdateRate(1)
                        .forceTrackedVelocityUpdates(true)
                        .build()
        );
    }
}
