package com.lowdragmc.multiblocked.core.mixins.rubidium;

import com.lowdragmc.multiblocked.persistence.MultiblockWorldSavedData;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderRebuildTask;
import me.jellysquid.mods.sodium.client.render.pipeline.BlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
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

    @Redirect(method = "performBuild",  at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/pipeline/BlockRenderer;renderModel(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/client/resources/model/BakedModel;Lme/jellysquid/mods/sodium/client/render/chunk/compile/buffers/ChunkModelBuilder;ZJLnet/minecraftforge/client/model/data/IModelData;)Z"),
            remap = false)
    public boolean injectPerformBuild(BlockRenderer blockRenderer,
                                      BlockAndTintGetter world, BlockState state,
                                      BlockPos pos, BlockPos origin,
                                      BakedModel model,
                                      ChunkModelBuilder buffers,
                                      boolean cull,
                                      long seed,
                                      IModelData modelData) {
        MultiblockWorldSavedData.isBuildingChunk.set(true);
        if (MultiblockWorldSavedData.isModelDisabled(pos)) {
            MultiblockWorldSavedData.isBuildingChunk.set(false);
            return false;
        }
        boolean result = blockRenderer.renderModel(world, state, pos, origin, model, buffers, cull, seed, modelData);
        MultiblockWorldSavedData.isBuildingChunk.set(false);
        return result;
    }
}
