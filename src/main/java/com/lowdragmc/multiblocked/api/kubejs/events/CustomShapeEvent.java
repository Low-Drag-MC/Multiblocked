package com.lowdragmc.multiblocked.api.kubejs.events;

import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import dev.latvian.kubejs.event.EventJS;
import net.minecraft.util.math.shapes.VoxelShape;

public class CustomShapeEvent extends EventJS {
    public static final String ID = "mbd.custom_shape";
    private final ComponentTileEntity<?> component;
    private VoxelShape shape;

    public CustomShapeEvent(ComponentTileEntity<?> component, VoxelShape shape) {
        this.component = component;
        this.shape = shape;
    }

    public ComponentTileEntity<?> getComponent() {
        return component;
    }

    public VoxelShape getShape() {
        return shape;
    }

    public void setShape(VoxelShape shape) {
        this.shape = shape;
    }

    @Override
    public boolean canCancel() {
        return true;
    }
}
