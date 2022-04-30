package com.lowdragmc.multiblocked.jei.ingredient;

import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.registration.IModIngredientRegistration;

import java.util.Collection;

public abstract class AbstractIngredient<T> implements IIngredientType<T>, IIngredientHelper<T>, IIngredientRenderer<T> {
    
    public void registerIngredients(IModIngredientRegistration registry) {
        registry.register(this, getAllIngredients(), this, this);
    }

    public abstract Collection<T> getAllIngredients();
}
