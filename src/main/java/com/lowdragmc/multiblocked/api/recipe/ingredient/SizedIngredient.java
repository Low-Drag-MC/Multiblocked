package com.lowdragmc.multiblocked.api.recipe.ingredient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.stream.Stream;

public class SizedIngredient extends Ingredient {
    private final int amount;
    private String tag;
    private final Ingredient inner;
    private ItemStack[] itemStacks = null;

    public SizedIngredient(Ingredient inner, int amount) {
        super(Stream.empty());
        this.amount = amount;
        this.inner = inner;
    }

    public SizedIngredient(String tag, int amount) {
        this(Ingredient.of(TagKey.create(Registry.ITEM_REGISTRY, new ResourceLocation(tag.toLowerCase()))), amount);
        this.tag = tag;
    }

    public int getAmount() {
        return amount;
    }

    public Ingredient getInner() {
        return inner;
    }

    @Override
    public @NotNull JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "multiblocked:sized");
        json.addProperty("count", amount);
        if (tag != null) {
            json.addProperty("tag", tag);
        } else {
            json.add("ingredient", inner.toJson());
        }
        return json;
    }

    @Override
    public boolean test(@Nullable ItemStack stack) {
        return inner.test(stack);
    }

    @Override
    public ItemStack @NotNull [] getItems() {
        if (itemStacks == null)
            itemStacks = Arrays.stream(inner.getItems()).map(i -> {
                ItemStack ic = i.copy();
                ic.setCount(amount);
                return ic;
            }).toArray(ItemStack[]::new);
        return itemStacks;
    }

    @Override
    public @NotNull IntList getStackingIds() {
        return inner.getStackingIds();
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public @NotNull IIngredientSerializer<? extends Ingredient> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public boolean isEmpty() {
        return inner.isEmpty();
    }

    public static final IIngredientSerializer<SizedIngredient> SERIALIZER = new IIngredientSerializer<>() {
        @Override
        public @NotNull SizedIngredient parse(FriendlyByteBuf buffer) {
            int amount = buffer.readVarInt();
            if (buffer.readBoolean()) {
                return new SizedIngredient(buffer.readUtf(), amount);
            } else {
                return new SizedIngredient(Ingredient.fromNetwork(buffer), amount);
            }
        }

        @Override
        public @NotNull SizedIngredient parse(JsonObject json) {
            int amount = json.get("count").getAsInt();
            if (json.has("tag")) {
                return new SizedIngredient(json.get("tag").getAsString(), amount);
            } else {
                Ingredient inner = Ingredient.fromJson(json.get("ingredient"));
                return new SizedIngredient(inner, amount);
            }
        }

        @Override
        public void write(FriendlyByteBuf buffer, SizedIngredient ingredient) {
            buffer.writeVarInt(ingredient.getAmount());
            if (ingredient.tag != null) {
                buffer.writeBoolean(true);
                buffer.writeUtf(ingredient.tag);
            } else {
                buffer.writeBoolean(false);
                ingredient.inner.toNetwork(buffer);
            }
        }
    };

    public boolean isTag() {
        return tag != null;
    }

    public String getTag() {
        return tag;
    }
}
