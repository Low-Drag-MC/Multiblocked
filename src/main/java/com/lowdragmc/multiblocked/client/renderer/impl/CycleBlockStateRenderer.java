package com.lowdragmc.multiblocked.client.renderer.impl;

import com.lowdragmc.lowdraglib.LDLMod;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.FacadeBlockWorld;
import com.lowdragmc.multiblocked.api.block.BlockComponent;
import com.lowdragmc.multiblocked.client.renderer.IMultiblockedRenderer;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
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
    protected IBakedModel getItemModel(ItemStack renderItem) {
        return Minecraft.getInstance().getItemRenderer().getModel(renderItem, null, null);
    }

    public BlockInfo getBlockInfo() {
        long time = System.currentTimeMillis();
        if (time - lastTime > 1000) {
            lastTime = time;
            index = LDLMod.RNG.nextInt();
        }
        return blockInfos[Math.abs(index) % blockInfos.length];
    }

    @Override
    public BlockState getState(BlockState blockState) {
        BlockState state = getBlockInfo().getBlockState();
        if (blockState.hasProperty(BlockStateProperties.FACING) && state.hasProperty(BlockStateProperties.FACING)) {
            state = state.setValue(BlockStateProperties.FACING, blockState.getValue(BlockStateProperties.FACING));
        } else if (blockState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, blockState.getValue(BlockStateProperties.HORIZONTAL_FACING));
        }
        return state;
    }

    @Override
    public List<BakedQuad> renderModel(IBlockDisplayReader level, BlockPos pos, BlockState state, Direction side, Random rand, IModelData modelData) {
        return Collections.emptyList();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void render(TileEntity te, float partialTicks, MatrixStack stack, IRenderTypeBuffer buffer, int combinedLight, int combinedOverlay) {
        BlockState state = getState(te.getBlockState());
        FluidState fluidState = state.getFluidState();
        TileEntity tileEntity = getBlockInfo().getTileEntity();
        Minecraft mc = Minecraft.getInstance();

        BlockRendererDispatcher brd  = mc.getBlockRenderer();

        FacadeBlockWorld dummyWorld = new FacadeBlockWorld(te.getLevel(), te.getBlockPos(), getState(te.getBlockState()), tileEntity);
        if (tileEntity != null) {
            tileEntity.setLevelAndPosition(dummyWorld, te.getBlockPos());
        }

        RenderType oldRenderLayer = MinecraftForgeClient.getRenderLayer();

        RenderSystem.enableCull();
        RenderSystem.enableRescaleNormal();
        RenderSystem.disableLighting();
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderHelper.turnOff();
        RenderSystem.disableLighting();
        RenderSystem.enableTexture();
        RenderSystem.enableAlphaTest();
        mc.getTextureManager().bind(AtlasTexture.LOCATION_BLOCKS);

        for (RenderType layer : RenderType.chunkBufferLayers()) {
            if (!fluidState.isEmpty() && RenderTypeLookup.canRenderInLayer(fluidState, layer)) {
                ForgeHooksClient.setRenderLayer(layer);
                IVertexBuilder bufferBuilder = buffer.getBuffer(layer);
                brd.renderLiquid(te.getBlockPos(), dummyWorld, bufferBuilder, fluidState);
            }
            if (RenderTypeLookup.canRenderInLayer(state, layer)) {
                ForgeHooksClient.setRenderLayer(layer);
                IVertexBuilder bufferBuilder = buffer.getBuffer(layer);
                IModelData modelData = tileEntity == null ? EmptyModelData.INSTANCE : tileEntity.getModelData();
                brd.renderModel(state, te.getBlockPos(), dummyWorld, stack, bufferBuilder, true, LDLMod.random, modelData);
            }
        }

        RenderSystem.shadeModel(7425);
        RenderSystem.enableColorMaterial();
        RenderSystem.colorMaterial(1032, 5634);
        RenderSystem.disableRescaleNormal();
        RenderSystem.depthMask(true);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();

        ForgeHooksClient.setRenderLayer(oldRenderLayer);

        if (tileEntity == null) return;
        TileEntityRenderer<TileEntity> tesr = TileEntityRendererDispatcher.instance.getRenderer(tileEntity);
        if (tesr != null) {
            try {
                tesr.render(tileEntity, partialTicks, stack, buffer, combinedLight, combinedOverlay);
            } catch (Exception e) {
                getBlockInfo().setTileEntity(null);
            }
        }
    }

    @Override
    public boolean hasTESR(TileEntity tileEntity) {
        return true;
    }

    @Override
    public boolean isGlobalRenderer(TileEntity tileEntity) {
        return true;
    }

}
