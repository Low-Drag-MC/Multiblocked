package com.lowdragmc.multiblocked.client.renderer.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.client.model.ModelFactory;
import com.lowdragmc.lowdraglib.client.renderer.impl.IModelRenderer;
import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import com.lowdragmc.multiblocked.client.renderer.IMultiblockedRenderer;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.EnumMap;
import java.util.Map;

/**
 * Author: KilaBash
 * Date: 2022/04/24
 * Description:
 */
public class MBDIModelRenderer extends IModelRenderer implements IMultiblockedRenderer {
    public static final IMultiblockedRenderer INSTANCE = new MBDIModelRenderer();

    @OnlyIn(Dist.CLIENT)
    protected Map<Direction, IBakedModel> blockModels;

    protected MBDIModelRenderer() {
        super();
    }

    public MBDIModelRenderer(ResourceLocation modelLocation) {
        super(modelLocation);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            blockModels = new EnumMap<>(Direction.class);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected IBakedModel getBlockBakedModel(BlockPos pos, IBlockDisplayReader blockAccess) {
        TileEntity tileEntity = blockAccess.getBlockEntity(pos);
        Direction frontFacing = Direction.NORTH;
        if (tileEntity instanceof ComponentTileEntity) {
            frontFacing = ((ComponentTileEntity<?>) tileEntity).getFrontFacing();
        }
        return blockModels.computeIfAbsent(frontFacing, facing -> getModel().bake(
                ModelLoader.instance(),
                ModelLoader.defaultTextureGetter(),
                ModelFactory.getRotation(facing),
                modelLocation));
    }

    @Override
    public void onTextureSwitchEvent(TextureStitchEvent.Pre event) {
        blockModels.clear();
        super.onTextureSwitchEvent(event);
    }

    @Override
    public String getType() {
        return "imodel";
    }

    @Override
    public IMultiblockedRenderer fromJson(Gson gson, JsonObject jsonObject) {
        return new MBDIModelRenderer(gson.fromJson(jsonObject.get("modelLocation"), ResourceLocation.class));
    }

    @Override
    public JsonObject toJson(Gson gson, JsonObject jsonObject) {
        jsonObject.add("modelLocation", gson.toJsonTree(modelLocation, ResourceLocation.class));
        return jsonObject;
    }
}
