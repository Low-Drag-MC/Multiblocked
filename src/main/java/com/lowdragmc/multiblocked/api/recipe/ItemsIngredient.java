package com.lowdragmc.multiblocked.api.recipe;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;

public class ItemsIngredient {
    public final Ingredient ingredient;
    private int amount;

    public ItemsIngredient(ItemStack... stack) {
        this(Ingredient.of(stack), stack[0].getCount());
    }

    public ItemsIngredient(Ingredient ingredient, int amount) {
        this.ingredient = ingredient;
        this.amount = amount;
    }
    
    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public ItemsIngredient copy() {
        return new ItemsIngredient(ingredient, amount);
    }

    public ItemStack getOutputStack() {
        ItemStack[] stacks = ingredient.getItems();
        return stacks.length > 0 ? stacks[0] : ItemStack.EMPTY;
    }
}
