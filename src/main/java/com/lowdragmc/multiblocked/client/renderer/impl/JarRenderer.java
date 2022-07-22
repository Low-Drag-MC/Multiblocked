package com.lowdragmc.multiblocked.client.renderer.impl;


import com.lowdragmc.lowdraglib.client.model.ModelFactory;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.common.tile.JarTileEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.ForgeModelBakery;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;

/**
 * @author KilaBash
 * @date 2022/7/20
 * @implNote PedestalRenderer
 */
public class JarRenderer extends MBDIModelRenderer{
    public JarRenderer() {
        super(new ResourceLocation(Multiblocked.MODID,"block/jar"));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean hasTESR(BlockEntity BlockEntity) {
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    protected BakedModel getBlockBakedModel(BlockPos pos, BlockAndTintGetter blockAccess) {
        BlockState blockState = blockAccess.getBlockState(pos);
        Direction frontFacing = Direction.NORTH;
        if (blockState.hasProperty(BlockStateProperties.FACING)) {
            frontFacing = blockState.getValue(BlockStateProperties.FACING);
        } else if (blockState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            frontFacing = blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        return blockModels.computeIfAbsent(frontFacing, facing -> {
            BakedModel model = getModel().bake(
                    ForgeModelBakery.instance(),
                    ForgeModelBakery.defaultTextureGetter(),
                    ModelFactory.getRotation(facing),
                    modelLocation);
            return model;
        });
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void render(BlockEntity BlockEntity, float partialTicks, PoseStack stack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        if (BlockEntity instanceof JarTileEntity tile) {
            FluidStack fluidStack = tile.getFluidStack();
            Minecraft mc = Minecraft.getInstance();
            if (fluidStack != null && !fluidStack.isEmpty() && mc.level != null) {
                float height = fluidStack.getAmount() / 5000f * (11f / 16);
                ResourceLocation LOCATION_BLOCKS_TEXTURE = TextureAtlas.LOCATION_BLOCKS;
                FluidAttributes fluid = fluidStack.getFluid().getAttributes();
                ResourceLocation fluidStill = fluid.getStillTexture();
                TextureAtlasSprite fluidStillSprite = Minecraft.getInstance().getTextureAtlas(LOCATION_BLOCKS_TEXTURE).apply(fluidStill);

                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShaderTexture(0, LOCATION_BLOCKS_TEXTURE);

                stack.pushPose();
                Tesselator tessellator = Tesselator.getInstance();
                BufferBuilder bufferBuilder = tessellator.getBuilder();

                RenderSystem.enableDepthTest();
                RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
                bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

                renderCubeFace(stack, bufferBuilder, 4f / 16, 1f / 16, 4f / 16, 12f / 16, height, 12f / 16, fluid.getColor(fluidStack) | 0xff000000, fluidStillSprite);
                tessellator.end();

                stack.popPose();
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void renderCubeFace(PoseStack matrixStack, BufferBuilder buffer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int fluidColor, TextureAtlasSprite textureSprite) {
        Matrix4f mat = matrixStack.last().pose();
        float uMin = textureSprite.getU0();
        float uMax = textureSprite.getU1();
        float vMin = textureSprite.getV0();
        float vMax = textureSprite.getV1();

        buffer.vertex(mat, minX, minY, minZ).uv(uMin, vMax).color(fluidColor).endVertex();
        buffer.vertex(mat, minX, minY, maxZ).uv(uMax, vMax).color(fluidColor).endVertex();
        buffer.vertex(mat, minX, maxY, maxZ).uv(uMax, vMin).color(fluidColor).endVertex();
        buffer.vertex(mat, minX, maxY, minZ).uv(uMin, vMin).color(fluidColor).endVertex();

        buffer.vertex(mat, maxX, minY, minZ).uv(uMin, vMax).color(fluidColor).endVertex();
        buffer.vertex(mat, maxX, maxY, minZ).uv(uMax, vMax).color(fluidColor).endVertex();
        buffer.vertex(mat, maxX, maxY, maxZ).uv(uMax, vMin).color(fluidColor).endVertex();
        buffer.vertex(mat, maxX, minY, maxZ).uv(uMin, vMin).color(fluidColor).endVertex();


        buffer.vertex(mat, minX, minY, minZ).uv(uMin, vMax).color(fluidColor).endVertex();
        buffer.vertex(mat, maxX, minY, minZ).uv(uMax, vMax).color(fluidColor).endVertex();
        buffer.vertex(mat, maxX, minY, maxZ).uv(uMax, vMin).color(fluidColor).endVertex();
        buffer.vertex(mat, minX, minY, maxZ).uv(uMin, vMin).color(fluidColor).endVertex();


        buffer.vertex(mat, minX, maxY, minZ).uv(uMin, vMax).color(fluidColor).endVertex();
        buffer.vertex(mat, minX, maxY, maxZ).uv(uMax, vMax).color(fluidColor).endVertex();
        buffer.vertex(mat, maxX, maxY, maxZ).uv(uMax, vMin).color(fluidColor).endVertex();
        buffer.vertex(mat, maxX, maxY, minZ).uv(uMin, vMin).color(fluidColor).endVertex();

        buffer.vertex(mat, minX, minY, minZ).uv(uMin, vMax).color(fluidColor).endVertex();
        buffer.vertex(mat, minX, maxY, minZ).uv(uMax, vMax).color(fluidColor).endVertex();
        buffer.vertex(mat, maxX, maxY, minZ).uv(uMax, vMin).color(fluidColor).endVertex();
        buffer.vertex(mat, maxX, minY, minZ).uv(uMin, vMin).color(fluidColor).endVertex();

        buffer.vertex(mat, minX, minY, maxZ).uv(uMin, vMax).color(fluidColor).endVertex();
        buffer.vertex(mat, maxX, minY, maxZ).uv(uMax, vMax).color(fluidColor).endVertex();
        buffer.vertex(mat, maxX, maxY, maxZ).uv(uMax, vMin).color(fluidColor).endVertex();
        buffer.vertex(mat, minX, maxY, maxZ).uv(uMin, vMin).color(fluidColor).endVertex();
    }
}
