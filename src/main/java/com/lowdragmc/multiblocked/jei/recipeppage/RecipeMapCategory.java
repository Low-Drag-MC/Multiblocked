package com.lowdragmc.multiblocked.jei.recipeppage;

import com.lowdragmc.lowdraglib.jei.IGui2IDrawable;
import com.lowdragmc.lowdraglib.jei.ModularUIRecipeCategory;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.definition.ControllerDefinition;
import com.lowdragmc.multiblocked.api.gui.recipe.RecipeWidget;
import com.lowdragmc.multiblocked.api.kubejs.events.RecipeUIEvent;
import com.lowdragmc.multiblocked.api.recipe.RecipeMap;
import com.lowdragmc.multiblocked.jei.multipage.MultiblockInfoCategory;
import dev.latvian.mods.kubejs.script.ScriptType;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RecipeMapCategory extends ModularUIRecipeCategory<RecipeWrapper> {
    public static final Function<ResourceLocation, RecipeType<RecipeWrapper>> TYPES = Util.memoize(location -> new RecipeType<>(location, RecipeWrapper.class));

    private final RecipeMap recipeMap;
    private final IDrawable background;
    private IDrawable icon;

    public RecipeMapCategory(IJeiHelpers helpers, RecipeMap recipeMap) {
        IGuiHelper guiHelper = helpers.getGuiHelper();
        this.background = guiHelper.createBlankDrawable(176, 84);
        this.recipeMap = recipeMap;
    }

    @Override
    @Nonnull
    public RecipeType<RecipeWrapper> getRecipeType() {
        return TYPES.apply(new ResourceLocation(Multiblocked.MODID, recipeMap.name));
    }

    @Nonnull
    @Override
    public ResourceLocation getUid() {
        return getRecipeType().getUid();
    }

    @Nonnull
    @Override
    public Class<? extends RecipeWrapper> getRecipeClass() {
        return getRecipeType().getRecipeClass();
    }

    @Nonnull
    @Override
    public Component getTitle() {
        return new TranslatableComponent(recipeMap.getUnlocalizedName());
    }

    @Nonnull
    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Nonnull
    @Override
    public IDrawable getIcon() {
        return icon == null ? (icon = IGui2IDrawable.toDrawable(recipeMap.categoryTexture, 18, 18)) : icon;
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        for (RecipeMap recipeMap : RecipeMap.RECIPE_MAP_REGISTRY.values()) {
            if (recipeMap == RecipeMap.EMPTY || recipeMap.recipes.isEmpty()) continue;
            registration.addRecipes(RecipeMapCategory.TYPES.apply(new ResourceLocation(Multiblocked.MODID, recipeMap.name)), recipeMap.recipes.values()
                    .stream()
                    .map(recipe -> {
                        RecipeWidget recipeWidget = new RecipeWidget(recipe, recipeMap.progressTexture);
                        if (Multiblocked.isKubeJSLoaded()) {
                            new RecipeUIEvent(recipeWidget).post(ScriptType.CLIENT, RecipeUIEvent.ID, recipeMap.name);
                        }
                        return recipeWidget;
                    })
                    .map(RecipeWrapper::new)
                    .collect(Collectors.toList()));
        }
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (ControllerDefinition definition : MultiblockInfoCategory.REGISTER) {
            for (RecipeMap recipeMap : RecipeMap.RECIPE_MAP_REGISTRY.values()) {
                if (recipeMap == definition.getRecipeMap()) {
                    registration.addRecipeCatalyst(definition.getStackForm(), RecipeMapCategory.TYPES.apply(new ResourceLocation(Multiblocked.MODID, recipeMap.name)));
                }
            }
        }
    }

}
