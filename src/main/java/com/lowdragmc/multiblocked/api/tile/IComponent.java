package com.lowdragmc.multiblocked.api.tile;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.multiblocked.api.definition.ComponentDefinition;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.World;

import java.util.UUID;

/**
 * @author KilaBash
 * @date 2022/06/01
 * @implNote IComponent
 */
public interface IComponent {
    default TileEntity self() {
        return (TileEntity) this;
    }

    ComponentDefinition getDefinition();

    default ActionResultType use(PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        return ActionResultType.PASS;
    }

    default void onNeighborChange() {}

    default void setOwner(UUID uuid) {}

    default boolean isValidFrontFacing(Direction up) {
        return getDefinition().allowRotate;
    }

    default void setFrontFacing(Direction facing) {
        World level = self().getLevel();
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

    default void onDrops(NonNullList<ItemStack> drops, PlayerEntity entity) {
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

    default VoxelShape getDynamicShape() {
        return getDefinition().properties.shape;
    }
}
