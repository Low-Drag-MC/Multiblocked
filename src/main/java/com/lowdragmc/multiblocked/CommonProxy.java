package com.lowdragmc.multiblocked;

import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.client.renderer.impl.BlockStateRenderer;
import com.lowdragmc.multiblocked.api.block.ItemComponent;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.definition.ComponentDefinition;
import com.lowdragmc.multiblocked.api.definition.ControllerDefinition;
import com.lowdragmc.multiblocked.api.definition.PartDefinition;
import com.lowdragmc.multiblocked.api.gui.dialogs.JsonBlockPatternWidget;
import com.lowdragmc.multiblocked.api.pattern.JsonBlockPattern;
import com.lowdragmc.multiblocked.api.recipe.RecipeMap;
import com.lowdragmc.multiblocked.api.recipe.ingredient.SizedIngredient;
import com.lowdragmc.multiblocked.api.recipe.serde.recipe.MultiBlockRecipe;
import com.lowdragmc.multiblocked.api.registry.*;
import com.lowdragmc.multiblocked.api.tile.BlueprintTableTileEntity;
import com.lowdragmc.multiblocked.api.tile.ControllerTileTesterEntity;
import com.lowdragmc.multiblocked.api.tile.part.PartTileTesterEntity;
import com.lowdragmc.multiblocked.client.renderer.IMultiblockedRenderer;
import com.lowdragmc.multiblocked.client.renderer.impl.CycleBlockStateRenderer;
import com.lowdragmc.multiblocked.common.block.CreateBlockComponent;
import com.lowdragmc.multiblocked.common.capability.CreateStressCapacityCapability;
import com.lowdragmc.multiblocked.common.definition.CreatePartDefinition;
import com.lowdragmc.multiblocked.common.tile.JarTileEntity;
import com.lowdragmc.multiblocked.common.tile.PedestalTileEntity;
import com.lowdragmc.multiblocked.network.MultiblockedNetworking;
import com.simibubi.create.foundation.block.BlockStressValues;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.IForgeRegistry;
import software.bernie.geckolib3.GeckoLib;

import java.io.File;

import static com.lowdragmc.multiblocked.api.recipe.serde.recipe.MBDRecipeType.MULTIBLOCK_RECIPE_SERIALIZER;
import static com.lowdragmc.multiblocked.api.recipe.serde.recipe.MBDRecipeType.MULTIBLOCK_RECIPE_TYPE;

public class CommonProxy {
    public CommonProxy() {
        if (Multiblocked.isGeoLoaded()) {
            GeckoLib.initialize();
        }
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        eventBus.register(this);
        MultiblockedNetworking.init();
        MbdCapabilities.registerCapabilities();
        MbdRenderers.registerRenderers();
        MbdPredicates.registerPredicates();
        MbdRecipeConditions.registerConditions();
    }

