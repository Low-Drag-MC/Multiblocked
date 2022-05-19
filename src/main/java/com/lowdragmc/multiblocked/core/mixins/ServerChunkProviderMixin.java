package com.lowdragmc.multiblocked.core.mixins;

import com.lowdragmc.multiblocked.persistence.MultiblockWorldSavedData;
import com.mojang.datafixers.util.Either;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(ServerChunkProvider.class)
public abstract class ServerChunkProviderMixin {

    @Shadow @Final private Thread mainThread;

    @Shadow @Final public ServerWorld level;

    @Shadow @Final private long[] lastChunkPos;

    @Shadow @Final private ChunkStatus[] lastChunkStatus;

    @Shadow @Final private IChunk[] lastChunk;

    @Shadow @Nullable protected abstract ChunkHolder getVisibleChunkIfPresent(long p_217213_1_);

    @Shadow protected abstract void storeInCache(long p_225315_1_, IChunk p_225315_3_, ChunkStatus p_225315_4_);

    @Inject(method = "getChunkNow", at = @At(value = "HEAD"), cancellable = true)
    private void getTileEntity(int pChunkX, int pChunkZ, CallbackInfoReturnable<Chunk> cir) {
        if (Thread.currentThread() != this.mainThread && MultiblockWorldSavedData.isThreadService()) {
            long i = ChunkPos.asLong(pChunkX, pChunkZ);

            for(int j = 0; j < 4; ++j) {
                if (i == this.lastChunkPos[j] && this.lastChunkStatus[j] == ChunkStatus.FULL) {
                    IChunk ichunk = this.lastChunk[j];
                    cir.setReturnValue(ichunk instanceof Chunk ? (Chunk)ichunk : null);
                    return;
                }
            }

            ChunkHolder chunkholder = this.getVisibleChunkIfPresent(i);
            if (chunkholder == null) {
                cir.setReturnValue(null);
            } else {
                Either<IChunk, ChunkHolder.IChunkLoadingError> either = chunkholder.getFutureIfPresent(ChunkStatus.FULL).getNow(null);
                if (either == null) {
                    cir.setReturnValue(null);
                } else {
                    IChunk ichunk1 = either.left().orElse(null);
                    if (ichunk1 != null) {
                        this.storeInCache(i, ichunk1, ChunkStatus.FULL);
                        if (ichunk1 instanceof Chunk) {
                            cir.setReturnValue((Chunk)ichunk1);
                            return;
                        }
                    }
                    cir.setReturnValue(null);
                }
            }
        }
    }

}
