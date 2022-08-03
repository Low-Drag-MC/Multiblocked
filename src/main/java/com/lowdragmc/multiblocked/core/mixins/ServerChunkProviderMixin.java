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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.Arrays;

@Mixin(ServerChunkProvider.class)
public abstract class ServerChunkProviderMixin {

    @Shadow @Final private Thread mainThread;

    @Shadow @Final public ServerWorld level;

    @Shadow @Nullable protected abstract ChunkHolder getVisibleChunkIfPresent(long p_217213_1_);

    private final long[] mbdLastChunkPos = new long[4];

    private final Chunk[] mbdLastChunk = new Chunk[4];

    @Inject(method = "clearCache", at = @At(value = "TAIL"))
    private void injectClearCache(CallbackInfo ci) {
        synchronized (this.mbdLastChunkPos) {
            Arrays.fill(this.mbdLastChunkPos, ChunkPos.INVALID_CHUNK_POS);
            Arrays.fill(this.mbdLastChunk, null);
        }
    }

    private void storeInCache(long pos, Chunk chunkAccess) {
        synchronized (this.mbdLastChunkPos) {
            for(int i = 3; i > 0; --i) {
                this.mbdLastChunkPos[i] = this.mbdLastChunkPos[i - 1];
                this.mbdLastChunk[i] = this.mbdLastChunk[i - 1];
            }

            this.mbdLastChunkPos[0] = pos;
            this.mbdLastChunk[0] = chunkAccess;
        }
    }

    @Inject(method = "getChunkNow", at = @At(value = "HEAD"), cancellable = true)
    private void getTileEntity(int pChunkX, int pChunkZ, CallbackInfoReturnable<Chunk> cir) {
        if (Thread.currentThread() != this.mainThread && MultiblockWorldSavedData.isThreadService()) {
            long i = ChunkPos.asLong(pChunkX, pChunkZ);

            for(int j = 0; j < 4; ++j) {
                if (i == this.mbdLastChunkPos[j]) {
                    cir.setReturnValue(this.mbdLastChunk[j]);
                    return;
                }
            }

            ChunkHolder chunkholder = this.getVisibleChunkIfPresent(i);
            if (chunkholder != null) {
                Either<IChunk, ChunkHolder.IChunkLoadingError> either = chunkholder.getFutureIfPresent(ChunkStatus.FULL).getNow(null);
                if (either != null) {
                    IChunk ichunk1 = either.left().orElse(null);
                    if (ichunk1 != null) {
                        if (ichunk1 instanceof Chunk) {
                            storeInCache(i, (Chunk) ichunk1);
                            cir.setReturnValue((Chunk)ichunk1);
                            return;
                        }
                    }
                }
            }
            cir.setReturnValue(null);
        }
    }

}
