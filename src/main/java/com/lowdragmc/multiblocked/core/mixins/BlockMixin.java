package com.lowdragmc.multiblocked.core.mixins;

import com.lowdragmc.multiblocked.api.block.BlockComponent;
import com.lowdragmc.multiblocked.persistence.MultiblockWorldSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public class BlockMixin {

    @Inject(method = "shouldRenderFace",  at = @At(value = "HEAD"), cancellable = true)
    private static void injectShouldRenderFace(BlockState state,
                                               BlockGetter p_152446_,
                                               BlockPos pos,
                                               Direction facing,
                                               BlockPos p_152449_,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() instanceof BlockComponent && !state.canOcclude()) {
            cir.setReturnValue(true);
        } else if (MultiblockWorldSavedData.isModelDisabled(pos.relative(facing))) {
            cir.setReturnValue(true);
        }
    }

}
