package tfar.mobcatcher.realize;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import tfar.mobcatcher.MobCatcher;
import tfar.mobcatcher.client.EntityTooltip;
import tfar.mobcatcher.config.ServerConfig;
import tfar.mobcatcher.init.ModDataComponents;
import tfar.mobcatcher.init.ModItems;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

public class NetItem extends Item {

    // Thread-local server reference set during server lifecycle events in MobCatcher
    public static net.minecraft.server.MinecraftServer currentServer = null;

    public NetItem(Properties properties) {
        super(properties);
    }

    /**
     * Gets RegistryAccess from the current server if available, otherwise falls back to the client level.
     */
    public static RegistryAccess registryAccess() {
        if (currentServer != null) {
            return currentServer.registryAccess();
        }
        // Client side fallback
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.level != null) {
            return mc.level.registryAccess();
        }
        return RegistryAccess.EMPTY;
    }

    /**
     * Gets RegistryAccess from a Level directly (preferred when a Level is available).
     */
    public static RegistryAccess getRegistryAccess(Level level) {
        return level.registryAccess();
    }

    public static Component getNameFromStoredEntity(ItemStack stack) {
        CompoundTag holder = getEntityData(stack);
        if (holder.contains("CustomName", Tag.TAG_STRING)) {
            String s = holder.getString("CustomName");
            // Use empty RegistryAccess for Component parsing; this is fine for display names
            return Component.Serializer.fromJson(s, RegistryAccess.EMPTY);
        }
        String id = holder.getString("id");
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(id));
        return type.getDescription();
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flagIn) {
        super.appendHoverText(stack, context, tooltip, flagIn);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        if (containsEntity(stack)) {
            CompoundTag entityData = getEntityData(stack);
            return Optional.of(new EntityTooltip(entityData));
        }
        return Optional.empty();
    }

    public static boolean containsEntity(ItemStack stack) {
        CompoundTag tag = stack.get(ModDataComponents.ENTITY_HOLDER);
        return tag != null && !tag.isEmpty();
    }

    public static CompoundTag getEntityData(ItemStack stack) {
        return containsEntity(stack) ? stack.get(ModDataComponents.ENTITY_HOLDER) : new CompoundTag();
    }

    public static String getEntityID(CompoundTag nbt) {
        return nbt.getString("id");
    }

    public static boolean isBlacklisted(EntityType<?> type) {
        return type == EntityType.PLAYER || type.is(MobCatcher.blacklisted);
    }

    public static Entity getEntityFromNBT(CompoundTag nbt, Level world, boolean withInfo) {
        Entity entity = BuiltInRegistries.ENTITY_TYPE
                .get(ResourceLocation.parse(getEntityID(nbt)))
                .create(world);
        if (entity != null && withInfo) entity.load(nbt);
        return entity;
    }

    public static Entity getEntityFromStack(ItemStack stack, Level world, boolean withInfo) {
        return getEntityFromNBT(stack.get(ModDataComponents.ENTITY_HOLDER), world, withInfo);
    }

    public static CompoundTag getNBTfromEntity(Entity entity) {
        CompoundTag nbt = new CompoundTag();
        entity.save(nbt);
        return nbt;
    }

    @Override
    @Nonnull
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level world = context.getLevel();
        if (player == null) return InteractionResult.FAIL;
        ItemStack stack = context.getItemInHand();
        if (world.isClientSide || !containsEntity(stack)) return InteractionResult.FAIL;

        Entity entity = getEntityFromStack(stack, world, true);
        BlockPos blockPos = context.getClickedPos();
        entity.absMoveTo(blockPos.getX() + 0.5, blockPos.getY() + 1, blockPos.getZ() + 0.5, 0, 0);
        stack.remove(ModDataComponents.ENTITY_HOLDER);
        world.addFreshEntity(entity);

        /*if (isDamageable(stack)) {
            stack.hurtAndBreak(1, player, player.getMainHandItem().getEquipmentSlot());
        }
        return InteractionResult.SUCCESS;*/

        if (stack.isDamageableItem()) {
            stack.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactLivingEntity(@NotNull ItemStack stack, @NotNull Player player,
                                                   LivingEntity target, @NotNull InteractionHand hand) {
        if (target.getCommandSenderWorld().isClientSide
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
            if (player.isCreative()) {
                ItemStack newStack = stack.copy();
                newStack.set(ModDataComponents.ENTITY_HOLDER, nbt);
                player.setItemInHand(hand, newStack);
            } else {
                stack.set(ModDataComponents.ENTITY_HOLDER, nbt);
            }
        } else {
            ItemStack newStack = stack.split(1);
            newStack.set(ModDataComponents.ENTITY_HOLDER, nbt);
            if (!player.addItem(newStack)) {
                ItemEntity itemEntity = new ItemEntity(
                        player.level(), player.getX(), player.getY(), player.getZ(), newStack);
                player.level().addFreshEntity(itemEntity);
            }
        }

        player.swing(hand);
        target.discard();
        player.getCooldowns().addCooldown(this, 5);
        return InteractionResult.SUCCESS;
    }

    @Override
    @Nonnull
    public Component getName(@Nonnull ItemStack stack) {
        Component nameComponent = stack.get(DataComponents.CUSTOM_NAME);
        Component baseName = nameComponent != null ? nameComponent : super.getName(stack);
        if (!containsEntity(stack)) {
            return baseName;
        } else {
            return Component.translatable("item.mobcatcher.net.with_entity",
                    baseName, getNameFromStoredEntity(stack));
        }
    }

    public NetEntity createNet(Level worldIn, LivingEntity shooter, ItemStack stack) {
        ItemStack newStack = stack.copy();
        newStack.setCount(1);
        return new NetEntity(shooter.getX(), shooter.getY() + 1.25, shooter.getZ(), worldIn, newStack);
    }
}
