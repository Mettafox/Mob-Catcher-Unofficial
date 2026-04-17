package tfar.mobcatcher.init;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import tfar.mobcatcher.realize.NetItem;
import tfar.mobcatcher.realize.NetLauncherItem;

import static tfar.mobcatcher.MobCatcher.MOD_ID;

public class ModItems {

    public static int netStackSize = 64;
    public static Item NET;
    public static Item NET_LAUNCHER;

    public static void register() {
        NET_LAUNCHER = Registry.register(
                BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "net_launcher"),
                new NetLauncherItem(new Item.Properties().stacksTo(1))
        );

        NET = Registry.register(
                BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "net"),
                new NetItem(new Item.Properties().stacksTo(netStackSize))
        );

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT).register(entries -> {
            entries.accept(NET);
            entries.accept(NET_LAUNCHER);
        });
    }
}
