package com.lowdragmc.multiblocked.client.renderer.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.client.model.ModelFactory;
import com.lowdragmc.lowdraglib.client.model.custommodel.CustomBakedModel;
import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.gui.dialogs.ResourceTextureWidget;
import com.lowdragmc.multiblocked.api.tile.ControllerTileEntity;
import com.lowdragmc.multiblocked.api.tile.part.PartTileEntity;
import com.lowdragmc.multiblocked.client.renderer.IMultiblockedRenderer;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.BlockModel;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.data.IModelData;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;


public class GTRenderer extends MBDIModelRenderer {
    public final static GTRenderer INSTANCE = new GTRenderer();

    public ResourceLocation baseTexture = new ResourceLocation("multiblocked:blocks/gregtech_base");
    public ResourceLocation frontOverlay = new ResourceLocation("multiblocked:blocks/gregtech_front");

    public ResourceLocation backOverlay;
    public ResourceLocation leftOverlay;
    public ResourceLocation rightOverlay;
    public ResourceLocation upOverlay;
    public ResourceLocation downOverlay;

    public boolean formedAsController;

    private GTRenderer() {

    }

    public GTRenderer(ResourceLocation baseTexture, ResourceLocation... frontOverlay) {
        super();
        this.baseTexture = baseTexture;
        if (frontOverlay.length > 0) {
            this.frontOverlay = frontOverlay[0];
        }
        if (frontOverlay.length > 1) {
            this.backOverlay = frontOverlay[1];
        }
        if (frontOverlay.length > 2) {
            this.leftOverlay = frontOverlay[2];
        }
        if (frontOverlay.length > 3) {
            this.rightOverlay = frontOverlay[3];
        }
        if (frontOverlay.length > 4) {
            this.upOverlay = frontOverlay[4];
        }
        if (frontOverlay.length > 5) {
            this.downOverlay = frontOverlay[5];
        }
        if (Multiblocked.isClient()) {
            registerTextureSwitchEvent();
            blockModels = new EnumMap<>(Direction.class);
        }
    }

