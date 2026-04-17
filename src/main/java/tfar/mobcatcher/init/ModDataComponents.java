package tfar.mobcatcher.init;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import static tfar.mobcatcher.MobCatcher.MOD_ID;

public class ModDataComponents {

    public static DataComponentType<CompoundTag> ENTITY_HOLDER;
    public static DataComponentType<Boolean> CAPTURE_MODE;

    public static void register() {
        ENTITY_HOLDER = Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "entity_holder"),
                DataComponentType.<CompoundTag>builder()
                        .persistent(CompoundTag.CODEC)
                        .build()
        );

        CAPTURE_MODE = Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "capture_mode"),
                DataComponentType.<Boolean>builder()
                        .persistent(Codec.BOOL)
                        .build()
        );
    }
}
