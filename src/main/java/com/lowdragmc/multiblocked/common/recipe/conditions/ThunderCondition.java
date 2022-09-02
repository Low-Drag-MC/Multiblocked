package com.lowdragmc.multiblocked.common.recipe.conditions;

import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.multiblocked.api.recipe.Recipe;
import com.lowdragmc.multiblocked.api.recipe.RecipeCondition;
import com.lowdragmc.multiblocked.api.recipe.RecipeLogic;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

/**
 * @author KilaBash
 * @date 2022/05/27
 * @implNote WhetherCondition, specific whether
 */
public class ThunderCondition extends RecipeCondition {

    public final static ThunderCondition INSTANCE = new ThunderCondition();
    private float level;

    private ThunderCondition() {}

    public ThunderCondition(float level) {
        this.level = level;
    }

    @Override
    public String getType() {
        return "thunder";
    }

    @Override
    public Component getTooltips() {
        return new TranslatableComponent("multiblocked.recipe.condition.thunder.tooltip", level);
    }

    @Override
    public boolean test(@Nonnull Recipe recipe, @Nonnull RecipeLogic recipeLogic) {
        Level level = recipeLogic.controller.self().getLevel();
        return level != null && level.getThunderLevel(1) >= this.level;
    }

    @Override
    public RecipeCondition createTemplate() {
        return new ThunderCondition();
    }

    @Nonnull
    @Override
    public JsonObject serialize() {
        JsonObject config = super.serialize();
        config.addProperty("level", level);
        return config;
    }

    @Override
    public RecipeCondition deserialize(@Nonnull JsonObject config) {
        super.deserialize(config);
        level = GsonHelper.getAsFloat(config, "level", 0);
        return this;
    }

    @Override
    public RecipeCondition fromNetwork(FriendlyByteBuf buf) {
        super.fromNetwork(buf);
        level = buf.readFloat();
        return this;
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf) {
        super.toNetwork(buf);
        buf.writeFloat(level);
    }

    @Override
    public void openConfigurator(WidgetGroup group) {
        super.openConfigurator(group);
        group.addWidget(new TextFieldWidget(0, 20, 60, 15, null, s->level = Float.parseFloat(s))
                .setCurrentString(level + "")
                .setNumbersOnly(0f, 1f)
                .setHoverTooltips("multiblocked.gui.condition.thunder.level"));
    }
}
