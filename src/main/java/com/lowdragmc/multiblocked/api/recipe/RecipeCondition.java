package com.lowdragmc.multiblocked.api.recipe;

import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.SwitchWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraft.network.chat.Component;
import net.minecraft.util.GsonHelper;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

/**
 * @author KilaBash
 * @date 2022/05/27
 * @implNote RecipeCondition, global conditions
 */
public abstract class RecipeCondition {

    protected boolean isReverse;

    public abstract String getType();

    public String getTranlationKey() {
        return "multiblocked.recipe.condition." + getType();
    }

    public IGuiTexture getInValidTexture() {
        return new ResourceTexture("multiblocked:textures/gui/condition_" + getType() + ".png").getSubTexture(0,0,1,0.5f);
    }

    public IGuiTexture getValidTexture() {
        return new ResourceTexture("multiblocked:textures/gui/condition_" + getType() + ".png").getSubTexture(0,0.5f,1,0.5f);
    }

    public boolean isReverse() {
        return isReverse;
    }

    public RecipeCondition setReverse(boolean reverse) {
        isReverse = reverse;
        return this;
    }

    public abstract Component getTooltips();

    public abstract boolean test(@Nonnull Recipe recipe, @Nonnull RecipeLogic recipeLogic);

    public abstract RecipeCondition createTemplate();

    @Nonnull
    public JsonObject serialize() {
        JsonObject jsonObject = new JsonObject();
        if (isReverse) {
            jsonObject.addProperty("reverse", true);
        }
        return jsonObject;
    }

    public RecipeCondition deserialize(@Nonnull JsonObject config) {
        isReverse = GsonHelper.getAsBoolean(config, "reverse", false);
        return this;
    }

    @OnlyIn(Dist.CLIENT)
    public void openConfigurator(WidgetGroup group) {
        group.addWidget(new SwitchWidget(0 , 0, 15, 15, (cd, r) -> isReverse = r)
                .setBaseTexture(new ResourceTexture("multiblocked:textures/gui/boolean.png").getSubTexture(0,0,1,0.5))
                .setPressedTexture(new ResourceTexture("multiblocked:textures/gui/boolean.png").getSubTexture(0,0.5,1,0.5))
                .setHoverBorderTexture(1, -1)
                .setPressed(isReverse)
                .setHoverTooltips("multiblocked.gui.condition.reverse"));
        group.addWidget(new LabelWidget(20, 3, "multiblocked.gui.condition.reverse.label").setTextColor(-1).setDrop(true));
    }

}
