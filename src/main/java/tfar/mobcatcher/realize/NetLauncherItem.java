package tfar.mobcatcher.realize;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import tfar.mobcatcher.config.ServerConfig;
import tfar.mobcatcher.init.ModDataComponents;

public class NetLauncherItem extends Item {

    public static final Component CAPTURE = Component.translatable("mobcatcher.capturing");
    public static final Component RELEASE = Component.translatable("mobcatcher.releasing");

    public NetLauncherItem(Properties properties) {
        super(properties);
    }

    public static float getNetVelocity(int charge) {
        float f = (float) charge / 20;
        f = (f * f + f * 2) / 3;
        return Math.min(f, 1.5f);
    }

    public static boolean isCaptureMode(ItemStack stack) {
        var value = stack.get(ModDataComponents.CAPTURE_MODE);
        return value == null || value;
    }

    public static boolean isEmptyNet(ItemStack stack) {
        return stack.getItem() instanceof NetItem && !NetItem.containsEntity(stack);
    }

    public static boolean isFilledNet(ItemStack stack) {
        return stack.getItem() instanceof NetItem && NetItem.containsEntity(stack);
    }

    protected ItemStack findNet(Player player) {
        boolean capture = isCaptureMode(player.getMainHandItem());

        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack s = player.getItemInHand(hand);
            if (capture ? isEmptyNet(s) : isFilledNet(s)) return s;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (capture ? isEmptyNet(s) : isFilledNet(s)) return s;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void releaseUsing(@NotNull ItemStack stack, @NotNull Level world,
                             @NotNull LivingEntity user, int timeLeft) {
        if (!(user instanceof Player player)) return;

        ItemStack netStack = findNet(player);
        if (netStack.isEmpty()) return;

        int useTime = getUseDuration(netStack, player) - timeLeft;
        if (useTime < 0) return;

        float velocity = getNetVelocity(useTime);
        if (velocity < 0.1f) return;

        if (!world.isClientSide) {
            var netEntity = new NetEntity(world, player, netStack.copy());
            netEntity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F,
                    velocity * (float) ServerConfig.launcherVelocityMultiplier, 0.0F);
            world.addFreshEntity(netEntity);

            if (!player.getAbilities().instabuild) {
                netStack.shrink(1);
            }
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
        return 72000;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level world, @NotNull Player player,
                                                           @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isCrouching()) {
            boolean current = isCaptureMode(stack);
            stack.set(ModDataComponents.CAPTURE_MODE, !current);
            player.displayClientMessage(current ? RELEASE : CAPTURE, true);
            return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
        }

        if (!player.getAbilities().instabuild && findNet(player).isEmpty()) {
            return new InteractionResultHolder<>(InteractionResult.FAIL, stack);
        }

        player.startUsingItem(hand);
        return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        return ((MutableComponent) super.getName(stack))
                .append(" (")
                .append(isCaptureMode(stack) ? CAPTURE : RELEASE)
                .append(")");
    }
}
