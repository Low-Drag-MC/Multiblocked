package com.lowdragmc.multiblocked.common.block;

import com.lowdragmc.multiblocked.api.block.BlockComponent;
import com.lowdragmc.multiblocked.api.definition.ComponentDefinition;
import com.simibubi.create.content.contraptions.base.IRotate;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

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

}
