package com.lowdragmc.multiblocked.core.mixins;

import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import com.lowdragmc.multiblocked.persistence.MultiblockWorldSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.List;

@Mixin(Level.class)
public abstract class WorldMixin implements LevelAccessor {

    @Shadow @Final public boolean isClientSide;

    @Shadow @Final private Thread thread;

    @Shadow public abstract boolean isLoaded(BlockPos pPos);

    @Inject(method = "tickBlockEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;pop()V"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void afterUpdatingEntities(CallbackInfo ci, ProfilerFiller profilerfiller) {
        profilerfiller.popPush("multiblocked_update");
        if (!((Level) (Object) this).isClientSide) {
            List<ComponentTileEntity<?>> isRemoved = null;
            MultiblockWorldSavedData mbds = MultiblockWorldSavedData.getOrCreate((Level) (Object) this);
            for (ComponentTileEntity<?> loading : mbds.getLoadings()) {
                if (loading.isRemoved()) {
                    if (isRemoved == null) {
                        isRemoved = new ArrayList<>();
                    }
                    isRemoved.add(loading);
                } else {
                    loading.update();
                }
            }
            if (isRemoved != null) {
                for (ComponentTileEntity<?> inValid : isRemoved) {
                    mbds.removeLoading(inValid.getBlockPos());
                }
            }
        }
    }

    private ChunkAccess getChunkNow(int pX, int pZ) {
        return this.getChunkSource().getChunkNow(pX, pZ);
    }

    @Inject(method = "getBlockEntity", at = @At(value = "HEAD"), cancellable = true)
    private void getTileEntity(BlockPos pos, CallbackInfoReturnable<BlockEntity> cir) {
        if (!this.isClientSide && Thread.currentThread() != this.thread && MultiblockWorldSavedData.isThreadService() && isLoaded(pos)) {
            ChunkAccess chunk = this.getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            if (chunk instanceof LevelChunk levelChunk) {
                cir.setReturnValue(levelChunk.getBlockEntities().get(pos));
            }
        }
    }

    @Inject(method = "getBlockState", at = @At(value = "HEAD"), cancellable = true)
    private void getBlockState(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        if (!this.isClientSide && Thread.currentThread() != this.thread && MultiblockWorldSavedData.isThreadService() && isLoaded(pos)) {
            ChunkAccess chunk = this.getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            if (chunk != null) {
                cir.setReturnValue(chunk.getBlockState(pos));
            }
        }
    }

}
