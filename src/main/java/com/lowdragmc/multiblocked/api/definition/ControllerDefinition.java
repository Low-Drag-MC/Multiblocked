package com.lowdragmc.multiblocked.api.definition;


import com.lowdragmc.multiblocked.api.pattern.BlockPattern;
import com.lowdragmc.multiblocked.api.pattern.MultiblockShapeInfo;
import com.lowdragmc.multiblocked.api.recipe.RecipeMap;
import com.lowdragmc.multiblocked.api.tile.ControllerTileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.function.Function;

/**
 * Definition of a controller, which define its structure, logic, recipe chain and so on.
 */
public class ControllerDefinition extends ComponentDefinition {
    public transient BlockPattern basePattern;
    public transient RecipeMap recipeMap;
    public ItemStack catalyst; // if null, checking pattern per second
    public boolean consumeCatalyst;
    public transient List<MultiblockShapeInfo> designs;

    // used for Gson
    public ControllerDefinition() {
        this(null);
    }

    public ControllerDefinition(ResourceLocation location) {
        this(location, ControllerTileEntity::new);
    }

    public ControllerDefinition(ResourceLocation location, Function<ControllerDefinition, TileEntity> teSupplier) {
        super(location, d -> teSupplier.apply((ControllerDefinition) d));
        this.recipeMap = RecipeMap.EMPTY;
    }

    public List<MultiblockShapeInfo> getDesigns() {
        if (designs != null) return designs;
        // auto gen
        if (basePattern != null) {
            return autoGenDFS(basePattern, new ArrayList<>(), new Stack<>());
        }
        return Collections.emptyList();
    }

    private List<MultiblockShapeInfo> autoGenDFS(BlockPattern structurePattern, List<MultiblockShapeInfo> pages, Stack<Integer> repetitionStack) {
        int[][] aisleRepetitions = structurePattern.aisleRepetitions;
        if (repetitionStack.size() == aisleRepetitions.length) {
            int[] repetition = new int[repetitionStack.size()];
            for (int i = 0; i < repetitionStack.size(); i++) {
                repetition[i] = repetitionStack.get(i);
            }
            pages.add(new MultiblockShapeInfo(structurePattern.getPreview(repetition)));
        } else {
            for (int i = aisleRepetitions[repetitionStack.size()][0]; i <= aisleRepetitions[repetitionStack.size()][1]; i++) {
                repetitionStack.push(i);
                autoGenDFS(structurePattern, pages, repetitionStack);
                repetitionStack.pop();
            }
        }
        return pages;
    }

    public String getDescription() {
        return location.getNamespace() + "." + location.getPath() + ".description";
    }

    @Override
    public boolean needUpdateTick() {
        return super.needUpdateTick() || catalyst == null;
    }

    public BlockPattern getBasePattern() {
        return basePattern;
    }

    public RecipeMap getRecipeMap() {
        return recipeMap;
    }


    public void setBasePattern(BlockPattern basePattern) {
        this.basePattern = basePattern;
    }

    public void setRecipeMap(RecipeMap recipeMap) {
        this.recipeMap = recipeMap;
    }

    public void setDesigns(List<MultiblockShapeInfo> designs) {
        this.designs = designs;
    }
}
