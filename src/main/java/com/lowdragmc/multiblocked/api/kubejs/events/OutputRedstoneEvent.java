package com.lowdragmc.multiblocked.api.kubejs.events;

import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import dev.latvian.kubejs.event.EventJS;
import net.minecraft.util.Direction;

public class OutputRedstoneEvent extends EventJS {
    public static final String ID = "mbd.output_redstone";
    private final ComponentTileEntity<?> component;
    private final Direction facing;
    public int redstone;

    public OutputRedstoneEvent(ComponentTileEntity<?> component, Direction facing) {
        this.component = component;
        this.facing = facing;
    }

    public Direction getFacing() {
        return facing;
    }

    public ComponentTileEntity<?> getComponent() {
        return component;
    }

    public void setRedstone(int redstone) {
        this.redstone = redstone;
    }

}
