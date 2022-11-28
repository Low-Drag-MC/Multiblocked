package com.lowdragmc.multiblocked.rei.multipage;

import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.rei.IGui2Renderer;
import com.lowdragmc.lowdraglib.rei.ModularUIDisplayCategory;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.definition.ControllerDefinition;
import com.lowdragmc.multiblocked.api.recipe.RecipeMap;
import com.lowdragmc.multiblocked.api.tile.BlueprintTableTileEntity;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class MultiblockInfoDisplayCategory extends ModularUIDisplayCategory<MultiblockInfoDisplay> {
    public static final CategoryIdentifier<MultiblockInfoDisplay> CATEGORY = CategoryIdentifier.of(new ResourceLocation(Multiblocked.MODID + ":multiblock_info"));
    private final Renderer icon;

    public MultiblockInfoDisplayCategory() {
        this.icon = IGui2Renderer.toDrawable(new ItemStackTexture(BlueprintTableTileEntity.tableDefinition.getStackForm()));
    }

    public static final List<ControllerDefinition> REGISTER = new ArrayList<>();

    public static void registerMultiblock(ControllerDefinition controllerDefinition) {
        REGISTER.add(controllerDefinition);
    }

    public static void registerDisplays(DisplayRegistry registry) {
        REGISTER.stream().map(MultiblockInfoDisplay::new).forEach(registry::add);
        for (ControllerDefinition definition : REGISTER) {
            if (definition.getRecipeMap() != null) {
                if (definition.getRecipeMap().categoryTexture == null) {
                    definition.getRecipeMap().categoryTexture = new ItemStackTexture(definition.getStackForm());
                }
            }
        }
    }

    public static void registerWorkStations(CategoryRegistry registry) {
        for (ControllerDefinition definition : REGISTER) {
            if (definition.getRecipeMap() != null && definition.getRecipeMap() != RecipeMap.EMPTY) {
                registry.addWorkstations(CATEGORY, EntryStacks.of(definition.getStackForm()));
            }
        }
    }

    @Override
    public int getDisplayHeight() {
        return 220 + 8;
    }

    @Override
    public int getDisplayWidth(MultiblockInfoDisplay display) {
        return 176 + 8;
    }

    @Override
    public CategoryIdentifier<? extends MultiblockInfoDisplay> getCategoryIdentifier() {
        return CATEGORY;
    }

    @Override
    public Component getTitle() {
        return new TranslatableComponent("multiblocked.jei.multiblock_info");
    }

    @Override
    public Renderer getIcon() {
        return icon;
    }

}
