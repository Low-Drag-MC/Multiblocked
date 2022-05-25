package com.lowdragmc.multiblocked.client.renderer;

import com.lowdragmc.lowdraglib.client.renderer.ATESRRendererProvider;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

/**
 * Author: KilaBash
 * Date: 2022/04/26
 * Description:
 */
@OnlyIn(Dist.CLIENT)
public class ComponentTESR extends ATESRRendererProvider<ComponentTileEntity<?>> {

    public ComponentTESR(BlockEntityRendererProvider.Context context) {

    }

    @Override
    public IRenderer getRenderer(@Nonnull ComponentTileEntity<?> tileEntity) {
        return tileEntity.getRenderer();
    }

}
