package tfar.mobcatcher;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import tfar.mobcatcher.config.ServerConfig;
import tfar.mobcatcher.init.ModBehaviors;
import tfar.mobcatcher.init.ModDataComponents;
import tfar.mobcatcher.init.ModEntities;
import tfar.mobcatcher.init.ModItems;
import tfar.mobcatcher.realize.NetItem;

public class MobCatcher implements ModInitializer {

    public static final String MOD_ID = "mobcatcher";

    public static final TagKey<EntityType<?>> blacklisted = TagKey.create(
            Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "blacklisted")
    );

    @Override
    public void onInitialize() {
        ServerConfig.init();
        ModDataComponents.register();
        ModEntities.register();
        ModItems.register();
        ModBehaviors.registerDispenserBehaviors();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> NetItem.currentServer = server);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> NetItem.currentServer = null);
    }
}
