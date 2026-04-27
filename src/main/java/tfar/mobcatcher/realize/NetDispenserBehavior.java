package tfar.mobcatcher.realize;

import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import org.jetbrains.annotations.NotNull;
import tfar.mobcatcher.config.ServerConfig;

public class NetDispenserBehavior extends DefaultDispenseItemBehavior {

    @Override
    public @NotNull ItemStack execute(BlockSource source, ItemStack stack) {
        Level level = source.level();
        Direction direction = source.state().getValue(DispenserBlock.FACING);

        double x = source.pos().getX() + direction.getStepX() * 1.1D;
        double y = source.pos().getY() + direction.getStepY() * 1.1D;
        double z = source.pos().getZ() + direction.getStepZ() * 1.1D;

        var netEntity = new NetEntity(x, y, z, level, stack.copyWithCount(1));
        netEntity.shoot(
                direction.getStepX(),
                direction.getStepY(),
                direction.getStepZ(),
                (float) ServerConfig.dispenserVelocity,
                (float) ServerConfig.dispenserInaccuracy
        );
        level.addFreshEntity(netEntity);
        stack.shrink(1);
        return stack;
    }
}
