package com.lowdragmc.multiblocked.core.mixins.rubidium;

import com.lowdragmc.multiblocked.api.block.BlockComponent;
import com.lowdragmc.multiblocked.persistence.MultiblockWorldSavedData;
import me.jellysquid.mods.sodium.client.render.occlusion.BlockOcclusionCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author KilaBash
 * @date 2022/05/29
 * @implNote BlockOcclusionCacheMixin
 */
@Mixin(BlockOcclusionCache.class)
public class BlockOcclusionCacheMixin {
    @Inject(method = "shouldDrawSide",  at = @At(value = "HEAD"), cancellable = true, remap = false)
    public void injectShouldRenderFace(BlockState state,
                                               BlockGetter view, BlockPos pos,
                                               Direction facing,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() instanceof BlockComponent && !state.canOcclude()) {
            cir.setReturnValue(true);
        } else if (MultiblockWorldSavedData.isModelDisabled(pos.relative(facing))) {
            cir.setReturnValue(true);
        }
    }
}
