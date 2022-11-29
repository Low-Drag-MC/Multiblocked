package com.lowdragmc.multiblocked.api.gui.controller;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.gui.recipe.RecipeWidget;
import com.lowdragmc.multiblocked.api.kubejs.events.RecipeUIEvent;
import com.lowdragmc.multiblocked.api.recipe.Recipe;
import com.lowdragmc.multiblocked.api.recipe.RecipeLogic;
import com.lowdragmc.multiblocked.api.recipe.RecipeMap;
import com.lowdragmc.multiblocked.api.tile.ControllerTileEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import dev.latvian.kubejs.script.ScriptType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class RecipePage extends PageWidget{
    public static ResourceTexture resourceTexture = new ResourceTexture("multiblocked:textures/gui/recipe_page.png");
    public final ControllerTileEntity controller;
    public final DraggableScrollableWidgetGroup tips;
    private Recipe recipe;
    @OnlyIn(Dist.CLIENT)
    private RecipeWidget recipeWidget;
    private RecipeLogic.Status status;
    private int progress, fuelTime;
    
    public RecipePage(ControllerTileEntity controller, TabContainer tabContainer) {
        super(resourceTexture, tabContainer);
        this.controller = controller;
        this.status = RecipeLogic.Status.IDLE;
        this.addWidget(tips = new DraggableScrollableWidgetGroup(8, 34, 160, 112));
        tips.addWidget(new LabelWidget(5, 5, () -> I18n.get("multiblocked.recipe.status." + status.name)).setTextColor(-1));
        tips.addWidget(new LabelWidget(5, 20, () -> I18n.get("multiblocked.recipe.remaining", recipe == null ? 0 : (recipe.duration - progress) / 20)).setTextColor(-1));
        this.addWidget(new SwitchWidget(153, 131, 12, 12, (cd, r) -> {
            controller.asyncRecipeSearching = r;
            if (!cd.isRemote) {
                controller.markAsDirty();
            }
        })
                .setPressed(controller.asyncRecipeSearching)
                .setSupplier(() -> controller.asyncRecipeSearching)
                .setTexture(resourceTexture.getSubTexture(176 / 256.0, 143 / 256.0, 12 / 256.0, 12 / 256.0),
                        resourceTexture.getSubTexture(176 / 256.0, 155 / 256.0, 12 / 256.0, 12 / 256.0))
                .setHoverTooltips("Async/Sync recipes searching:",
                        "Async has better performance and only tries to match recipes when the internal contents changed",
                        "Sync always tries to match recipes, never miss matching recipes"));
        if (controller.getDefinition().getRecipeMap().isFuelRecipeMap()) {
            tips.addWidget(new LabelWidget(5, 35, () -> (status == RecipeLogic.Status.SUSPEND && fuelTime == 0) ? I18n.get("multiblocked.recipe.lack_fuel") : I18n.get("multiblocked.recipe.remaining_fuel", fuelTime / 20)).setTextColor(-1));
        }
        this.addWidget(new ImageWidget(7, 7, 162, 16,
                new TextTexture(controller.getUnlocalizedName(), -1)
                        .setType(TextTexture.TextType.ROLL)
                        .setWidth(162)
                        .setDropShadow(true)));
        this.addWidget(new ProgressWidget(this::getProgress, 17, 154, 143, 9).setProgressBar(
                new IGuiTexture() {
                    @OnlyIn(Dist.CLIENT)
                    @Override
                    public void draw(MatrixStack stack, int mouseX, int mouseY, float x, float y, int width, int height) {
                        float imageU = 185f / 256;
                        float imageV = 0;
                        float imageWidth = 9f / 256;
                        float imageHeight = 143f / 256;
                        Minecraft.getInstance().textureManager.bind(resourceTexture.imageLocation);
                        Tessellator tessellator = Tessellator.getInstance();
                        BufferBuilder bufferbuilder = tessellator.getBuilder();
                        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
                        Matrix4f mat = stack.last().pose();
                        bufferbuilder.vertex(mat, x, y + height, 0).uv(imageU + imageWidth, imageV + imageHeight).endVertex();
                        bufferbuilder.vertex(mat, x + width, y + height, 0).uv(imageU + imageWidth, imageV).endVertex();
                        bufferbuilder.vertex(mat, x + width, y, 0).uv(imageU, imageV).endVertex();
                        bufferbuilder.vertex(mat, x, y, 0).uv(imageU, imageV + imageHeight).endVertex();
                        tessellator.end();
                    }
                }, new IGuiTexture() {
                    @OnlyIn(Dist.CLIENT)
                    @Override
                    public void draw(MatrixStack stack, int mouseX, int mouseY, float x, float y, int width, int height) {

                    }

                    @OnlyIn(Dist.CLIENT)
                    @Override
                    public void drawSubArea(MatrixStack stack, float x, float y, int width, int height, float drawnU, float drawnV, float drawnWidth, float drawnHeight) {
                        float imageU = 176f / 256;
                        float imageV = 0;
                        float imageWidth = 9f / 256;
                        float imageHeight = 143f / 256;
                        Minecraft.getInstance().textureManager.bind(resourceTexture.imageLocation);
                        Tessellator tessellator = Tessellator.getInstance();
                        BufferBuilder bufferbuilder = tessellator.getBuilder();
                        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
                        Matrix4f mat = stack.last().pose();
                        bufferbuilder.vertex(mat, x, y + height, 0).uv(imageU + imageWidth, imageV + imageHeight).endVertex();
                        bufferbuilder.vertex(mat, x + width, y + height, 0).uv(imageU + imageWidth,  imageV + imageHeight - drawnWidth * imageHeight).endVertex();
                        bufferbuilder.vertex(mat, x + width, y, 0).uv(imageU, imageV + imageHeight - drawnWidth * imageHeight).endVertex();
                        bufferbuilder.vertex(mat, x, y, 0).uv(imageU, imageV + imageHeight).endVertex();
                        tessellator.end();
                    }
                }));
    }

    private double getProgress() {
        if (recipe == null) return 0;
        return progress * 1. / recipe.duration;
    }

    private double getFuelProgress() {
        return Math.min(fuelTime, controller.getDefinition().getRecipeMap().fuelThreshold) * 1d / controller.getDefinition().getRecipeMap().fuelThreshold;
    }

    @Override
    public void writeInitialData(PacketBuffer buffer) {
        super.writeInitialData(buffer);
        detectAndSendChanges();
        writeRecipe(buffer);
        writeStatus(buffer);
    }

    @Override
    public void readInitialData(PacketBuffer buffer) {
        super.readInitialData(buffer);
        readRecipe(buffer);
        readStatus(buffer);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (controller.getRecipeLogic() != null) {
            RecipeLogic recipeLogic = controller.getRecipeLogic();
            if (recipe != recipeLogic.lastRecipe) {
                recipe = recipeLogic.lastRecipe;
                writeUpdateInfo(-1, this::writeRecipe);
            }
            if (status != recipeLogic.getStatus() || progress != recipeLogic.progress || fuelTime != recipeLogic.fuelTime) {
                status = recipeLogic.getStatus();
                progress = recipeLogic.progress;
                fuelTime = recipeLogic.fuelTime;
                writeUpdateInfo(-2, this::writeStatus);
            }
        } else if (recipe != null) {
            recipe = null;
            writeUpdateInfo(-1, this::writeRecipe);
        }
    }

    private void writeStatus(PacketBuffer buffer) {
        buffer.writeEnum(status);
        buffer.writeVarInt(progress);
        buffer.writeVarInt(fuelTime);
    }

    private void readStatus(PacketBuffer buffer) {
        status = buffer.readEnum(RecipeLogic.Status.class);
        progress = buffer.readVarInt();
        fuelTime = buffer.readVarInt();
    }

    private void writeRecipe(PacketBuffer buffer) {
        if (recipe == null) {
            buffer.writeBoolean(false);
        }
        else {
            buffer.writeBoolean(true);
            buffer.writeUtf(recipe.uid);
        }
    }

    private void readRecipe(PacketBuffer buffer) {
        if (buffer.readBoolean()) {
            RecipeMap recipeMap = controller.getDefinition().getRecipeMap();
            recipe = recipeMap.recipes.get(buffer.readUtf());
            if (recipeWidget != null) {
                removeWidget(recipeWidget);
            }
            recipeWidget = new RecipeWidget(
                    recipeMap,
                    recipe,
                    ProgressWidget.JEIProgress,
                    this::getFuelProgress);
            if (Multiblocked.isKubeJSLoaded()) {
                new RecipeUIEvent(recipeWidget).post(ScriptType.CLIENT, RecipeUIEvent.ID, controller.getDefinition().getRecipeMap().name);
            }
            this.addWidget(recipeWidget);
            recipeWidget.inputs.addSelfPosition(5, 0);
            recipeWidget.outputs.addSelfPosition(-5, 0);
            recipeWidget.setSelfPosition(new Position(0, 167));
        } else {
            if (recipeWidget != null) {
                removeWidget(recipeWidget);
            }
            RecipeMap recipeMap = controller.getDefinition().getRecipeMap();
            recipeWidget = new RecipeWidget(
                    recipeMap,
                    null,
                    () -> 0,
                    this::getFuelProgress);
            addWidget(recipeWidget);
            recipeWidget.inputs.addSelfPosition(5, 0);
            recipeWidget.outputs.addSelfPosition(-5, 0);
            recipeWidget.setSelfPosition(new Position(0, 167));
            status = RecipeLogic.Status.IDLE;
            progress = 0;
            fuelTime = 0;
        }
    }

    @Override
    public void readUpdateInfo(int id, PacketBuffer buffer) {
        if (id == -1) {
            readRecipe(buffer);
        } else if (id == -2) {
            readStatus(buffer);
        } else {
            super.readUpdateInfo(id, buffer);
        }
    }
}
