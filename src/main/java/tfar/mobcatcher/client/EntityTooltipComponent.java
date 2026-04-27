package tfar.mobcatcher.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.WaterAnimal;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.ref.SoftReference;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class EntityTooltipComponent implements ClientTooltipComponent {

    private static final int MIN_WIDTH = 120;
    private static final int MAX_WIDTH = 400;
    private static final int DISPLAY_HEIGHT = 60;
    private static final float BASE_SCALE = 25.0F;
    private static final float MIN_SCALE = 1.0F;

    /** Reused every frame — avoids allocating a new Vector3f per render call. */
    private static final Vector3f ZERO = new Vector3f();

    private static final Map<String, Float> VISUAL_HEIGHT_OVERRIDES = Map.ofEntries(
            Map.entry("minecraft:enderman", 1.4F),
            Map.entry("minecraft:iron_golem", 1.4F),
            Map.entry("minecraft:warden", 1.6F),
            Map.entry("minecraft:camel", 1.4F),
            Map.entry("minecraft:horse", 1.2F),
            Map.entry("minecraft:skeleton_horse", 1.2F),
            Map.entry("minecraft:zombie_horse", 1.2F),
            Map.entry("minecraft:ghast", 6.5F),
            Map.entry("minecraft:ender_dragon", 8.0F),
            Map.entry("minecraft:wither", 4.0F),
            Map.entry("minecraft:elder_guardian", 2.2F),
            Map.entry("minecraft:wither_skeleton", 1.2F),
            Map.entry("minecraft:bee", 0.2F),
            Map.entry("minecraft:parrot", 0.2F)
    );

    // Fields resolved once at construction — avoids repeated NBT reads and registry lookups per frame
    private final CompoundTag entityTag;
    private final String entityId;
    private final EntityType<?> entityType;
    private final Component entityName;
    private final Component itemName;
    private final Component healthText;
    private int cachedWidth;

    /**
     * Entity held via SoftReference — the GC only reclaims it under actual memory pressure.
     * This avoids repeated entity creation while still releasing memory when needed.
     */
    private SoftReference<LivingEntity> entityRef = new SoftReference<>(null);

    public EntityTooltipComponent(EntityTooltip tooltipData) {
        this.entityTag = tooltipData.entityTag();
        this.itemName = tooltipData.itemName();
        this.entityId = entityTag.getString("id");
        this.entityType = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(entityId));
        this.entityName = entityType.getDescription();

        double health = entityTag.getDouble("Health");
        this.healthText = Component.translatable("mobcatcher.health")
                .append(": " + String.format("%.1f", health));
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private int computeWidth(Font font) {
        int entityNameWidth = font.width(entityName);
        int itemNameWidth = font.width(itemName);
        return Math.clamp(Math.max(entityNameWidth, itemNameWidth), MIN_WIDTH, MAX_WIDTH);
    }

    @Override
    public int getHeight() {
        return 20 + DISPLAY_HEIGHT + 10;
    }

    @Override
    public int getWidth(@NotNull Font font) {
        cachedWidth = computeWidth(font);
        return cachedWidth;
    }

    // ── Scale ─────────────────────────────────────────────────────────────────

    private float calculateScale(int displayWidth) {
        float visualHeight = VISUAL_HEIGHT_OVERRIDES.getOrDefault(entityId, entityType.getDimensions().height());
        float visualWidth = entityType.getDimensions().width();

        float scaleByHeight = (DISPLAY_HEIGHT - 4) / (visualHeight * 2.0F);
        float scaleByWidth = (displayWidth - 8) / (visualWidth * 2.0F);

        return Math.clamp(Math.min(scaleByHeight, scaleByWidth), MIN_SCALE, BASE_SCALE);
    }

    // ── Entity type helpers ───────────────────────────────────────────────────

    private static boolean isAquatic(LivingEntity e) {
        return e instanceof AbstractFish || e instanceof WaterAnimal;
    }

    private static boolean isFlying(LivingEntity e) {
        return e instanceof FlyingMob || e instanceof FlyingAnimal || e instanceof AmbientCreature;
    }

    // ── Entity cache ──────────────────────────────────────────────────────────

    /**
     * Returns the cached entity, or creates and initialises a new one if the
     * SoftReference was cleared by the GC or on first call.
     */
    private LivingEntity getOrCreateEntity(ClientLevel level) {
        LivingEntity entity = entityRef.get();
        if (entity != null) return entity;

        entity = (LivingEntity) entityType.create(level);
        if (entity == null) return null;

        entity.setOnGround(true);
        entity.load(entityTag);

        // Reset all rotation fields saved in NBT
        entity.setXRot(0); entity.xRotO = 0;
        entity.setYRot(0); entity.yRotO = 0;
        entity.yBodyRot = 0; entity.yBodyRotO = 0;
        entity.yHeadRot = 0; entity.yHeadRotO = 0;

        entityRef = new SoftReference<>(entity);
        return entity;
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    @Override
    public void renderImage(Font font, int pX, int pY, GuiGraphics guiGraphics) {
        guiGraphics.drawString(font, entityName, pX, pY, 0xFFFFFF);
        guiGraphics.drawString(font, healthText, pX, pY + 10, 0xAAAAFF);

        int displayWidth = cachedWidth > 0 ? cachedWidth : computeWidth(font);
        int renderAreaTop = pY + 20;
        int renderAreaBottom = renderAreaTop + DISPLAY_HEIGHT;
        int posX = pX + displayWidth / 2;
        float scale = calculateScale(displayWidth);
        double rot = System.currentTimeMillis() / 25.0D % 360.0D;

        guiGraphics.enableScissor(pX, renderAreaTop, pX + displayWidth, renderAreaBottom);

        ClientLevel clientLevel = Minecraft.getInstance().level;
        if (clientLevel == null) {
            guiGraphics.disableScissor();
            return;
        }

        LivingEntity livingEntity = getOrCreateEntity(clientLevel);
        if (livingEntity == null) {
            guiGraphics.disableScissor();
            return;
        }

        boolean isSquid = livingEntity instanceof Squid;
        boolean isFish = livingEntity instanceof AbstractFish;
        boolean aquatic = isAquatic(livingEntity);
        boolean flying = isFlying(livingEntity);

        int posY = (aquatic || flying)
                ? renderAreaTop + DISPLAY_HEIGHT / 2 + (int)(scale * 0.3F)
                : renderAreaBottom;

        Quaternionf pose;
        if (isSquid) {
            pose = new Quaternionf()
                    .rotateZ((float) Math.PI)
                    .rotateX((float) Math.toRadians(-90))
                    .rotateY((float) Math.toRadians(90))
                    .rotateX((float) Math.toRadians(50 - rot));
        } else if (isFish) {
            pose = new Quaternionf()
                    .rotateZ((float) Math.PI)
                    .rotateX((float) Math.toRadians(90))
                    .rotateY((float) Math.toRadians(90))
                    .rotateX((float) Math.toRadians(rot));
        } else {
            pose = new Quaternionf()
                    .rotateZ((float) Math.PI)
                    .rotateY((float) Math.toRadians(rot));
        }

        InventoryScreen.renderEntityInInventory(
                guiGraphics, posX, posY, scale,
                ZERO, pose, null, livingEntity);

        guiGraphics.disableScissor();
    }
}
