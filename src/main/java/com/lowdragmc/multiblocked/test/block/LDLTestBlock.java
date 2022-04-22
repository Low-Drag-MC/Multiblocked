package com.lowdragmc.multiblocked.test.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraftforge.common.ToolType;

import java.util.List;

public class LDLTestBlock extends Block {

    public LDLTestBlock() {
        super(Block.Properties
                .of(Material.METAL)
                .strength(5.0f, 6.0f)
                .sound(SoundType.STONE)
                .harvestLevel(1)
                .harvestTool(ToolType.PICKAXE));
    }

    @Override
    public List<ItemStack> getDrops(BlockState pState, LootContext.Builder pBuilder) {
        return super.getDrops(pState, pBuilder);
    }

//    @Override
//    @Nonnull
//    public IRenderer getRenderer(BlockState state, BlockPos pos, IBlockDisplayReader blockReader) {
//
//        return new BlockStateRenderer(Blocks.GLASS.defaultBlockState());
//    }

}
