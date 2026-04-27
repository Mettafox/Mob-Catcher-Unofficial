package tfar.mobcatcher.realize;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;
import tfar.mobcatcher.config.ServerConfig;
import tfar.mobcatcher.init.ModDataComponents;
import tfar.mobcatcher.init.ModEntities;
import tfar.mobcatcher.init.ModItems;

public class NetEntity extends ThrowableItemProjectile {

    private ItemStack stack;

    public NetEntity(EntityType<? extends NetEntity> type, Level world) {
        super(type, world);
        this.stack = new ItemStack(ModItems.NET);
    }

    public NetEntity(Level world, LivingEntity shooter, ItemStack newStack) {
        super(ModEntities.NET, shooter, world);
        this.stack = validated(newStack);
    }

    public NetEntity(double x, double y, double z, Level world, ItemStack newStack) {
        super(ModEntities.NET, x, y, z, world);
        this.stack = validated(newStack);
    }

    private static ItemStack validated(ItemStack input) {
        return input.isEmpty() ? new ItemStack(ModItems.NET) : input.copy();
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return ModItems.NET;
    }

    @Override
    public @NotNull ItemStack getItem() {
        return stack.isEmpty() ? new ItemStack(ModItems.NET) : stack;
    }

    public void setStack(ItemStack stack) {
        this.stack = validated(stack);
    }

    @Override
    protected void onHit(@NotNull HitResult result) {
        if (level().isClientSide || !isAlive()) return;

        boolean containsEntity = NetItem.containsEntity(stack);

        if (containsEntity) {
            Entity entity = NetItem.getEntityFromStack(stack, level(), true);
            if (entity == null) {
                level().addFreshEntity(createDroppedItem(this, stack.copyWithCount(1)));
                discard();
                return;
            }

            BlockPos pos = switch (result.getType()) {
                case ENTITY -> ((EntityHitResult) result).getEntity().blockPosition();
                default -> ((BlockHitResult) result).getBlockPos();
            };
            entity.absMoveTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0, 0);
            stack.remove(ModDataComponents.ENTITY_HOLDER);
            level().addFreshEntity(entity);
            level().addFreshEntity(createDroppedItem(this, stack.copy()));

        } else if (result.getType() == HitResult.Type.ENTITY) {
            Entity target = ((EntityHitResult) result).getEntity();
            String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();

            if (!(target instanceof LivingEntity)
                    || !target.isAlive()
                    || NetItem.isBlacklisted(target.getType())
                    || ServerConfig.entityBlacklist.contains(entityId)) {
                return;
            }

            ItemStack captured = stack.copyWithCount(1);
            captured.set(ModDataComponents.ENTITY_HOLDER, NetItem.getNBTfromEntity(target));
            level().addFreshEntity(createDroppedItem(target, captured));
            target.discard();

        } else {
            level().addFreshEntity(createDroppedItem(this, stack.copyWithCount(1)));
        }

        discard();
    }

    private ItemEntity createDroppedItem(Entity at, ItemStack itemStack) {
        return new ItemEntity(level(), at.getX(), at.getY(), at.getZ(), itemStack);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        if (!stack.isEmpty()) {
            nbt.put("mobcatcher", stack.save(level().registryAccess()));
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        stack = nbt.contains("mobcatcher", Tag.TAG_COMPOUND)
                ? ItemStack.parse(level().registryAccess(), nbt.getCompound("mobcatcher"))
                        .orElseGet(() -> new ItemStack(ModItems.NET))
                : new ItemStack(ModItems.NET);
    }
}
