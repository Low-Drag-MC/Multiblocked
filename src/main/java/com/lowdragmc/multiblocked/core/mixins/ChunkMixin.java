package com.lowdragmc.multiblocked.core.mixins;

import com.lowdragmc.multiblocked.api.pattern.MultiblockState;
import com.lowdragmc.multiblocked.persistence.MultiblockWorldSavedData;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Chunk.class)
public class ChunkMixin {
    @Final @Shadow private World level;
    @Final @Shadow private ChunkPos chunkPos;
    
    // We want to be as quick as possible here
    @Inject(method = "setBlockState", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/world/World;captureBlockSnapshots:Z", remap = false))
    private void onAddingBlock(BlockPos pos, BlockState state, boolean isMoving, CallbackInfoReturnable<BlockState> cir) {
        MinecraftServer server = level.getServer();
        if (server != null) {
            server.executeBlocking(() -> {
                for (MultiblockState structure : MultiblockWorldSavedData.getOrCreate(level).getControllerInChunk(chunkPos)) {
                    if (structure.isPosInCache(pos)) {
                        structure.onBlockStateChanged(pos);
                    }
                }
            });
        }
    }

}
