package com.lowdragmc.multiblocked.common.block;

import com.lowdragmc.multiblocked.api.block.BlockComponent;
import com.lowdragmc.multiblocked.api.definition.ComponentDefinition;
import com.simibubi.create.content.contraptions.base.IRotate;
import net.minecraft.block.BlockState;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;

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
    public boolean hasShaftTowards(IWorldReader world, BlockPos pos, BlockState state, Direction face) {
        return state.getValue(BlockStateProperties.FACING) == face;
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(BlockStateProperties.FACING).getAxis();
    }

}
