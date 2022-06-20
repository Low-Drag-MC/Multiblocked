package com.lowdragmc.multiblocked.api.recipe.serde.recipe;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

public class MBDRecipeReloadListener {
    public MinecraftServer server;
    public static MBDRecipeReloadListener INSTANCE = new MBDRecipeReloadListener();

    public void reloadRecipes() {
        if (server == null) return;
        MBDRecipeType.unloadRecipes();
        MBDRecipeType.loadRecipes(server.getRecipeManager());
    }

}