    @SubscribeEvent
    public void commonSetup(FMLCommonSetupEvent e) {
        e.enqueueWork(() -> {
            for (MultiblockCapability<?> capability : MbdCapabilities.CAPABILITY_REGISTRY.values()) {
                capability.getAnyBlock().definition.baseRenderer = new CycleBlockStateRenderer(capability.getCandidates());
            }
            RecipeMap.registerRecipeFromFile(Multiblocked.GSON, new File(Multiblocked.location, "recipe_map"));
            MbdComponents.commonLastWork();
            if (Multiblocked.isCreateLoaded()) {
                MbdComponents.DEFINITION_REGISTRY.forEach((r, d) -> {
                    if (d instanceof CreatePartDefinition) {
                        BlockStressValues.IStressValueProvider provider = BlockStressValues.getProvider(r.getNamespace());
                        if (provider == null) {
                            BlockStressValues.registerProvider(r.getNamespace(), CreateStressCapacityCapability.STRESS_PROVIDER);
                        }
                    }
                });
            }
        });

        CraftingHelper.register(new ResourceLocation("multiblocked:sized"), SizedIngredient.SERIALIZER);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void registerBlocks(RegistryEvent.Register<Block> event) {
        registerComponents();
        IForgeRegistry<Block> registry = event.getRegistry();
        MbdComponents.registerBlocks(registry);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void registerTileEntities(RegistryEvent.Register<BlockEntityType<?>> event) {
        IForgeRegistry<BlockEntityType<?>> registry = event.getRegistry();
        MbdComponents.registerTileEntity(registry);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void registerRecipes(RegistryEvent.Register<RecipeSerializer<?>> event) {
        IForgeRegistry<RecipeSerializer<?>> registry = event.getRegistry();
        Registry.register(Registry.RECIPE_TYPE, MultiBlockRecipe.MultiBlockRecipeType.TYPE_ID, MULTIBLOCK_RECIPE_TYPE);
        MULTIBLOCK_RECIPE_SERIALIZER.setRegistryName(MultiBlockRecipe.MultiBlockRecipeType.TYPE_ID);
        registry.register(MULTIBLOCK_RECIPE_SERIALIZER);
    }

    @SubscribeEvent
    public void registerItems(RegistryEvent.Register<Item> event) {
        IForgeRegistry<Item> registry = event.getRegistry();
        MbdComponents.COMPONENT_ITEMS_REGISTRY.values().forEach(registry::register);
        MbdItems.registerItems(registry);
    }

    public static void registerComponents() {
        // register any capability block
        MbdCapabilities.registerAnyCapabilityBlocks();
        // register blueprint table
        BlueprintTableTileEntity.registerBlueprintTable();
        // register controller tester
        ControllerTileTesterEntity.registerTestController();
        // register part tester
        PartTileTesterEntity.registerTestPart();
        // register part pedestal
        PedestalTileEntity.registerPedestal();
        // register part jar
        JarTileEntity.registerJar();
        // register JsonBlockPatternBlock
        JsonBlockPatternWidget.registerBlock();
        // register builtin components
        MbdComponents.registerComponentFromResource(Multiblocked.class, Multiblocked.GSON, new ResourceLocation(Multiblocked.MODID, "part/mbd_energy_input"), PartDefinition.class, null);
        MbdComponents.registerComponentFromResource(Multiblocked.class, Multiblocked.GSON, new ResourceLocation(Multiblocked.MODID, "part/mbd_energy_output"), PartDefinition.class, null);
        MbdComponents.registerComponentFromResource(Multiblocked.class, Multiblocked.GSON, new ResourceLocation(Multiblocked.MODID, "part/mbd_item_input"), PartDefinition.class, null);
        MbdComponents.registerComponentFromResource(Multiblocked.class, Multiblocked.GSON, new ResourceLocation(Multiblocked.MODID, "part/mbd_item_output"), PartDefinition.class, null);
        MbdComponents.registerComponentFromResource(Multiblocked.class, Multiblocked.GSON, new ResourceLocation(Multiblocked.MODID, "part/mbd_fluid_input"), PartDefinition.class, null);
        MbdComponents.registerComponentFromResource(Multiblocked.class, Multiblocked.GSON, new ResourceLocation(Multiblocked.MODID, "part/mbd_fluid_output"), PartDefinition.class, null);
        MbdComponents.registerComponentFromResource(Multiblocked.class, Multiblocked.GSON, new ResourceLocation(Multiblocked.MODID, "part/mbd_entity"), PartDefinition.class, null);
        // register JsonFiles
        MbdComponents.registerComponentFromFile(
                Multiblocked.GSON,
                new File(Multiblocked.location, "definition/controller"),
                ControllerDefinition.class,
                CommonProxy::controllerPost);
        MbdComponents.registerComponentFromFile(
                Multiblocked.GSON,
                new File(Multiblocked.location, "definition/part"),
                PartDefinition.class,
                CommonProxy::componentPost);

        if (Multiblocked.isCreateLoaded()) {

            MbdComponents.registerComponentFromResource(Multiblocked.class, Multiblocked.GSON, new ResourceLocation(Multiblocked.MODID, "part/create/mbd_create_input"), CreatePartDefinition.class, CreateBlockComponent::new, ItemComponent::new, null);
            MbdComponents.registerComponentFromResource(Multiblocked.class, Multiblocked.GSON, new ResourceLocation(Multiblocked.MODID, "part/create/mbd_create_output"), CreatePartDefinition.class, CreateBlockComponent::new, ItemComponent::new, null);

            MbdComponents.registerComponentFromFile(
                    Multiblocked.GSON,
                    new File(Multiblocked.location, "definition/part/create"),
                    CreatePartDefinition.class,
                    CreateBlockComponent::new,
                    ItemComponent::new,
                    CommonProxy::componentPost);
        }
    }

    private static void componentPost(ComponentDefinition definition, JsonObject config) {
        if (definition.baseRenderer instanceof BlockStateRenderer) {
            definition.baseRenderer = Multiblocked.GSON.fromJson(config.get("baseRenderer"), IMultiblockedRenderer.class);
        }
        if (definition.formedRenderer instanceof BlockStateRenderer) {
            definition.formedRenderer = Multiblocked.GSON.fromJson(config.get("formedRenderer"), IMultiblockedRenderer.class);
        }
        if (definition.workingRenderer instanceof BlockStateRenderer) {
            definition.workingRenderer = Multiblocked.GSON.fromJson(config.get("workingRenderer"), IMultiblockedRenderer.class);
        }
    }

    public static void controllerPost(ControllerDefinition definition, JsonObject config) {
        definition.basePattern = Multiblocked.GSON.fromJson(config.get("basePattern"), JsonBlockPattern.class).build();
        definition.recipeMap = RecipeMap.RECIPE_MAP_REGISTRY.getOrDefault(config.get("recipeMap").getAsString(), RecipeMap.EMPTY);
        definition.catalyst = Multiblocked.GSON.fromJson(config.get("catalyst"), ItemStack.class);
        componentPost(definition, config);
    }
}
