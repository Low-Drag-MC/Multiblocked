package com.lowdragmc.multiblocked.api.gui.recipe;

import com.google.common.collect.Lists;
import com.lowdragmc.lowdraglib.gui.ingredient.IRecipeIngredientSlot;
import com.lowdragmc.lowdraglib.gui.ingredient.Target;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DialogWidget;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.SwitchWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.SelectableWidgetGroup;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.mojang.blaze3d.vertex.PoseStack;
import com.lowdragmc.multiblocked.api.recipe.Content;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.Rect2i;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public abstract class ContentWidget<T> extends SelectableWidgetGroup implements IRecipeIngredientSlot {
    protected T content;
    protected float chance;
    protected IO io;
    protected String slotName;
    protected boolean perTick;
    protected IGuiTexture background;
    protected Consumer<ContentWidget<T>> onPhantomUpdate;
    protected Consumer<ContentWidget<T>> onMouseClicked;

    public ContentWidget() {
        super(0, 0, 20, 20);
        setClientSideWidget();
    }

    public ContentWidget<T> setSelfPosition(int x, int y) {
        setSelfPosition(new Position(x, y));
        return this;
    }

    @SuppressWarnings("unchecked")
    @Deprecated
    public final ContentWidget<T> setContent(@Nonnull IO io, @Nonnull Object content, float chance, boolean perTick) {
        this.io = io;
        this.content = (T) content;
        this.chance = chance;
        this.perTick = perTick;
        this.slotName = null;
        onContentUpdate();
        return this;
    }

    @SuppressWarnings("unchecked")
    public final ContentWidget<T> setContent(@Nonnull IO io, Content content, boolean perTick) {
        this.io = io;
        this.content = (T) content.content;
        this.chance = content.chance;
        this.perTick = perTick;
        this.slotName = content.slotName;
        onContentUpdate();
        return this;
    }

    @Override
    public List<Target> getPhantomTargets(Object ingredient) {
        List<Target> pattern = super.getPhantomTargets(ingredient);
        if (pattern != null && pattern.size() > 0) return pattern;
        Object ingredientContent = getJEIIngredient(getContent());
        if (ingredientContent == null || !ingredient.getClass().equals(ingredientContent.getClass())) {
            return Collections.emptyList();
        }
        Rect2i rectangle = toRectangleBox();
        return Lists.newArrayList(new Target() {
            @Nonnull
            @Override
            public Rect2i getArea() {
                return rectangle;
            }

            @Override
            public void accept(@Nonnull Object ingredient) {
                Object ingredientContent = getJEIIngredient(getContent());
                if (ingredientContent != null && ingredient.getClass().equals(ingredientContent.getClass())) {
                    T content = getJEIContent(ingredient);
                    if (content != null) {
                        setContent(io, content, chance, perTick);
                        if (onPhantomUpdate != null) {
                            onPhantomUpdate.accept(ContentWidget.this);
                        }
                    }
                }
            }
        });
    }

    @Override
    public Object getIngredientOverMouse(double mouseX, double mouseY) {
        Object result = super.getIngredientOverMouse(mouseX, mouseY);
        if (result != null) return  result;
        if (isMouseOverElement(mouseX, mouseY)) {
            return getJEIIngredient(getContent());
        }
        return null;
    }

    /**
     * get the content from a JEI ingredient
     * @return content
     */
    @SuppressWarnings("unchecked")
    public T getJEIContent(Object content) {
        return (T)content;
    }

    /**
     * get the content's ingredient form in JEI
     * @return ingredient
     */
    @Nullable
    public Object getJEIIngredient(T content) {
        return content;
    }

    @Override
    public Object getJEIIngredient() {
        return getJEIIngredient(content);
    }

    public IO getIo() {
        return io;
    }

    public @NotNull T getContent() {
        return content;
    }

    public float getChance() {
        return chance;
    }

    public String getSlotName() {
        return slotName;
    }

    public boolean getPerTick() {
        return perTick;
    }

    @Override
    public int getPosX() {
        return getPosition().x;
    }

    @Override
    public int getPosY() {
        return getPosition().y;
    }

    @Override
    public IngredientIO getIngredientIo() {
        if (io == IO.IN){
            return chance == 0 ?  IngredientIO.CATALYST : IngredientIO.INPUT;
        }
        if (io == IO.OUT){
            return IngredientIO.OUTPUT;
        }

       return IngredientIO.RENDER_ONLY;
    }

    public ContentWidget<T> setOnMouseClicked(Consumer<ContentWidget<T>> onMouseClicked) {
        this.onMouseClicked = onMouseClicked;
        return this;
    }

    public ContentWidget<T> setOnPhantomUpdate(Consumer<ContentWidget<T>> onPhantomUpdate) {
        this.onPhantomUpdate = onPhantomUpdate;
        return this;
    }

    @Override
    public boolean allowSelected(double mouseX, double mouseY, int button) {
        return onSelected != null && isMouseOverElement(mouseX, mouseY);
    }

    @Override
    public void onUnSelected() {
        isSelected = false;
    }

    protected abstract void onContentUpdate();

    /**
     * Configurator.
     */
    public void openConfigurator(WidgetGroup dialog){
        dialog.addWidget(new LabelWidget(5, 8, "multiblocked.gui.label.chance"));
        dialog.addWidget(new TextFieldWidget(125 - 60, 5, 30, 15, null, number -> setContent(io, content, Float.parseFloat(number), perTick)).setNumbersOnly(0f, 1f).setCurrentString(chance + ""));
        dialog.addWidget(new ButtonWidget(125 - 25 , 5, 15, 15, new ResourceTexture("multiblocked:textures/gui/option.png"), cd -> {
            DialogWidget dialogWidget = new DialogWidget(dialog, true);
            dialogWidget
                    .addWidget(new ImageWidget(0, 0, dialog.getSize().width, dialog.getSize().height, ResourceBorderTexture.BORDERED_BACKGROUND))
                    .addWidget(new LabelWidget(25, 8, "perTick"))
                    .addWidget(new SwitchWidget(5, 5, 15, 15, (x, r) -> setContent(io, content, chance, r))
                            .setBaseTexture(new ResourceTexture("multiblocked:textures/gui/boolean.png").getSubTexture(0,0,1,0.5))
                            .setPressedTexture(new ResourceTexture("multiblocked:textures/gui/boolean.png").getSubTexture(0,0.5,1,0.5))
                            .setHoverBorderTexture(1, -1)
                            .setPressed(perTick)
                            .setHoverTooltips("multiblocked.gui.content.per_tick"))
                    .addWidget(new TextFieldWidget(5, 25, dialog.getSize().width - 10, 15, null, s -> setContent(io, new Content(content, chance, s != null && !s.isEmpty() ? s : null), perTick))
                            .setCurrentString(slotName == null ? "" : slotName)
                            .setHoverTooltips("multiblocked.gui.content.slot_name"))
                    .addWidget(new ButtonWidget(5, dialog.getSize().height - 20, dialog.getSize().width - 10, 15, null, c -> dialogWidget.close()).setButtonTexture(ResourceBorderTexture.BUTTON_COMMON, new TextTexture("multiblocked.gui.content.back")));
        }).setHoverTooltips("multiblocked.gui.content.more_option"));
    }

    public ContentWidget<T> setBackground(IGuiTexture background) {
        this.background = background;
        return this;
    }

    @Override
    public ContentWidget<T> addWidget(Widget widget) {
        super.addWidget(widget);
        return this;
    }

    public ContentWidget<T> setHoverTooltips(String... tooltipTexts) {
        if (chance < 1) {
            tooltipTexts = ArrayUtils.add(tooltipTexts, chance == 0 ?
                    LocalizationUtils.format("multiblocked.gui.content.chance_0") :
                    LocalizationUtils.format("multiblocked.gui.content.chance_1", String.format("%.1f", chance * 100) + "%%"));
        }
        if (perTick) {
            tooltipTexts = ArrayUtils.add(tooltipTexts, LocalizationUtils.format("multiblocked.gui.content.per_tick"));
        }
        super.setHoverTooltips(tooltipTexts);
        return this;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (background != null) {
            background.updateTick();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOverElement(mouseX, mouseY) && onMouseClicked != null) onMouseClicked.accept(this);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public final void drawInBackground(@Nonnull PoseStack stack, int mouseX, int mouseY, float partialTicks) {
        Position position = getPosition();
        Size size = getSize();
        if (background != null) {
            background.draw(stack, mouseX, mouseY, position.x, position.y, size.width, size.height);
        }
        drawHookBackground(stack, mouseX, mouseY, partialTicks);
        super.drawInBackground(stack, mouseX, mouseY, partialTicks);
        drawChance(stack);
        drawTick(stack);
        drawHoverOverlay(stack, mouseX, mouseY);
        if (isSelected) {
            DrawerHelper.drawBorder(stack, getPosition().x, getPosition().y, getSize().width, getSize().height, 0xff00aa00, 1);
        }
    }

    protected void drawHookBackground(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {

    }


    @OnlyIn(Dist.CLIENT)
    public void drawChance(PoseStack matrixStack) {
        if (chance == 1) return;
        Position pos = getPosition();
        Size size = getSize();
        matrixStack.pushPose();
        matrixStack.translate(0 ,0 , 170);
        matrixStack.scale(0.5f, 0.5f, 1);
        String s = chance == 0 ? LocalizationUtils.format("multiblocked.gui.content.chance_0_short") : String.format("%.1f", chance * 100) + "%";
        int color = chance == 0 ? 0xff0000 : 0xFFFF00;
        Font fontRenderer = Minecraft.getInstance().font;
        fontRenderer.drawShadow(matrixStack, s, (pos.x + (size.width / 3f)) * 2 - fontRenderer.width(s) + 23, (pos.y + (size.height / 3f) + 6) * 2 - size.height, color);
        matrixStack.popPose();
    }

    @OnlyIn(Dist.CLIENT)
    public void drawTick(PoseStack matrixStack) {
        if (perTick) {
            Position pos = getPosition();
            Size size = getSize();
            matrixStack.pushPose();
            RenderSystem.disableDepthTest();
            matrixStack.translate(0 ,0 , 400);
            matrixStack.scale(0.5f, 0.5f, 1);
            String s = LocalizationUtils.format("multiblocked.gui.content.tips.per_tick_short");
            int color = 0xFFFF00;
            Font fontRenderer = Minecraft.getInstance().font;
            fontRenderer.drawShadow(matrixStack, s, (pos.x + (size.width / 3f)) * 2 - fontRenderer.width(s) + 23, (pos.y + (size.height / 3f) + 6) * 2 - size.height + (chance == 1 ? 0 : 10), color);
            matrixStack.popPose();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void drawHoverOverlay(PoseStack matrixStack, int mouseX, int mouseY) {
        if (isMouseOverElement(mouseX, mouseY)) {
            RenderSystem.colorMask(true, true, true, false);
            DrawerHelper.drawSolidRect(matrixStack, getPosition().x + 1, getPosition().y + 1, 18, 18, -2130706433);
            RenderSystem.colorMask(true, true, true, true);
        }
    }

}
