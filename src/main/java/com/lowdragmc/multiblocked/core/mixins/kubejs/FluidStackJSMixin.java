package com.lowdragmc.multiblocked.core.mixins.kubejs;

import com.lowdragmc.multiblocked.api.kubejs.recipes.IMBDRecipeProperty;
import dev.latvian.mods.kubejs.fluid.FluidStackJS;
import dev.latvian.mods.kubejs.item.ingredient.IngredientJS;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(FluidStackJS.class)
public class FluidStackJSMixin implements IMBDRecipeProperty {
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
