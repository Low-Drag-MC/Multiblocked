package com.lowdragmc.multiblocked.core.mixins;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@Mixin(RecipeManager.class)
public interface RecipeManagerMixin {
    @Invoker("byType")
    <C extends Container, T extends Recipe<C>> Map<ResourceLocation, Recipe<C>> mbd_byType(RecipeType<T> type);
}
