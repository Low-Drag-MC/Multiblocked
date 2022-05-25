package com.lowdragmc.multiblocked.core.mixins;

import com.lowdragmc.multiblocked.persistence.MultiblockWorldSavedData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityRenderDispatcher.class)
public class TileEntityRendererDispatcherMixin {

    @Inject(method = "getRenderer", at = @At(value = "HEAD"), cancellable = true)
    private <T extends BlockEntity> void injectGetRenderer(T tileEntity, CallbackInfoReturnable<BlockEntityRenderer<T>> cir) {
        if (tileEntity != null) {
            if (tileEntity.getLevel() == Minecraft.getInstance().level && MultiblockWorldSavedData.modelDisabled.contains(tileEntity.getBlockPos())) {
                cir.setReturnValue(null);
            }
        }
    }

}
