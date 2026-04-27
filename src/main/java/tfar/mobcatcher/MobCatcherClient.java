package tfar.mobcatcher;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import tfar.mobcatcher.client.EntityTooltip;
import tfar.mobcatcher.client.EntityTooltipComponent;
import tfar.mobcatcher.init.ModEntities;

public class MobCatcherClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.NET, ThrownItemRenderer::new);

        TooltipComponentCallback.EVENT.register(data -> {
            if (data instanceof EntityTooltip entityTooltip) {
                return new EntityTooltipComponent(entityTooltip);
            }
            return null;
        });
    }
}
