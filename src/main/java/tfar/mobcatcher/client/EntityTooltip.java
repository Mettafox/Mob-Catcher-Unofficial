package tfar.mobcatcher.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

/**
 * Data carrier for the entity tooltip — immutable by design.
 * Using a record eliminates boilerplate and signals intent clearly.
 */
public record EntityTooltip(CompoundTag entityTag, Component itemName) implements TooltipComponent {}
