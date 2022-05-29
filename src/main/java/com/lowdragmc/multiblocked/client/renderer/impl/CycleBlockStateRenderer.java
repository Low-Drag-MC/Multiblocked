package com.lowdragmc.multiblocked.client.renderer.impl;

import com.lowdragmc.lowdraglib.LDLMod;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.FacadeBlockWorld;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * It will toggles the rendered block each second, mainly for rendering of the Any Capability.
 *
 * Because you did not schedule the chunk compiling. So please don't use it in the world. Just for JEI or such dynamic rendering.
 */
public class CycleBlockStateRenderer extends MBDBlockStateRenderer {
    public final BlockInfo[] blockInfos;
    public int index;
    public long lastTime;

    public CycleBlockStateRenderer(BlockInfo[] blockInfos) {
        super(Blocks.AIR.defaultBlockState());
        if (blockInfos.length == 0) blockInfos = new BlockInfo[]{new BlockInfo(Blocks.AIR)};
        this.blockInfos = blockInfos;
    }

    @Override
    public String getType() {
        return "cycle_state";
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    protected BakedModel getItemModel(ItemStack renderItem) {
        return Minecraft.getInstance().getItemRenderer().getModel(renderItem, null, null, 0);
    }

    public BlockInfo getBlockInfo() {
        long time = System.currentTimeMillis();
        if (time - lastTime > 1000) {
            lastTime = time;
            index = LDLMod.random.nextInt();
        }
        return blockInfos[Math.abs(index) % blockInfos.length];
    }

    @Override
    public BlockState getState(BlockState blockState) {
        BlockState state = getBlockInfo().getBlockState();
        if (blockState.hasProperty(BlockStateProperties.FACING) && state.hasProperty(BlockStateProperties.FACING)) {
            state = state.setValue(BlockStateProperties.FACING, blockState.getValue(
                    BlockStateProperties.FACING));
        } else if (blockState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, blockState.getValue(BlockStateProperties.HORIZONTAL_FACING));
        }
        return state;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public List<BakedQuad> renderModel(BlockAndTintGetter level, BlockPos pos, BlockState state, Direction side, Random rand, IModelData modelData) {
        return Collections.emptyList();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void render(BlockEntity te, float partialTicks, PoseStack stack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        BlockState state = getState(te.getBlockState());
        BlockEntity tileEntity = getBlockInfo().getBlockEntity(te.getBlockPos());
        Minecraft mc = Minecraft.getInstance();

        BlockRenderDispatcher brd  = mc.getBlockRenderer();

        FacadeBlockWorld dummyWorld = new FacadeBlockWorld(te.getLevel(), te.getBlockPos(), getState(te.getBlockState()), tileEntity);
        if (tileEntity != null) {
            tileEntity.setLevel(dummyWorld);
        }

        RenderType oldRenderLayer = MinecraftForgeClient.getRenderType();

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.enableTexture();

        for (RenderType layer : RenderType.chunkBufferLayers()) {
            if (ItemBlockRenderTypes.canRenderInLayer(state, layer)) {
                ForgeHooksClient.setRenderType(layer);
                BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
                bufferBuilder.begin(layer.mode(), layer.format());
                IModelData modelData = tileEntity == null ? EmptyModelData.INSTANCE : tileEntity.getModelData();
//                if (state.getBlock() instanceof BlockComponent) {
//                    IMultiblockedRenderer renderer = ((BlockComponent) state.getBlock()).definition.baseRenderer;
//                    if (renderer != null) {
//                        renderer.renderModel(state, te.getBlockPos(), dummyWorld, stack, bufferBuilder, true, LDLMod.random, modelData);
//                    }
//                } else {
//                    brd.renderBatched(state, te.getBlockPos(), dummyWorld, stack, bufferBuilder, true, LDLMod.random, modelData);
//                }
                brd.renderBatched(state, te.getBlockPos(), dummyWorld, stack, bufferBuilder, true, LDLMod.random, modelData);

                layer.end(bufferBuilder, 0, 0, 0);
            }
        }

        RenderSystem.depthMask(true);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();

        ForgeHooksClient.setRenderType(oldRenderLayer);

        if (tileEntity == null) return;
        BlockEntityRenderer<BlockEntity> tesr = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(tileEntity);
        if (tesr != null) {
            try {
                tesr.render(tileEntity, partialTicks, stack, buffer, combinedLight, combinedOverlay);
            } catch (Exception e) {
                getBlockInfo().setHasBlockEntity(false);
            }
        }
    }

    @Override
    public boolean hasTESR(BlockEntity tileEntity) {
        return true;
    }

    @Override
    public boolean isGlobalRenderer(BlockEntity tileEntity) {
        return true;
    }

}
