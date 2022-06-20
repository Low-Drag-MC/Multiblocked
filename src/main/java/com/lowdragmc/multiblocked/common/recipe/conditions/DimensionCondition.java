package com.lowdragmc.multiblocked.common.recipe.conditions;

import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.widget.SelectorWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.multiblocked.api.recipe.Recipe;
import com.lowdragmc.multiblocked.api.recipe.RecipeCondition;
import com.lowdragmc.multiblocked.api.recipe.RecipeLogic;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author KilaBash
 * @date 2022/05/27
 * @implNote DimensionCondition, specific dimension
 */
public class DimensionCondition extends RecipeCondition {

    public final static DimensionCondition INSTANCE = new DimensionCondition();
    private ResourceLocation dimension = new ResourceLocation("dummy");

    private DimensionCondition() {
    }

    public DimensionCondition(ResourceLocation dimension) {
        this.dimension = dimension;
    }

    @Override
    public String getType() {
        return "dimension";
    }

    @Override
    public Component getTooltips() {
        return new TranslatableComponent("multiblocked.recipe.condition.dimension.tooltip", dimension);
    }

    @Override
    public boolean test(@Nonnull Recipe recipe, @Nonnull RecipeLogic recipeLogic) {
        Level level = recipeLogic.controller.getLevel();
        return level != null && dimension.equals(level.dimension().location());
    }

    @Override
    public RecipeCondition createTemplate() {
        return new DimensionCondition();
    }

    @Nonnull
    @Override
    public JsonObject serialize() {
        JsonObject config = super.serialize();
        config.addProperty("dim", dimension.toString());
        return config;
    }

    @Override
    public RecipeCondition deserialize(@Nonnull JsonObject config) {
        super.deserialize(config);
        dimension = new ResourceLocation(
                GsonHelper.getAsString(config, "dim", "dummy"));
        return this;
    }

    @Override
    public RecipeCondition fromNetwork(FriendlyByteBuf buf) {
        super.fromNetwork(buf);
        buf.writeUtf(dimension.toString());
        return this;
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf) {
        super.toNetwork(buf);
        dimension = new ResourceLocation(buf.readUtf());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void openConfigurator(WidgetGroup group) {
        super.openConfigurator(group);
        Set<ResourceLocation> types = Minecraft.getInstance().level.registryAccess().registry(
                Registry.DIMENSION_TYPE_REGISTRY).get().keySet();
        group.addWidget(new SelectorWidget(0, 20, 80, 15, types.stream().map(ResourceLocation::toString).collect(Collectors.toList()), -1)
                .setButtonBackground(new ColorRectTexture(0x7f2e2e2e))
                .setOnChanged(dim -> {
                    if (dim != null && !dim.isEmpty() && ResourceLocation.isValidResourceLocation(dim)) {
                        dimension = new ResourceLocation(dim);
                    }
                }).setValue(types.contains(dimension) ? dimension.toString() : ""));
    }
}
