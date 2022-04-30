package com.lowdragmc.multiblocked.core.mixins;

import com.lowdragmc.multiblocked.persistence.MultiblockWorldSavedData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TileEntityRendererDispatcher.class)
public class TileEntityRendererDispatcherMixin {
    @Final @Shadow public static TileEntityRendererDispatcher instance;

    @Inject(method = "getRenderer", at = @At(value = "HEAD"), cancellable = true)
    private <T extends TileEntity> void injectGetRenderer(T tileEntity, CallbackInfoReturnable<TileEntityRenderer<T>> cir) {
        if (tileEntity != null) {
            if (tileEntity.getLevel() == Minecraft.getInstance().level && MultiblockWorldSavedData.modelDisabled.contains(tileEntity.getBlockPos())) {
                cir.setReturnValue(null);
            }
        }
    }

}
