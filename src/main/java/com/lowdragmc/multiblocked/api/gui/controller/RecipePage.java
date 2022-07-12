package com.lowdragmc.multiblocked.api.gui.controller;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.SwitchWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.TabContainer;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.gui.recipe.ProgressWidget;
import com.lowdragmc.multiblocked.api.gui.recipe.RecipeWidget;
import com.lowdragmc.multiblocked.api.kubejs.events.RecipeUIEvent;
import com.lowdragmc.multiblocked.api.recipe.Recipe;
import com.lowdragmc.multiblocked.api.recipe.RecipeLogic;
import com.lowdragmc.multiblocked.api.tile.ControllerTileEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.latvian.mods.kubejs.script.ScriptType;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.FriendlyByteBuf;
import com.mojang.math.Matrix4f;
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
    private int progress;
    
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
        this.addWidget(new ImageWidget(7, 7, 162, 16,
                new TextTexture(controller.getUnlocalizedName(), -1)
                        .setType(TextTexture.TextType.ROLL)
                        .setWidth(162)
                        .setDropShadow(true)));
        this.addWidget(new ProgressWidget(this::getProgress, 17, 154, 143, 9).setProgressBar(
                new IGuiTexture() {
                    @OnlyIn(Dist.CLIENT)
                    @Override
                    public void draw(PoseStack stack, int mouseX, int mouseY, float x, float y, int width, int height) {
                        float imageU = 185f / 256;
                        float imageV = 0;
                        float imageWidth = 9f / 256;
                        float imageHeight = 143f / 256;
                        RenderSystem.setShader(GameRenderer::getPositionTexShader);
                        RenderSystem.setShaderTexture(0, resourceTexture.imageLocation);
                        Tesselator tessellator = Tesselator.getInstance();
                        BufferBuilder bufferbuilder = tessellator.getBuilder();
                        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
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
                    public void draw(PoseStack stack, int mouseX, int mouseY, float x, float y, int width, int height) {

                    }

                    @OnlyIn(Dist.CLIENT)
                    @Override
                    public void drawSubArea(PoseStack stack, float x, float y, int width, int height, float drawnU, float drawnV, float drawnWidth, float drawnHeight) {
                        float imageU = 176f / 256;
                        float imageV = 0;
                        float imageWidth = 9f / 256;
                        float imageHeight = 143f / 256;
                        RenderSystem.setShader(GameRenderer::getPositionTexShader);
                        RenderSystem.setShaderTexture(0, resourceTexture.imageLocation);
                        Tesselator tessellator = Tesselator.getInstance();
                        BufferBuilder bufferbuilder = tessellator.getBuilder();
                        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
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

    @Override
    public void writeInitialData(FriendlyByteBuf buffer) {
        super.writeInitialData(buffer);
        detectAndSendChanges();
        writeRecipe(buffer);
        writeStatus(buffer);
    }

    @Override
    public void readInitialData(FriendlyByteBuf buffer) {
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
            if (status != recipeLogic.getStatus() || progress != recipeLogic.progress) {
                status = recipeLogic.getStatus();
                progress = recipeLogic.progress;
                writeUpdateInfo(-2, this::writeStatus);
            }
        } else if (recipe != null) {
            recipe = null;
            writeUpdateInfo(-1, this::writeRecipe);
        }
    }

    private void writeStatus(FriendlyByteBuf buffer) {
        buffer.writeEnum(status);
        buffer.writeVarInt(progress);
    }

    private void readStatus(FriendlyByteBuf buffer) {
        status = buffer.readEnum(RecipeLogic.Status.class);
        progress = buffer.readVarInt();
    }

    private void writeRecipe(FriendlyByteBuf buffer) {
        if (recipe == null) {
            buffer.writeBoolean(false);
        }
        else {
            buffer.writeBoolean(true);
            buffer.writeUtf(recipe.uid);
        }
    }

    private void readRecipe(FriendlyByteBuf buffer) {
        if (buffer.readBoolean()) {
            recipe = controller.getDefinition().recipeMap.recipes.get(buffer.readUtf());
            if (recipeWidget != null) {
                removeWidget(recipeWidget);
            }
            recipeWidget = new RecipeWidget(recipe, controller.getDefinition().recipeMap.progressTexture, null);
            if (Multiblocked.isKubeJSLoaded()) {
                new RecipeUIEvent(recipeWidget).post(ScriptType.CLIENT, RecipeUIEvent.ID, controller.getDefinition().recipeMap.name);
            }
            this.addWidget(recipeWidget);
            recipeWidget.inputs.addSelfPosition(5, 0);
            recipeWidget.outputs.addSelfPosition(-5, 0);
            recipeWidget.setSelfPosition(new Position(0, 167));
        } else {
            if (recipeWidget != null) {
                removeWidget(recipeWidget);
            }
            status = RecipeLogic.Status.IDLE;
            progress = 0;
        }
    }

    @Override
    public void readUpdateInfo(int id, FriendlyByteBuf buffer) {
        if (id == -1) {
            readRecipe(buffer);
        } else if (id == -2) {
            readStatus(buffer);
        } else {
            super.readUpdateInfo(id, buffer);
        }
    }
}
