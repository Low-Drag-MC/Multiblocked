package com.lowdragmc.multiblocked.api.recipe.ingredient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;

/**
 * @author KilaBash
 * @date 2022/8/22
 * @implNote EntityIngredient
 */
public class EntityIngredient {
    public EntityType<?> type = EntityType.PIG;
    public CompoundTag tag;

    public boolean match(Entity entity) {
        if (entity.getType() != type) return false;
        if (tag != null) {
            CompoundTag nbt = entity.serializeNBT();
            CompoundTag merged = nbt.copy().merge(tag);
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
            ingredient.type = EntityType.byString(GsonHelper.getAsString(object, "type")).orElse(EntityType.PIG);
            if (object.has("tag")) {
                try {
                    ingredient.tag = TagParser.parseTag(object.get("tag").getAsString());
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
        } else if (o instanceof CharSequence charSequence) {
            ingredient.type = EntityType.byString(charSequence.toString()).orElse(EntityType.PIG);
        } else if (o instanceof ResourceLocation resourceLocation) {
            ingredient.type =  EntityType.byString(resourceLocation.toString()).orElse(EntityType.PIG);
        }
        return ingredient;
    }

    public void spawn(ServerLevel serverLevel, CompoundTag tag, BlockPos pos) {
        type.spawn(serverLevel, tag, null, null, pos, MobSpawnType.NATURAL, false, false);
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
