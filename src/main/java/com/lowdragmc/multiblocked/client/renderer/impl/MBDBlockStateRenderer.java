package com.lowdragmc.multiblocked.client.renderer.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.client.renderer.impl.BlockStateRenderer;
import com.lowdragmc.lowdraglib.gui.widget.BlockSelectorWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import com.lowdragmc.multiblocked.client.renderer.IMultiblockedRenderer;
import net.minecraft.block.BlockState;

import java.util.function.Supplier;

/**
 * Author: KilaBash
 * Date: 2022/04/24
 * Description:
 */
public class MBDBlockStateRenderer extends BlockStateRenderer implements IMultiblockedRenderer {

    public static final IMultiblockedRenderer INSTANCE = new MBDBlockStateRenderer();

    private MBDBlockStateRenderer() {
        super((BlockInfo) null);
    }

    public MBDBlockStateRenderer(BlockState state) {
        super(state);
    }

    public MBDBlockStateRenderer(BlockInfo blockInfo) {
        super(blockInfo);
    }

    @Override
    public String getType() {
        return "blockstate";
    }

    @Override
    public IMultiblockedRenderer fromJson(Gson gson, JsonObject jsonObject) {
        return new MBDBlockStateRenderer(gson.fromJson(jsonObject.get("state"), BlockState.class));
    }

    @Override
    public JsonObject toJson(Gson gson, JsonObject jsonObject) {
        jsonObject.add("state", gson.toJsonTree(blockInfo.getBlockState(), BlockState.class));
        return jsonObject;
    }

    @Override
    public Supplier<IMultiblockedRenderer> createConfigurator(WidgetGroup parent, DraggableScrollableWidgetGroup group, IMultiblockedRenderer current) {
        BlockSelectorWidget blockSelectorWidget = new BlockSelectorWidget(0, 1, true);
        if (current instanceof BlockStateRenderer) {
            blockSelectorWidget.setBlock(((BlockStateRenderer) current).blockInfo.getBlockState());
        }
        group.addWidget(blockSelectorWidget);
        return () -> {
            if (blockSelectorWidget.getBlock() == null) {
                return null;
            } else {
                return new MBDBlockStateRenderer(blockSelectorWidget.getBlock());
            }
        };
    }
}
