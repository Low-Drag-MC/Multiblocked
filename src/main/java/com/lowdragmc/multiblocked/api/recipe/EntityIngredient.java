package com.lowdragmc.multiblocked.api.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

/**
 * @author KilaBash
 * @date 2022/8/22
 * @implNote EntityIngredient
 */
public class EntityIngredient {
    public EntityType<?> type = EntityType.PIG;
    public CompoundNBT tag;

    public boolean match(Entity entity) {
        if (entity.getType() != type) return false;
        if (tag != null) {
            CompoundNBT nbt = entity.serializeNBT();
            CompoundNBT merged = nbt.copy().merge(tag);
            return nbt.equals(merged);
        }
        return true;
    }

    public static EntityIngredient fromJson(JsonElement json) {
        EntityIngredient ingredient = new EntityIngredient();
        if (json.isJsonPrimitive()) {
            ingredient.type = EntityType.byString(json.getAsString()).orElse(EntityType.PIG);
        } else if (json.isJsonObject()) {
            JsonObject object = json.getAsJsonObject();
            ingredient.type = EntityType.byString(JSONUtils.getAsString(object, "type")).orElse(EntityType.PIG);
            if (object.has("tag")) {
                try {
                    ingredient.tag = JsonToNBT.parseTag(object.get("tag").getAsString());
                } catch (CommandSyntaxException ignored) {
                }
            }
        }
        return ingredient;
    }

    public JsonElement toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("type", type.getRegistryName().toString());
        if (tag != null) {
            object.addProperty("tag", tag.toString());
        }
        return object;
    }

    public static EntityIngredient of(Object o) {
        EntityIngredient ingredient = new EntityIngredient();
        if (o instanceof EntityIngredient) {
            ingredient = (EntityIngredient) o;
        } else if (o instanceof CharSequence) {
            ingredient.type = EntityType.byString(o.toString()).orElse(EntityType.PIG);
        } else if (o instanceof ResourceLocation) {
            ingredient.type = EntityType.byString(o.toString()).orElse(EntityType.PIG);
        }
        return ingredient;
    }

    public void spawn(ServerWorld serverLevel, CompoundNBT tag, BlockPos pos) {
        type.spawn(serverLevel, tag, null, null, pos, SpawnReason.NATURAL, false, false);
    }

    public EntityIngredient copy() {
        EntityIngredient copy = new EntityIngredient();
        copy.type = type;
        if (tag != null) {
            copy.tag = tag.copy();
        }
        return copy;
    }

    public boolean isEntityItem() {
        return type == EntityType.ITEM && tag != null && tag.contains("EntityTag") && tag.getCompound("EntityTag").contains("Item");
    }

    public ItemStack getEntityItem() {
        return ItemStack.of(tag.getCompound("EntityTag").getCompound("Item"));
    }
}
