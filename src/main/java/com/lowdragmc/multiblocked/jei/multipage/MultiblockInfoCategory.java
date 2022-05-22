package com.lowdragmc.multiblocked.jei.multipage;

import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.jei.ModularUIRecipeCategory;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.definition.ControllerDefinition;
import com.lowdragmc.multiblocked.api.tile.BlueprintTableTileEntity;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MultiblockInfoCategory extends ModularUIRecipeCategory<MultiblockInfoWrapper> {
    private final static ResourceLocation UID = new ResourceLocation(Multiblocked.MODID + ":multiblock_info");
    private final IDrawable background;
    private final IDrawable icon;

    public MultiblockInfoCategory(IJeiHelpers helpers) {
        IGuiHelper guiHelper = helpers.getGuiHelper();
        this.background = guiHelper.createBlankDrawable(176, 220);
        this.icon = guiHelper.createDrawableIngredient(BlueprintTableTileEntity.tableDefinition.getStackForm());
    }

    public static final List<ControllerDefinition> REGISTER = new ArrayList<>();

    public static void registerMultiblock(ControllerDefinition controllerDefinition) {
        REGISTER.add(controllerDefinition);
    }

    public static void registerRecipes(IRecipeRegistration registry) {
        registry.addRecipes(REGISTER.stream().map(MultiblockInfoWrapper::new).collect(Collectors.toList()), UID);
        for (ControllerDefinition definition : REGISTER) {
            if (definition.recipeMap != null) {
                if (definition.recipeMap.categoryTexture == null) {
                    definition.recipeMap.categoryTexture = new ItemStackTexture(definition.getStackForm());
                }
                registry.addIngredientInfo(definition.getStackForm(), VanillaTypes.ITEM, new TranslationTextComponent(Multiblocked.MODID + ":" + definition.recipeMap.name));
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

    @Nonnull
    @Override
    public String getTitle() {
        return I18n.get("multiblocked.jei.multiblock_info");
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
