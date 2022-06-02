package com.lowdragmc.multiblocked.client.renderer;

import com.lowdragmc.lowdraglib.client.renderer.ATESRRendererProvider;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Author: KilaBash
 * Date: 2022/04/26
 * Description:
 */
@OnlyIn(Dist.CLIENT)
public class ComponentTESR extends ATESRRendererProvider<BlockEntity> {

    public ComponentTESR(BlockEntityRendererProvider.Context context) {

    }

}
