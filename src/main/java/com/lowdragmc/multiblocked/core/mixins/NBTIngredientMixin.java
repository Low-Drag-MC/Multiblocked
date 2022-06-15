package com.lowdragmc.multiblocked.core.mixins;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.crafting.NBTIngredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NBTIngredient.class)
public interface NBTIngredientMixin {
    @Accessor(value = "stack", remap = false)
    ItemStack getStack();
}
