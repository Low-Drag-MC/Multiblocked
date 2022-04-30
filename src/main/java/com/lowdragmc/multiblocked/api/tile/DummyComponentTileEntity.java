package com.lowdragmc.multiblocked.api.tile;


import com.lowdragmc.multiblocked.api.definition.ComponentDefinition;

public class DummyComponentTileEntity extends ComponentTileEntity<ComponentDefinition> {
    public boolean isFormed;

    public DummyComponentTileEntity(ComponentDefinition definition) {
        super(definition);
    }

    @Override
    public boolean isFormed() {
        return isFormed;
    }
}
