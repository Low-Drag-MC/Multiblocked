package com.lowdragmc.multiblocked.client.renderer.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.client.renderer.impl.BlockStateRenderer;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import com.lowdragmc.multiblocked.client.renderer.IMultiblockedRenderer;
import net.minecraft.block.BlockState;

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
        jsonObject.add("state", gson.toJsonTree(getState(), BlockState.class));
        return jsonObject;
    }
    
}
