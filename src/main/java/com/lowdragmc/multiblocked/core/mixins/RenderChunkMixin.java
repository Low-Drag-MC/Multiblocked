package com.lowdragmc.multiblocked.core.mixins;

import com.lowdragmc.multiblocked.persistence.MultiblockWorldSavedData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraftforge.client.model.data.IModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

@Mixin(targets = {"net/minecraft/client/renderer/chunk/ChunkRenderDispatcher$RenderChunk$RebuildTask"})
public class RenderChunkMixin {
    @Redirect(method = "compile", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/BlockRenderDispatcher;renderBatched(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLjava/util/Random;Lnet/minecraftforge/client/model/data/IModelData;)Z"))
    private boolean injectRenderModel(
            BlockRenderDispatcher blockRendererDispatcher,
            BlockState pState, BlockPos pPos, BlockAndTintGetter pLevel,
            PoseStack pPoseStack, VertexConsumer pConsumer,
            boolean pCheckSides, Random pRandom,
            IModelData modelData) {
        MultiblockWorldSavedData.isBuildingChunk.set(true);
        if (MultiblockWorldSavedData.isModelDisabled(pPos)) {
            MultiblockWorldSavedData.isBuildingChunk.set(false);
            return false;
        }
        boolean result = blockRendererDispatcher.renderBatched(pState, pPos, pLevel, pPoseStack, pConsumer, pCheckSides, pRandom, modelData);
        MultiblockWorldSavedData.isBuildingChunk.set(false);
        return result;
    }

}
