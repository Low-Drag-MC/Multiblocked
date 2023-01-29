package com.lowdragmc.multiblocked.common.recipe.conditions;

import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.widget.SearchComponentWidget;
import com.lowdragmc.lowdraglib.gui.widget.SelectorWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.multiblocked.api.recipe.Recipe;
import com.lowdragmc.multiblocked.api.recipe.RecipeCondition;
import com.lowdragmc.multiblocked.api.recipe.RecipeLogic;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.core.Registry;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author KilaBash
 * @date 2022/05/27
 * @implNote DimensionCondition, specific dimension
 */
public class BiomeCondition extends RecipeCondition {

    public final static BiomeCondition INSTANCE = new BiomeCondition();
    private ResourceLocation biome = new ResourceLocation("dummy");

    private BiomeCondition() {
    }

    public BiomeCondition(ResourceLocation biome) {
        this.biome = biome;
    }

    @Override
    public String getType() {
        return "biome";
    }

    @Override
    public boolean isOr() {
        return true;
    }

    @Override
    public Component getTooltips() {
        return new TranslatableComponent(String.format("biome.%s.%s", biome.getNamespace(), biome.getPath()));
    }

    public ResourceLocation getBiome() {
        return biome;
    }

    @Override
    public boolean test(@Nonnull Recipe recipe, @Nonnull RecipeLogic recipeLogic) {
        Level level = recipeLogic.controller.self().getLevel();
        if (level == null) return false;
        Holder<Biome> biome = level.getBiome(recipeLogic.controller.self().getBlockPos());
        return this.biome.equals(biome.value().getRegistryName());
    }

    @Override
    public RecipeCondition createTemplate() {
        return new BiomeCondition();
    }

    @Nonnull
    @Override
    public JsonObject serialize() {
        JsonObject config = super.serialize();
        config.addProperty("biome", biome.toString());
        return config;
    }

    @Override
    public RecipeCondition deserialize(@Nonnull JsonObject config) {
        super.deserialize(config);
        biome = new ResourceLocation(
                GsonHelper.getAsString(config, "biome", "dummy"));
        return this;
    }

    @Override
    public RecipeCondition fromNetwork(FriendlyByteBuf buf) {
        super.fromNetwork(buf);
        biome = new ResourceLocation(buf.readUtf());
        return this;
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf) {
        super.toNetwork(buf);
        buf.writeUtf(biome.toString());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void openConfigurator(WidgetGroup group) {
        super.openConfigurator(group);
        Set<ResourceLocation> types = Minecraft.getInstance().level.registryAccess().registry(Registry.BIOME_REGISTRY).get().keySet();
        SelectorWidget selectorWidget = new SelectorWidget(0, 20, 80, 15, types.stream().map(ResourceLocation::toString).collect(Collectors.toList()), -1);
        SearchComponentWidget<ResourceLocation> searchComponentWidget = new SearchComponentWidget<>(0, 40, 80, 15, new SearchComponentWidget.IWidgetSearch<>() {
            @Override
            public void search(String word, Consumer<ResourceLocation> find) {
                for (ResourceLocation type : types) {
                    if (type.toString().toLowerCase().contains(word.toLowerCase())) {
                        find.accept(type);
                    }
                }
            }

            @Override
            public String resultDisplay(ResourceLocation value) {
                return value.toString();
            }

            @Override
            public void selectResult(ResourceLocation value) {
                if (value != null) {
                    biome = value;
                    selectorWidget.setValue(value.toString());
                }
            }
        });
        group.addWidget(selectorWidget
                .setButtonBackground(new ColorRectTexture(0x7f2e2e2e))
                .setOnChanged(dim -> {
                    if (dim != null && !dim.isEmpty() && ResourceLocation.isValidResourceLocation(dim)) {
                        biome = new ResourceLocation(dim);
                        searchComponentWidget.setCurrentString(dim);
                    }
                }).setIsUp(true).setValue(types.contains(biome) ? biome.toString() : ""));
        group.addWidget(searchComponentWidget.setCapacity(2).setCurrentString(types.contains(biome) ? biome.toString() : ""));
    }
}
