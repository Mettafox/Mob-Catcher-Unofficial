package tfar.mobcatcher.realize;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import tfar.mobcatcher.MobCatcher;
import tfar.mobcatcher.client.EntityTooltip;
import tfar.mobcatcher.config.ServerConfig;
import tfar.mobcatcher.init.ModDataComponents;

import java.util.Optional;

public class NetItem extends Item {

    /** Populated by ServerLifecycleEvents in MobCatcher — null on client-only sessions. */
    public static net.minecraft.server.MinecraftServer currentServer = null;

    public NetItem(Properties properties) {
        super(properties);
    }

    public static RegistryAccess registryAccess() {
        if (currentServer != null) return currentServer.registryAccess();
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            return clientRegistryAccess();
        }
        return RegistryAccess.EMPTY;
    }

    @Environment(EnvType.CLIENT)
    private static RegistryAccess clientRegistryAccess() {
        var mc = Minecraft.getInstance();
        return (mc != null && mc.level != null) ? mc.level.registryAccess() : RegistryAccess.EMPTY;
    }

    public static Component getNameFromStoredEntity(ItemStack stack) {
        CompoundTag holder = getEntityData(stack);
        if (holder.contains("CustomName", Tag.TAG_STRING)) {
            String json = holder.getString("CustomName");
            Component parsed = Component.Serializer.fromJson(json, RegistryAccess.EMPTY);
            if (parsed != null) return parsed;
        }
        String id = holder.getString("id");
        return BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(id)).getDescription();
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        if (containsEntity(stack)) {
            return Optional.of(new EntityTooltip(getEntityData(stack), getName(stack)));
        }
        return Optional.empty();
    }

    public static boolean containsEntity(ItemStack stack) {
        var tag = stack.get(ModDataComponents.ENTITY_HOLDER);
        return tag != null && !tag.isEmpty();
    }

    public static CompoundTag getEntityData(ItemStack stack) {
        return containsEntity(stack) ? stack.get(ModDataComponents.ENTITY_HOLDER) : new CompoundTag();
    }

    public static boolean isBlacklisted(EntityType<?> type) {
        return type == EntityType.PLAYER || type.is(MobCatcher.blacklisted);
    }

    public static Entity getEntityFromNBT(CompoundTag nbt, Level world, boolean withInfo) {
        Entity entity = BuiltInRegistries.ENTITY_TYPE
                .get(ResourceLocation.parse(nbt.getString("id")))
                .create(world);
        if (entity != null && withInfo) entity.load(nbt);
        return entity;
    }

    public static Entity getEntityFromStack(ItemStack stack, Level world, boolean withInfo) {
        return getEntityFromNBT(stack.get(ModDataComponents.ENTITY_HOLDER), world, withInfo);
    }

    public static CompoundTag getNBTfromEntity(Entity entity) {
        var nbt = new CompoundTag();
        entity.save(nbt);
        return nbt;
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level world = context.getLevel();
        if (player == null) return InteractionResult.FAIL;

        ItemStack stack = context.getItemInHand();
        if (world.isClientSide || !containsEntity(stack)) return InteractionResult.FAIL;

        Entity entity = getEntityFromStack(stack, world, true);
        if (entity == null) return InteractionResult.FAIL;

        BlockPos pos = context.getClickedPos();
        entity.absMoveTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0, 0);
        stack.remove(ModDataComponents.ENTITY_HOLDER);
        world.addFreshEntity(entity);

        if (stack.isDamageableItem()) {
            stack.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactLivingEntity(@NotNull ItemStack stack, @NotNull Player player,
                                                  LivingEntity target, @NotNull InteractionHand hand) {
        if (target.level().isClientSide
                || target instanceof Player
                || !target.isAlive()
                || containsEntity(stack)) {
            return InteractionResult.FAIL;
        }

        EntityType<?> entityType = target.getType();
        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString();

        if (isBlacklisted(entityType) || ServerConfig.entityBlacklist.contains(entityId)) {
            return InteractionResult.FAIL;
        }

        CompoundTag nbt = getNBTfromEntity(target);

        if (stack.getCount() == 1) {
            ItemStack copy = player.isCreative() ? stack.copy() : stack;
            copy.set(ModDataComponents.ENTITY_HOLDER, nbt);
            if (player.isCreative()) player.setItemInHand(hand, copy);
        } else {
            ItemStack split = stack.split(1);
            split.set(ModDataComponents.ENTITY_HOLDER, nbt);
            if (!player.addItem(split)) {
                player.level().addFreshEntity(
                        new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), split));
            }
        }

        player.swing(hand);
        target.discard();
        player.getCooldowns().addCooldown(this, 5);
        return InteractionResult.SUCCESS;
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        var custom = stack.get(DataComponents.CUSTOM_NAME);
        var base = custom != null ? custom : super.getName(stack);
        return containsEntity(stack)
                ? Component.translatable("item.mobcatcher.net.with_entity", base, getNameFromStoredEntity(stack))
                : base;
    }
}
