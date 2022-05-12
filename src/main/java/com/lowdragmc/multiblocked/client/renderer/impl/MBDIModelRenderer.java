package com.lowdragmc.multiblocked.client.renderer.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.client.model.ModelFactory;
import com.lowdragmc.lowdraglib.client.renderer.impl.IModelRenderer;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DialogWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.multiblocked.Multiblocked;
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

import java.io.File;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

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

    @Override
    public Supplier<IMultiblockedRenderer> createConfigurator(WidgetGroup parent, DraggableScrollableWidgetGroup group, IMultiblockedRenderer current) {
        TextFieldWidget tfw = new TextFieldWidget(1,1,150,20,null, null);
        group.addWidget(tfw);
        File path = new File(Multiblocked.location, "assets/multiblocked/models");
        group.addWidget(new ButtonWidget(155, 1, 20, 20, cd -> DialogWidget.showFileDialog(parent, "select a java model", path, true,
                DialogWidget.suffixFilter(".json"), r -> {
                    if (r != null && r.isFile()) {
                        tfw.setCurrentString("multiblocked:" + r.getPath().replace(path.getPath(), "").substring(1).replace(".json", "").replace('\\', '/'));
                    }
                })).setButtonTexture(new ResourceTexture("multiblocked:textures/gui/darkened_slot.png"), new TextTexture("F", -1)).setHoverTooltips("multiblocked.gui.tips.file_selector"));
        if (current instanceof IModelRenderer && ((IModelRenderer) current).modelLocation != null) {
            tfw.setCurrentString(((IModelRenderer) current).modelLocation.toString());
        }
        return () -> {
            if (tfw.getCurrentString().isEmpty()) {
                return null;
            } else {
                return new MBDIModelRenderer(new ResourceLocation(tfw.getCurrentString()));
            }
        };
    }
}
