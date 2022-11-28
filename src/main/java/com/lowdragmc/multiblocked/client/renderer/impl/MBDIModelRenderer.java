package com.lowdragmc.multiblocked.client.renderer.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.client.model.ModelFactory;
import com.lowdragmc.lowdraglib.client.model.custommodel.CustomBakedModel;
import com.lowdragmc.lowdraglib.client.renderer.impl.IModelRenderer;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DialogWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.client.renderer.IMultiblockedRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.ForgeModelBakery;

import javax.annotation.Nullable;
import java.io.File;
import java.util.function.Supplier;

/**
 * Author: KilaBash
 * Date: 2022/04/24
 * Description:
 */
public class MBDIModelRenderer extends IModelRenderer implements IMultiblockedRenderer {
    public static final IMultiblockedRenderer INSTANCE = new MBDIModelRenderer();

    protected MBDIModelRenderer() {
        super();
    }

    public MBDIModelRenderer(ResourceLocation modelLocation) {
        super(modelLocation);
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    protected BakedModel getBlockBakedModel(BlockPos pos, BlockAndTintGetter blockAccess) {
        BlockState blockState = blockAccess.getBlockState(pos);
        Direction frontFacing = Direction.NORTH;
        if (blockState.hasProperty(BlockStateProperties.FACING)) {
            frontFacing = blockState.getValue(BlockStateProperties.FACING);
        } else if (blockState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            frontFacing = blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        return blockModels.computeIfAbsent(frontFacing, facing -> {
            BakedModel model = getModel().bake(
                    ForgeModelBakery.instance(),
                    ForgeModelBakery.defaultTextureGetter(),
                    ModelFactory.getRotation(facing),
                    modelLocation);
            return model == null ? null : new CustomBakedModel(model);
        });
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
