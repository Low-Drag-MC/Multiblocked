package com.lowdragmc.multiblocked.jei.recipepage;

import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RecipeMapCategory extends ModularUIRecipeCategory<RecipeWrapper> {
    public static final Function<RecipeMap, RecipeType<RecipeWrapper>> TYPES = Util.memoize(recipeMap -> new RecipeType<>(new ResourceLocation(Multiblocked.MODID, recipeMap.name), RecipeWrapper.class));

    private final RecipeMap recipeMap;
    private final IDrawable background;
    private IDrawable icon;

    public RecipeMapCategory(IJeiHelpers helpers, RecipeMap recipeMap) {
        this.recipeMap = recipeMap;
        IGuiHelper guiHelper = helpers.getGuiHelper();
        var ui = recipeMap.createLDLibUI(null);
        if (ui == null) {
            this.background = guiHelper.createBlankDrawable(176, 84);
        } else {
            this.background = guiHelper.createBlankDrawable(ui.getSize().width, ui.getSize().height);
        }
    }

    @Override
    @Nonnull
    public RecipeType<RecipeWrapper> getRecipeType() {
        return TYPES.apply(recipeMap);
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
            registration.addRecipes(RecipeMapCategory.TYPES.apply(recipeMap), recipeMap.recipes.values()
                    .stream()
                    .map(recipe -> {
                        WidgetGroup recipeWidget = recipeMap.createLDLibUI(recipe);
                        if (recipeWidget == null) {
                            recipeWidget = new RecipeWidget(
                                    recipeMap,
                                    recipe,
                                    ProgressWidget.JEIProgress,
                                    ProgressWidget.JEIProgress);
                        }
                        if (Multiblocked.isKubeJSLoaded()) {
                            new RecipeUIEvent(recipeWidget).post(ScriptType.CLIENT, RecipeUIEvent.ID, recipeMap.name);
                        }
                        return new RecipeWrapper(recipeWidget, recipe);
                    })
                    .collect(Collectors.toList()));
        }
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (ControllerDefinition definition : MultiblockInfoCategory.REGISTER) {
            for (RecipeMap recipeMap : RecipeMap.RECIPE_MAP_REGISTRY.values()) {
                if (recipeMap == definition.getRecipeMap()) {
                    registration.addRecipeCatalyst(definition.getStackForm(), RecipeMapCategory.TYPES.apply(recipeMap));
                }
            }
        }
    }

    @Override
    public @Nullable ResourceLocation getRegistryName(@NotNull RecipeWrapper wrapper) {
        return new ResourceLocation(Multiblocked.MODID, wrapper.recipe.uid.replace(" ", "_").toLowerCase());
    }
}
