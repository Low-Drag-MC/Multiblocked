package com.lowdragmc.multiblocked.api.tile;


import com.lowdragmc.multiblocked.api.definition.ComponentDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class DummyComponentTileEntity extends ComponentTileEntity<ComponentDefinition> {
    public boolean isFormed;

    public DummyComponentTileEntity(ComponentDefinition definition, BlockPos pos, BlockState state) {
        super(definition, pos, state);
    }

    public void setDefinition(ComponentDefinition definition) {
        this.definition = definition;
    }


    @Override
    public boolean isFormed() {
        return isFormed;
    }
}
