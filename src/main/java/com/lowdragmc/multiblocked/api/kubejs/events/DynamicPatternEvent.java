package com.lowdragmc.multiblocked.api.kubejs.events;

import com.lowdragmc.multiblocked.api.pattern.BlockPattern;
import com.lowdragmc.multiblocked.api.tile.ControllerTileEntity;
import dev.latvian.mods.kubejs.event.EventJS;

public class DynamicPatternEvent extends EventJS {
    public static final String ID = "mbd.dynamic_pattern";
    private final ControllerTileEntity controller;
    public BlockPattern pattern;

    public DynamicPatternEvent(ControllerTileEntity controller, BlockPattern basePattern) {
        this.controller = controller;
        this.pattern = basePattern;
    }

    public ControllerTileEntity getController() {
        return controller;
    }

    public void setPattern(BlockPattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean canCancel() {
        return true;
    }

}
