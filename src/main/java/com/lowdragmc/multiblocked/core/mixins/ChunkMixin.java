package com.lowdragmc.multiblocked.core.mixins;

import com.lowdragmc.multiblocked.api.pattern.MultiblockState;
import com.lowdragmc.multiblocked.persistence.MultiblockWorldSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.chunk.LevelChunk;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public class ChunkMixin {
    @Final @Shadow Level level;

    // We want to be as quick as possible here
    @Inject(method = "setBlockState", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/world/level/Level;captureBlockSnapshots:Z"))
    private void onAddingBlock(BlockPos pos, BlockState state, boolean isMoving, CallbackInfoReturnable<BlockState> cir) {
        MinecraftServer server = level.getServer();
        if (server != null) {
            server.execute(() -> {
                for (MultiblockState structure : MultiblockWorldSavedData.getOrCreate(level).getControllerInChunk(((LevelChunk)(Object)this).getPos())) {
                    if (structure.isPosInCache(pos)) {
                        structure.onBlockStateChanged(pos);
                    }
                }
            });
        }
    }

}
