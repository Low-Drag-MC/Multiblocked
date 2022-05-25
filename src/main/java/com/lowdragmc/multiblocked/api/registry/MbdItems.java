package com.lowdragmc.multiblocked.api.registry;

import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.item.ItemBlueprint;
import com.lowdragmc.multiblocked.api.item.ItemMultiblockBuilder;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.IForgeRegistry;

public class MbdItems {
    public static ItemBlueprint BLUEPRINT = new ItemBlueprint();
    public static ItemMultiblockBuilder BUILDER = new ItemMultiblockBuilder();

    public static void registerItems(IForgeRegistry<Item> registry) {
        registry.register(BLUEPRINT);
        registry.register(BUILDER);
    }

    public static void registerModelsProperties() {
        ItemProperties.register(BLUEPRINT, new ResourceLocation(Multiblocked.MODID, "raw"), (itemStack, clientWorld, entity, seed) -> ItemBlueprint.isRaw(itemStack) ? 0 : 1);
        ItemProperties.register(BUILDER, new ResourceLocation(Multiblocked.MODID, "raw"), (itemStack, clientWorld, entity, seed) -> ItemMultiblockBuilder.isRaw(itemStack) ? 0 : 1);
    }
//
//    @SuppressWarnings("ConstantConditions")
//    public static void registerModels() {
//        ModelLoader.setCustomModelResourceLocation(BLUEPRINT, 0, new ModelResourceLocation(BLUEPRINT.getRegistryName(), "inventory"));
//        ModelLoader.setCustomModelResourceLocation(BLUEPRINT, 1, new ModelResourceLocation(BLUEPRINT.getRegistryName() + "_pattern", "inventory"));
//        ModelLoader.setCustomModelResourceLocation(BUILDER, 0, new ModelResourceLocation(BUILDER.getRegistryName(), "inventory"));
//        ModelLoader.setCustomModelResourceLocation(BUILDER, 1, new ModelResourceLocation(BUILDER.getRegistryName() + "_pattern", "inventory"));
//    }

}
