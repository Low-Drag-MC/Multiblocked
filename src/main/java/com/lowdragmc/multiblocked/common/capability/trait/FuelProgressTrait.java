package com.lowdragmc.multiblocked.common.capability.trait;

import com.lowdragmc.lowdraglib.LDLMod;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
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
public class FuelProgressTrait extends ProgressCapabilityTrait {

    public FuelProgressTrait(MultiblockCapability<?> capability) {
        super(capability);
    }

    @Override
    protected String dynamicHoverTips(double progress) {
        return LocalizationUtils.format("multiblocked.top.fuel_progress", ((int)(progress * 100)) + "%");
    }

    @Override
    protected double getProgress() {
        if (component instanceof IControllerComponent controller) {
            RecipeLogic recipeLogic = controller.getRecipeLogic();
            return recipeLogic == null ? 0 : Math.min(recipeLogic.fuelTime, recipeLogic.fuelMaxTime) * 1d / Math.max(recipeLogic.fuelMaxTime, 1);
        }
        return 0;
    }

    @Override
    protected boolean hasIOSettings() {
        return false;
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
    public void handleMbdUI(ModularUI modularUI) {
        if (slotName != null && !slotName.isEmpty()) {
            for (Widget widget : modularUI.getWidgetsById("^%s$".formatted(slotName))) {
                if (widget instanceof ProgressWidget progressWidget) {
                    progressWidget.setProgressSupplier(this::getProgress).setDynamicHoverTips(this::dynamicHoverTips);
                    var pos = progressWidget.getSelfPosition();
                    var size = progressWidget.getSize();
                    progressWidget.getParent().addWidget(new ButtonWidget(pos.x, pos.y, size.width, size.height, IGuiTexture.EMPTY, this::onClick));
                }
            }
        }
    }
}
