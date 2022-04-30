package com.lowdragmc.multiblocked.core.mixins;

import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import com.lowdragmc.multiblocked.persistence.MultiblockWorldSavedData;
import net.minecraft.profiler.IProfiler;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.List;

@Mixin(World.class)
public class WorldMixin {

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

}
