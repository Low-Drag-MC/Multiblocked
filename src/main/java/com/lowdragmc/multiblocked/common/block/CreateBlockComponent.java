package com.lowdragmc.multiblocked.common.block;

import com.lowdragmc.multiblocked.api.block.BlockComponent;
import com.lowdragmc.multiblocked.api.definition.ComponentDefinition;
import com.lowdragmc.multiblocked.common.tile.CreateKineticSourceTileEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * @author KilaBash
 * @date 2022/06/02
 * @implNote CreateBlockComponent, create block.
 */
public class CreateBlockComponent extends BlockComponent implements IRotate {

    public CreateBlockComponent(ComponentDefinition definition) {
        super(definition);
    }

    @Override
    public boolean hasShaftTowards(LevelReader levelReader, BlockPos blockPos, BlockState blockState, Direction direction) {
        return blockState.getValue(BlockStateProperties.FACING) == direction;
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(BlockStateProperties.FACING).getAxis();
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return (level, blockPos, blockState, t) -> {
            if (level.getBlockEntity(blockPos) instanceof CreateKineticSourceTileEntity createKineticSourceTile) {
                createKineticSourceTile.tick();
            }
        };
    }
}
