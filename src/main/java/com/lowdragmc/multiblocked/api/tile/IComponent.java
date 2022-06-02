package com.lowdragmc.multiblocked.api.tile;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.multiblocked.api.definition.ComponentDefinition;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * @author KilaBash
 * @date 2022/06/01
 * @implNote IComponent
 */
public interface IComponent {
    default BlockEntity self() {
        return (BlockEntity) this;
    }

    ComponentDefinition getDefinition();

    default InteractionResult use(Player player, InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    default void onNeighborChange() {}

    default void setOwner(UUID uuid) {}

    default boolean isValidFrontFacing(Direction up) {
        return getDefinition().allowRotate;
    }

    default void setFrontFacing(Direction facing) {
        Level level = self().getLevel();
        if (level != null && !level.isClientSide) {
            if (!isValidFrontFacing(facing)) return;
            if (self().getBlockState().getValue(BlockStateProperties.FACING) == facing) return;
            level.setBlock(self().getBlockPos(), self().getBlockState().setValue(BlockStateProperties.FACING, facing), 3);
        }
    }

    default Direction getFrontFacing() {
        return self().getBlockState().getValue(BlockStateProperties.FACING);
    }

    default void rotateTo(Rotation direction) {
        setFrontFacing(direction.rotate(getFrontFacing()));
    }

    default void onDrops(NonNullList<ItemStack> drops, Player entity) {
        drops.add(getDefinition().getStackForm());
    }

    default boolean canConnectRedstone(Direction direction) {
        return false;
    }

    IRenderer getRenderer();

    boolean isFormed();

    void setRendererObject(Object o);

    Object getRendererObject();

    String getStatus();

    void setStatus(String status);

}
