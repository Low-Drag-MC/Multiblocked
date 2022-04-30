package com.lowdragmc.multiblocked.core.mixins;

import com.lowdragmc.multiblocked.persistence.MultiblockWorldSavedData;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraftforge.client.model.data.IModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

@Mixin(targets = {"net/minecraft/client/renderer/chunk/ChunkRenderDispatcher$ChunkRender$RebuildTask"})
public class RenderChunkMixin {
    @Redirect(method = "compile", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/BlockRendererDispatcher;renderModel(Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/IBlockDisplayReader;Lcom/mojang/blaze3d/matrix/MatrixStack;Lcom/mojang/blaze3d/vertex/IVertexBuilder;ZLjava/util/Random;Lnet/minecraftforge/client/model/data/IModelData;)Z"), remap = false)
    private boolean injectRenderModel(
            BlockRendererDispatcher blockRendererDispatcher,
            BlockState blockStateIn, BlockPos posIn,
            IBlockDisplayReader lightReaderIn, MatrixStack matrixStackIn,
            IVertexBuilder vertexBuilderIn, boolean checkSides,
            Random rand, IModelData modelData) {
        MultiblockWorldSavedData.isBuildingChunk.set(true);
        if (MultiblockWorldSavedData.isModelDisabled(posIn)) {
            MultiblockWorldSavedData.isBuildingChunk.set(false);
            return false;
        }
        boolean result = blockRendererDispatcher.renderModel(blockStateIn, posIn, lightReaderIn, matrixStackIn, vertexBuilderIn, checkSides, rand, modelData);
        MultiblockWorldSavedData.isBuildingChunk.set(false);
        return result;
    }

}
