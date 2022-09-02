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

import javax.annotation.Nonnull;

/**
 * @author KilaBash
 * @date 2022/05/27
 * @implNote WhetherCondition, specific whether
 */
public class PositionYCondition extends RecipeCondition {

    public final static PositionYCondition INSTANCE = new PositionYCondition();
    private int min;
    private int max;

    private PositionYCondition() {
    }

    public PositionYCondition(int min, int max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public String getType() {
        return "pos_y";
    }

    @Override
    public Component getTooltips() {
        return new TranslatableComponent("multiblocked.recipe.condition.pos_y.tooltip", this.min, this.max);
    }

    @Override
    public boolean test(@Nonnull Recipe recipe, @Nonnull RecipeLogic recipeLogic) {
        int y = recipeLogic.controller.self().getBlockPos().getY();
        return y >= this.min && y <= this.max;
    }

    @Override
    public RecipeCondition createTemplate() {
        return new PositionYCondition();
    }

    @Nonnull
    @Override
    public JsonObject serialize() {
        JsonObject config = super.serialize();
        config.addProperty("min", this.min);
        config.addProperty("max", this.max);
        return config;
    }

    @Override
    public RecipeCondition deserialize(@Nonnull JsonObject config) {
        super.deserialize(config);
        min = GsonHelper.getAsInt(config, "min", Integer.MIN_VALUE);
        max = GsonHelper.getAsInt(config, "max", Integer.MAX_VALUE);
        return this;
    }

    @Override
    public RecipeCondition fromNetwork(FriendlyByteBuf buf) {
        super.fromNetwork(buf);
        min = buf.readVarInt();
        max = buf.readVarInt();
        return this;
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf) {
        super.toNetwork(buf);
        buf.writeVarInt(min);
        buf.writeVarInt(max);
    }

    @Override
    public void openConfigurator(WidgetGroup group) {
        super.openConfigurator(group);
        group.addWidget(new TextFieldWidget(0, 20, 60, 15, null, s -> min = Integer.parseInt(s))
                .setCurrentString(min + "")
                .setNumbersOnly(Integer.MIN_VALUE, Integer.MAX_VALUE)
                .setHoverTooltips("multiblocked.gui.condition.pos_y.min"));

        group.addWidget(new TextFieldWidget(0, 40, 60, 15, null, s -> max = Integer.parseInt(s))
                .setCurrentString(max + "")
                .setNumbersOnly(Integer.MIN_VALUE, Integer.MAX_VALUE)
                .setHoverTooltips("multiblocked.gui.condition.pos_y.max"));
    }
}
