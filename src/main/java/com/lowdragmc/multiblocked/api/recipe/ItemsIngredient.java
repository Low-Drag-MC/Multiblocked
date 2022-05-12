package com.lowdragmc.multiblocked.api.recipe;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.tags.ITag;
import net.minecraft.tags.TagCollectionManager;
import net.minecraft.util.ResourceLocation;

public class ItemsIngredient {
    public final Ingredient ingredient;
    private int amount;
    private String tag;

    public ItemsIngredient(ItemStack... stack) {
        this(Ingredient.of(stack), stack[0].getCount());
    }

    public ItemsIngredient(Ingredient ingredient, int amount) {
        this.ingredient = ingredient;
        this.amount = amount;
    }

    public ItemsIngredient(String tag, int amount) {
        ITag<Item> itag = TagCollectionManager.getInstance().getItems().getTag(new ResourceLocation(tag.toLowerCase()));
        this.tag = tag;
        if (itag != null) {
            this.ingredient = Ingredient.of(itag);
        } else {
            this.ingredient = Ingredient.of(ItemStack.EMPTY);
        }
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

    public boolean isTag() {
        return tag != null;
    }

    public String getTag() {
        return tag;
    }
}
