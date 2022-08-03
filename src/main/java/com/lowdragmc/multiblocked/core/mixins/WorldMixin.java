package com.lowdragmc.multiblocked.core.mixins;

import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import com.lowdragmc.multiblocked.persistence.MultiblockWorldSavedData;
import net.minecraft.block.BlockState;
import net.minecraft.profiler.IProfiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
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

@Mixin(World.class)
public abstract class WorldMixin implements IWorld {

    @Shadow @Final public boolean isClientSide;

    @Shadow @Final private Thread thread;

    @Shadow public abstract boolean isLoaded(BlockPos pPos);


    @Inject(method = "tickBlockEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/IProfiler;pop()V", ordinal = 1), locals = LocalCapture.CAPTURE_FAILHARD)
    private void afterUpdatingEntities(CallbackInfo ci, IProfiler iprofiler) {
        iprofiler.popPush("multiblocked_update");
        if (!((World) (Object) this).isClientSide) {
            List<ComponentTileEntity<?>> isRemoved = null;
            MultiblockWorldSavedData mbds = MultiblockWorldSavedData.getOrCreate((World) (Object) this);
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

    private Chunk getChunkNow(int pX, int pZ) {
        return this.getChunkSource().getChunkNow(pX, pZ);
    }

    @Inject(method = "getBlockEntity", at = @At(value = "HEAD"), cancellable = true)
    private void getTileEntity(BlockPos pos, CallbackInfoReturnable<TileEntity> cir) {
        if (!this.isClientSide && Thread.currentThread() != this.thread && MultiblockWorldSavedData.isThreadService() && isLoaded(pos)) {
            Chunk chunk = this.getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            if (chunk != null) {
                cir.setReturnValue(chunk.getBlockEntities().get(pos));
            }
        }
    }

    @Inject(method = "getBlockState", at = @At(value = "HEAD"), cancellable = true)
    private void getBlockState(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        if (!this.isClientSide && Thread.currentThread() != this.thread && MultiblockWorldSavedData.isThreadService() && isLoaded(pos)) {
            Chunk chunk = this.getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            if (chunk != null) {
                cir.setReturnValue(chunk.getBlockState(pos));
            }
        }
    }

}
