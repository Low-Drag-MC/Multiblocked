package com.lowdragmc.multiblocked.api.kubejs.recipes;

import com.lowdragmc.multiblocked.api.recipe.RecipeMap;
import dev.latvian.mods.rhino.util.HideFromJS;

import java.util.ArrayList;
import java.util.List;

public class RecipeMapJS {
    private final RecipeMap map;
    public final transient List<RecipeBuilderJS> builders;

    public RecipeMapJS(RecipeMap map) {
        this.map = map;
        this.builders = new ArrayList<>();
    }

    public RecipeBuilderJS create() {
        RecipeBuilderJS builderJS = new RecipeBuilderJS(this.map);
        builders.add(builderJS);
        return builderJS;
    }

    @HideFromJS
    public void build() {
        builders.forEach(RecipeBuilderJS::buildAndRegister);
    }
}
