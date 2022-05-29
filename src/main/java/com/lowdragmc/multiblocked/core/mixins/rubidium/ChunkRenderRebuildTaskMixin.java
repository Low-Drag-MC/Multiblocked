package com.lowdragmc.multiblocked.core.mixins.rubidium;

import com.lowdragmc.multiblocked.persistence.MultiblockWorldSavedData;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderRebuildTask;
import me.jellysquid.mods.sodium.client.render.pipeline.BlockRenderer;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraftforge.client.model.data.IModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author KilaBash
 * @date 2022/05/29
 * @implNote ChunkRenderRebuildTaskMixin
 */
@Mixin(ChunkRenderRebuildTask.class)
public class ChunkRenderRebuildTaskMixin {

    @Redirect(method = "performBuild",  at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/pipeline/BlockRenderer;renderModel(Lnet/minecraft/world/IBlockDisplayReader;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/model/IBakedModel;Lme/jellysquid/mods/sodium/client/render/chunk/compile/buffers/ChunkModelBuffers;ZJLnet/minecraftforge/client/model/data/IModelData;)Z"),
            remap = false)
    public boolean injectPerformBuild(BlockRenderer blockRenderer,
                                      IBlockDisplayReader world,
                                      BlockState state,
                                      BlockPos pos,
                                      IBakedModel model,
                                      ChunkModelBuffers buffers,
                                      boolean cull, long seed, IModelData modelData) {
        MultiblockWorldSavedData.isBuildingChunk.set(true);
        if (MultiblockWorldSavedData.isModelDisabled(pos)) {
            MultiblockWorldSavedData.isBuildingChunk.set(false);
            return false;
        }
        boolean result = blockRenderer.renderModel(world, state, pos, model, buffers, cull, seed, modelData);
        MultiblockWorldSavedData.isBuildingChunk.set(false);
        return result;
    }
}
