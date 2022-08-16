package com.lowdragmc.multiblocked.jei.multipage;

import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.jei.IGui2IDrawable;
import com.lowdragmc.lowdraglib.jei.ModularUIRecipeCategory;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.definition.ControllerDefinition;
import com.lowdragmc.multiblocked.api.recipe.RecipeMap;
import com.lowdragmc.multiblocked.api.tile.BlueprintTableTileEntity;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MultiblockInfoCategory extends ModularUIRecipeCategory<MultiblockInfoWrapper> {
    private final static ResourceLocation UID = new ResourceLocation(Multiblocked.MODID + ":multiblock_info");
    private final static RecipeType<MultiblockInfoWrapper> RECIPE_TYPE = new RecipeType<>(UID, MultiblockInfoWrapper.class);
    private final IDrawable background;
    private final IDrawable icon;

    public MultiblockInfoCategory(IJeiHelpers helpers) {
        IGuiHelper guiHelper = helpers.getGuiHelper();
        this.background = guiHelper.createBlankDrawable(176, 220);
        this.icon = IGui2IDrawable.toDrawable(new ItemStackTexture(BlueprintTableTileEntity.tableDefinition.getStackForm()), 18, 18);
    }

    public static final List<ControllerDefinition> REGISTER = new ArrayList<>();

    public static void registerMultiblock(ControllerDefinition controllerDefinition) {
        REGISTER.add(controllerDefinition);
    }

    public static void registerRecipes(IRecipeRegistration registry) {
        registry.addRecipes(RECIPE_TYPE, REGISTER.stream().map(MultiblockInfoWrapper::new).collect(Collectors.toList()));
        for (ControllerDefinition definition : REGISTER) {
            if (definition.getRecipeMap() != null) {
                if (definition.getRecipeMap().categoryTexture == null) {
                    definition.getRecipeMap().categoryTexture = new ItemStackTexture(definition.getStackForm());
                }
            }
        }
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (ControllerDefinition definition : REGISTER) {
            if (definition.getRecipeMap() != null && definition.getRecipeMap() != RecipeMap.EMPTY) {
                registration.addRecipeCatalyst(definition.getStackForm(), new ResourceLocation(Multiblocked.MODID + ":" + definition.getRecipeMap().name));
            }
        }
    }

    @Override
    public void setIngredients(@Nonnull MultiblockInfoWrapper recipe, IIngredients ingredients) {
        ingredients.setInputs(VanillaTypes.ITEM, recipe.getWidget().allItemStackInputs);
        ingredients.setOutput(VanillaTypes.ITEM, recipe.definition.getStackForm());
    }

    @Nonnull
    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Nonnull
    @Override
    public Class<? extends MultiblockInfoWrapper> getRecipeClass() {
        return MultiblockInfoWrapper.class;
    }

    @Override
    @Nonnull
    public RecipeType<MultiblockInfoWrapper> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Nonnull
    @Override
    public Component getTitle() {
        return new TranslatableComponent("multiblocked.jei.multiblock_info");
    }

    @Nonnull
    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Nonnull
    @Override
    public IDrawable getIcon() {
        return icon;
    }

}
