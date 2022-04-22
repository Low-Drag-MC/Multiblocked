package com.lowdragmc.multiblocked;

import com.lowdragmc.multiblocked.test.block.LDLTestBlock;
import com.lowdragmc.multiblocked.test.block.LDLTestBlockItem;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class RegistryHandler {
   public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MultiblockedMod.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MultiblockedMod.MODID);

    public static void init() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        registerBlocks();
    }

    public static RegistryObject<LDLTestBlock> TEST_BLOCK;
    public static RegistryObject<Item> TEST_BLOCK_ITEM;

    public static void registerBlocks() {
        TEST_BLOCK = BLOCKS.register("test_block", LDLTestBlock::new);
        TEST_BLOCK_ITEM = ITEMS.register("test_block", () -> new LDLTestBlockItem(TEST_BLOCK.get(), new Item.Properties().tab(ItemGroup.TAB_REDSTONE)));
    }

}
