package com.lowdragmc.multiblocked.core.mixins;

import com.lowdragmc.multiblocked.persistence.MultiblockWorldSavedData;
import com.mojang.datafixers.util.Either;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(ServerChunkCache.class)
public abstract class ServerChunkProviderMixin {

    @Shadow @Final Thread mainThread;

    @Shadow @Final public ServerLevel level;

    @Shadow @Final private long[] lastChunkPos;

    @Shadow @Final private ChunkStatus[] lastChunkStatus;

    @Shadow @Final private ChunkAccess[] lastChunk;

    @Shadow @Nullable protected abstract ChunkHolder getVisibleChunkIfPresent(long p_217213_1_);

    @Shadow protected abstract void storeInCache(long p_225315_1_, ChunkAccess p_225315_3_, ChunkStatus p_225315_4_);

    @Inject(method = "getChunkNow", at = @At(value = "HEAD"), cancellable = true)
    private void getTileEntity(int pChunkX, int pChunkZ, CallbackInfoReturnable<LevelChunk> cir) {
        if (Thread.currentThread() != this.mainThread && MultiblockWorldSavedData.isThreadService()) {
            long i = ChunkPos.asLong(pChunkX, pChunkZ);

            for(int j = 0; j < 4; ++j) {
                if (i == this.lastChunkPos[j] && this.lastChunkStatus[j] == ChunkStatus.FULL) {
                    ChunkAccess ichunk = this.lastChunk[j];
                    cir.setReturnValue(ichunk instanceof LevelChunk ? (LevelChunk)ichunk : null);
                    return;
                }
            }

            ChunkHolder chunkholder = this.getVisibleChunkIfPresent(i);
            if (chunkholder == null) {
                cir.setReturnValue(null);
            } else {
                Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> either = chunkholder.getFutureIfPresent(ChunkStatus.FULL).getNow(null);
                if (either == null) {
                    cir.setReturnValue(null);
                } else {
                    ChunkAccess ichunk1 = either.left().orElse(null);
                    if (ichunk1 != null) {
                        this.storeInCache(i, ichunk1, ChunkStatus.FULL);
                        if (ichunk1 instanceof LevelChunk) {
                            cir.setReturnValue((LevelChunk)ichunk1);
                            return;
                        }
                    }
                    cir.setReturnValue(null);
                }
            }
        }
    }

}
