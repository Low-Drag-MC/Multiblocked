package com.lowdragmc.multiblocked.core.mixins.kubejs;

import com.lowdragmc.multiblocked.api.kubejs.recipes.IMBDRecipeProperty;
import dev.latvian.mods.kubejs.item.ItemStackJS;
import dev.latvian.mods.kubejs.item.ingredient.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

//We're fucked.
@Mixin(value = {
        IgnoreNBTIngredientJS.class,
        CustomIngredient.class,
        FilteredIngredientJS.class,
        GroupIngredientJS.class,
        IngredientStackJS.class,
        IngredientWithCustomPredicateJS.class,
        MatchAllIngredientJS.class,
        MatchAnyIngredientJS.class,
        ModIngredientJS.class,
        NotIngredientJS.class,
        RegexIngredientJS.class,
        TagIngredientJS.class,
        WeakNBTIngredientJS.class,
        ItemStackJS.class
})
public class IngredientJSMixin implements IMBDRecipeProperty {

    @Unique
    private boolean mbdPerTick = false;

    @Unique
    private String mbdSlotName = null;

    public IngredientJS perTick(boolean perTick) {
        this.mbdPerTick = perTick;
        return (IngredientJS) this;
    }

    public IngredientJS atSlot(String slotName) {
        this.mbdSlotName = slotName;
        return (IngredientJS) this;
    }

    public boolean isPerTick() {
        return this.mbdPerTick;
    }

    @Override
    public String atSlot() {
        return mbdSlotName;
    }
}
