package com.lowdragmc.multiblocked.api.recipe;

import net.minecraft.core.Registry;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

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
        TagKey<Item> itag = null;
        try {
            itag = TagKey.create(Registry.ITEM_REGISTRY, new ResourceLocation(tag.toLowerCase()));
        } catch (Exception ignored) {}
        this.tag = tag;
        if (itag != null) {
            this.ingredient = Ingredient.of(itag);
        } else {
            this.ingredient = Ingredient.of(ItemStack.EMPTY);
        }
        this.amount = amount;
    }

    public ItemsIngredient(Ingredient ingredient) {
        this.ingredient = ingredient;
        this.amount = ingredient.isEmpty() ? 0 : ingredient.getItems()[0].getCount();
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
        ItemStack output = stacks.length > 0 ? stacks[0] : ItemStack.EMPTY;
        if (!output.isEmpty()) {
            output.setCount(amount);
        }
        return output;
    }

    public boolean isTag() {
        return tag != null;
    }

    public String getTag() {
        return tag;
    }

    @Override
    public int hashCode() {
        int hash = amount;
        for (ItemStack stack : ingredient.getItems()) {
            ResourceLocation name = stack.getItem().getRegistryName();
            hash += name == null ? 0 : name.hashCode();
        }
        return hash;
    }
}