    @Nonnull
    @Override
    @OnlyIn(Dist.CLIENT)
    public TextureAtlasSprite getParticleTexture() {
        return Minecraft.getInstance().getTextureAtlas(AtlasTexture.LOCATION_BLOCKS).apply(baseTexture);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public String getType() {
        return "gregtech";
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean isRaw() {
        return false;
    }

    @Override
    public List<BakedQuad> renderModel(IBlockDisplayReader level, BlockPos pos, BlockState state, Direction side, Random rand, IModelData modelData) {
        TileEntity te = level.getBlockEntity(pos);
        if (formedAsController && te instanceof PartTileEntity) {
            PartTileEntity<?> part = (PartTileEntity<?>) te;
            for (ControllerTileEntity controller : part.getControllers()) {
                if (controller.isFormed() && controller.getRenderer() instanceof GTRenderer) {
                    IBakedModel model = getModel(((GTRenderer) controller.getRenderer()).baseTexture).bake(
                            ModelLoader.instance(),
                            ModelLoader.defaultTextureGetter(),
                            ModelFactory.getRotation(part.getFrontFacing()),
                            modelLocation);
                    if (model == null) return Collections.emptyList();
                    model = new CustomBakedModel(model);
                    if (!((CustomBakedModel)model).shouldRenderInLayer(state, rand)) return Collections.emptyList();
                    return model.getQuads(state, side, rand, modelData);
                }
            }
        }
        return super.renderModel(level, pos, state, side, rand, modelData);

    }


    @OnlyIn(Dist.CLIENT)
    protected IUnbakedModel getModel(ResourceLocation baseTexture) {
        IUnbakedModel model = ModelFactory.getUnBakedModel(new ResourceLocation(Multiblocked.MODID, "block/cube_2_layer"));
        if (model instanceof BlockModel) {
            ((BlockModel) model).textureMap.put("bot_down", ModelFactory.parseBlockTextureLocationOrReference(baseTexture.toString()));
            ((BlockModel) model).textureMap.put("bot_up", ModelFactory.parseBlockTextureLocationOrReference(baseTexture.toString()));
            ((BlockModel) model).textureMap.put("bot_north", ModelFactory.parseBlockTextureLocationOrReference(baseTexture.toString()));
            ((BlockModel) model).textureMap.put("bot_south", ModelFactory.parseBlockTextureLocationOrReference(baseTexture.toString()));
            ((BlockModel) model).textureMap.put("bot_west", ModelFactory.parseBlockTextureLocationOrReference(baseTexture.toString()));
            ((BlockModel) model).textureMap.put("bot_east", ModelFactory.parseBlockTextureLocationOrReference(baseTexture.toString()));
            ((BlockModel) model).textureMap.put("top_north", ModelFactory.parseBlockTextureLocationOrReference(frontOverlay.toString()));
            if (backOverlay != null) {
                ((BlockModel) model).textureMap.put("top_south", ModelFactory.parseBlockTextureLocationOrReference(backOverlay.toString()));
            }
            if (leftOverlay != null) {
                ((BlockModel) model).textureMap.put("top_west", ModelFactory.parseBlockTextureLocationOrReference(leftOverlay.toString()));
            }
            if (rightOverlay != null) {
                ((BlockModel) model).textureMap.put("top_east", ModelFactory.parseBlockTextureLocationOrReference(rightOverlay.toString()));
            }
            if (upOverlay != null) {
                ((BlockModel) model).textureMap.put("top_up", ModelFactory.parseBlockTextureLocationOrReference(upOverlay.toString()));
            }
            if (downOverlay != null) {
                ((BlockModel) model).textureMap.put("top_down", ModelFactory.parseBlockTextureLocationOrReference(downOverlay.toString()));
            }
        }
        return model;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected IUnbakedModel getModel() {
        return getModel(baseTexture);
    }

    @Override
    public JsonObject toJson(Gson gson, JsonObject jsonObject) {
        jsonObject.add("baseTexture", gson.toJsonTree(baseTexture, ResourceLocation.class));
        jsonObject.add("frontTexture", gson.toJsonTree(frontOverlay, ResourceLocation.class));
        if (backOverlay != null) {
            jsonObject.add("backTexture", gson.toJsonTree(backOverlay, ResourceLocation.class) );
        }
        if (leftOverlay != null) {
            jsonObject.add("leftTexture", gson.toJsonTree(leftOverlay, ResourceLocation.class) );
        }
        if (rightOverlay != null) {
            jsonObject.add("rightTexture", gson.toJsonTree(rightOverlay, ResourceLocation.class) );
        }
        if (upOverlay != null) {
            jsonObject.add("upTexture", gson.toJsonTree(upOverlay, ResourceLocation.class) );
        }
        if (downOverlay != null) {
            jsonObject.add("downTexture", gson.toJsonTree(downOverlay, ResourceLocation.class) );
        }
        if (formedAsController) {
            jsonObject.addProperty("formedAsController", true);
        }
        return jsonObject;
    }

    @Override
    public IMultiblockedRenderer fromJson(Gson gson, JsonObject jsonObject) {
        GTRenderer renderer =  new GTRenderer(gson.fromJson(jsonObject.get("baseTexture"), ResourceLocation.class), gson.fromJson(jsonObject.get("frontTexture"), ResourceLocation.class));
        if (jsonObject.has("backTexture")) {
            renderer.backOverlay = gson.fromJson(jsonObject.get("backTexture"), ResourceLocation.class);
        }
        if (jsonObject.has("leftTexture")) {
            renderer.leftOverlay = gson.fromJson(jsonObject.get("leftTexture"), ResourceLocation.class);
        }
        if (jsonObject.has("rightTexture")) {
            renderer.rightOverlay = gson.fromJson(jsonObject.get("rightTexture"), ResourceLocation.class);
        }
        if (jsonObject.has("upTexture")) {
            renderer.upOverlay = gson.fromJson(jsonObject.get("upTexture"), ResourceLocation.class);
        }
        if (jsonObject.has("downTexture")) {
            renderer.downOverlay = gson.fromJson(jsonObject.get("downTexture"), ResourceLocation.class);
        }
        if (jsonObject.has("formedAsController")) {
            renderer.formedAsController = jsonObject.get("formedAsController").getAsBoolean();
        }
        return renderer;
    }

    @Override
    public Supplier<IMultiblockedRenderer> createConfigurator(WidgetGroup parent, DraggableScrollableWidgetGroup group, IMultiblockedRenderer current) {
        GTRenderer renderer = new GTRenderer();
        if (current instanceof GTRenderer) {
            renderer.formedAsController = ((GTRenderer) current).formedAsController;
            renderer.baseTexture = ((GTRenderer) current).baseTexture;
            renderer.frontOverlay = ((GTRenderer) current).frontOverlay;
            renderer.backOverlay = ((GTRenderer) current).backOverlay;
            renderer.leftOverlay = ((GTRenderer) current).leftOverlay;
            renderer.rightOverlay = ((GTRenderer) current).rightOverlay;
            renderer.upOverlay = ((GTRenderer) current).upOverlay;
            renderer.downOverlay = ((GTRenderer) current).downOverlay;
        }
        addTextureSelector(1, 1, 60, 60, "base texture", parent, group, renderer.baseTexture, r -> renderer.baseTexture = r);
        addTextureSelector(1 + 64, 1, 30, 30, "front overlay", parent, group, renderer.frontOverlay, r -> renderer.frontOverlay = r);
        addTextureSelector(1 + 64 + 34, 1, 30, 30, "back overlay", parent, group, renderer.backOverlay, r -> renderer.backOverlay = r);
        addTextureSelector(1 + 64 + 34 * 2, 1, 30, 30, "left overlay", parent, group, renderer.leftOverlay, r -> renderer.leftOverlay = r);
        addTextureSelector(1 + 64, 34, 30, 30, "right overlay", parent, group, renderer.rightOverlay, r -> renderer.rightOverlay = r);
        addTextureSelector(1 + 64 + 34, 34, 30, 30, "up overlay", parent, group, renderer.upOverlay, r -> renderer.upOverlay = r);
        addTextureSelector(1 + 64 + 34 * 2, 34, 30, 30, "down overlay", parent, group, renderer.downOverlay, r -> renderer.downOverlay = r);

        group.addWidget(createBoolSwitch(1, 70, "formedAsController", "When the multi formed, if its true and the controller also uses the GregTech Model, it will change the base texture to the controller’s base texture.", renderer.formedAsController, r -> renderer.formedAsController = r));
        return () -> {
            GTRenderer result = new GTRenderer(renderer.baseTexture, renderer.frontOverlay);
            result.backOverlay = renderer.backOverlay;
            result.leftOverlay = renderer.leftOverlay;
            result.rightOverlay = renderer.rightOverlay;
            result.upOverlay = renderer.upOverlay;
            result.downOverlay = renderer.downOverlay;
            result.formedAsController = renderer.formedAsController;
            return result;
        };
    }

    protected void addTextureSelector(int x, int y, int width, int height, String text, WidgetGroup parent, WidgetGroup group, ResourceLocation init, Consumer<ResourceLocation> newTexture) {
        ImageWidget imageWidget;
        if (init != null) {
            imageWidget = new ImageWidget(x, y, width, height, new GuiTextureGroup(new ColorBorderTexture(1, -1), new ResourceTexture(init.getNamespace() + ":textures/" + init.getPath() + ".png")));
        } else {
            imageWidget = new ImageWidget(x, y, width, height, new ColorBorderTexture(1, -1));
        }
        group.addWidget(imageWidget);
        group.addWidget(new ButtonWidget(x, y, width, height, null, cd -> new ResourceTextureWidget(parent, texture -> {
            if (texture != null) {
                imageWidget.setImage(new GuiTextureGroup(new ColorBorderTexture(1, -1), texture));
                newTexture.accept(new ResourceLocation(texture.imageLocation.toString().replace("textures/", "").replace(".png", "")));
            }
        })).setHoverTexture(new ColorRectTexture(0x4faaaaaa)).setHoverTooltips(String.format("select the %s texture", text)));
    }
}
