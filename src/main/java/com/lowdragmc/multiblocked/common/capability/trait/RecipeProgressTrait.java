package com.lowdragmc.multiblocked.common.capability.trait;

import com.lowdragmc.lowdraglib.LDLMod;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.jei.JEIPlugin;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.capability.trait.ProgressCapabilityTrait;
import com.lowdragmc.multiblocked.api.recipe.RecipeLogic;
import com.lowdragmc.multiblocked.api.recipe.RecipeMap;
import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import com.lowdragmc.multiblocked.api.tile.IControllerComponent;
import com.lowdragmc.multiblocked.jei.recipepage.RecipeMapCategory;
import com.lowdragmc.multiblocked.rei.recipepage.RecipeMapDisplayCategory;
import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;

/**
 * @author KilaBash
 * @date 2022/11/15
 * @implNote RecipeProgressTrait
 */
public class RecipeProgressTrait extends ProgressCapabilityTrait {

    public RecipeProgressTrait(MultiblockCapability<?> capability) {
        super(capability);
    }

    @Override
    protected String dynamicHoverTips(double progress) {
        return LocalizationUtils.format("multiblocked.top.recipe_progress", ((int)(progress * 100)) + "%");
    }

    private void onClick(ClickData cd) {
        if (cd.isRemote && component instanceof IControllerComponent controller) {
            var recipeMap = controller.getDefinition().getRecipeMap();
            if (recipeMap != RecipeMap.EMPTY) {
                if (LDLMod.isJeiLoaded()) {
                    JEIPlugin.jeiRuntime.getRecipesGui().showTypes(Collections.singletonList(RecipeMapCategory.TYPES.apply(recipeMap)));
                } else if (LDLMod.isReiLoaded()) {
                    ViewSearchBuilder.builder().addCategory(RecipeMapDisplayCategory.CATEGORIES.apply(recipeMap)).open();
                }
            }
        }
    }

    @Override
    public void createUI(ComponentTileEntity<?> component, WidgetGroup group, Player player) {
        super.createUI(component, group, player);
        group.addWidget(new ButtonWidget(x, y, width, height, IGuiTexture.EMPTY, this::onClick));
    }

    @Override
    protected double getProgress() {
        if (component instanceof IControllerComponent controller) {
            RecipeLogic recipeLogic = controller.getRecipeLogic();
            return recipeLogic == null ? 0 : (recipeLogic.progress * 1. / recipeLogic.duration);
        }
        return 0;
    }

    @Override
    protected boolean hasIOSettings() {
        return false;
    }
}
