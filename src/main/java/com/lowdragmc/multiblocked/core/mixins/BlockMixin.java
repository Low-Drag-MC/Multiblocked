package com.lowdragmc.multiblocked.core.mixins;

import com.lowdragmc.multiblocked.api.block.BlockComponent;
import com.lowdragmc.multiblocked.persistence.MultiblockWorldSavedData;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public class BlockMixin {

    @Inject(method = "shouldRenderFace",  at = @At(value = "HEAD"), cancellable = true)
    private static void injectShouldRenderFace(BlockState state,
                                               IBlockReader world,
                                               BlockPos pos,
                                               Direction facing,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() instanceof BlockComponent && !state.canOcclude()) {
            cir.setReturnValue(true);
        } else if (MultiblockWorldSavedData.isModelDisabled(pos.relative(facing))) {
            cir.setReturnValue(true);
        }
    }

}
